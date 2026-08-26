package com.parksense.persistence;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
import com.parksense.audit.AuditEntry;
import com.parksense.audit.AuditTrail;
import com.parksense.auth.User;
import com.parksense.common.Money;
import com.parksense.lot.SlotState;
import com.parksense.members.Member;
import com.parksense.occupancy.OccupancyLedger;
import com.parksense.store.MemberStore;
import com.parksense.store.TariffStore;
import com.parksense.store.TicketStore;
import com.parksense.store.UserStore;
import com.parksense.tariff.TariffKind;
import com.parksense.tariff.TariffPlan;
import com.parksense.tariff.builder.TariffPlanBuilder;
import com.parksense.tickets.FeeComponent;
import com.parksense.tickets.PaymentMethod;
import com.parksense.tickets.PaymentRecord;
import com.parksense.tickets.Ticket;
import com.parksense.tickets.TicketNoGenerator;
import com.parksense.tickets.state.IllegalTransitionException;
import com.parksense.vehicles.PlateRegistry;
import com.parksense.vehicles.VehicleType;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Optional MongoDB persistence: the in-memory repositories remain the live
 * source of truth, and this bridge mirrors them into a dedicated
 * {@code parksense} database (write-behind, debounced) and reloads them on
 * boot. With no reachable MONGODB_URI the system runs exactly as before in
 * zero-setup in-memory mode.
 *
 * Collections: tickets, members, tariffs, users, audit_log, lot_state,
 * counters.
 */
public final class MongoSync {

    private static final Logger log = LoggerFactory.getLogger(MongoSync.class);
    private static final Set<String> COLLECTIONS =
            Set.of("tickets", "members", "tariffs", "users", "audit_log", "lot_state", "counters");

    private final MongoClient client;      // null in in-memory mode
    private final MongoDatabase database;  // null in in-memory mode
    private final String mode;

    private TicketStore ticketStore;
    private MemberStore memberStore;
    private TariffStore tariffStore;
    private UserStore userStore;
    private AuditTrail auditTrail;

    private final Set<String> dirty = ConcurrentHashMap.newKeySet();
    private ScheduledExecutorService flusher;

    private MongoSync(MongoClient client, MongoDatabase database, String mode) {
        this.client = client;
        this.database = database;
        this.mode = mode;
    }

    // ------------------------------------------------------------------
    // Connection
    // ------------------------------------------------------------------

    /** Try to connect using .env / environment settings; never throws. */
    public static MongoSync connect() {
        String uri = EnvFile.get("MONGODB_URI");
        String dbName = Optional.ofNullable(EnvFile.get("MONGODB_DB")).orElse("parksense");
        if (uri == null || uri.isBlank() || uri.contains("YOUR-CLUSTER")) {
            log.info("ParkSense storage: IN-MEMORY (no MONGODB_URI set — a restart reseeds the demo)");
            return new MongoSync(null, null, "in-memory");
        }
        try {
            var settings = com.mongodb.MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(uri))
                    .applyToClusterSettings(cluster ->
                            cluster.serverSelectionTimeout(5, TimeUnit.SECONDS))
                    .build();
            MongoClient connected = MongoClients.create(settings);
            MongoDatabase db = connected.getDatabase(dbName);
            db.runCommand(new Document("ping", 1));
            log.info("ParkSense storage: MONGODB atlas database '{}' on {} — state persists across restarts",
                    dbName, uri.replaceAll("//.*@|mongodb(\\+srv)?:", "***"));
            return new MongoSync(connected, db, "mongodb:" + dbName);
        } catch (Exception e) {
            log.warn("ParkSense storage: IN-MEMORY FALLBACK — could not reach the database ({}). "
                            + "Check the password in .env and the Atlas Network Access IP list. "
                            + "The demo keeps running, but a restart loses today's changes.",
                    e.getMessage());
            return new MongoSync(null, null, "in-memory (fallback)");
        }
    }

    public boolean active() {
        return database != null;
    }

    public String mode() {
        return mode;
    }

    public void attachStores(TicketStore tickets, MemberStore members, TariffStore tariffs,
                             UserStore users, AuditTrail audit) {
        this.ticketStore = tickets;
        this.memberStore = members;
        this.tariffStore = tariffs;
        this.userStore = users;
        this.auditTrail = audit;
    }

    // ------------------------------------------------------------------
    // Boot: load persisted state (call BEFORE any seeding)
    // ------------------------------------------------------------------

    /**
     * Load every collection into the stores. Returns true when a previous
     * state was found (caller then skips seeding and restores runtime
     * state instead).
     */
    public boolean loadInto(TicketNoGenerator ticketNos) {
        if (!active()) {
            return false;
        }
        boolean foundAny = false;

        for (Document doc : database.getCollection("members").find()) {
            memberStore.save(memberFrom(doc));
            foundAny = true;
        }
        for (Document doc : database.getCollection("tariffs").find()) {
            tariffStore.save(tariffFrom(doc));
            foundAny = true;
        }
        for (Document doc : database.getCollection("users").find()) {
            userStore.save(userFrom(doc));
            foundAny = true;
        }
        if (userStore.find("dev").isEmpty()) {
            userStore.save(new User("dev", "Lead Software Developer",
                    com.parksense.auth.PasswordHasher.hash("dev123".toCharArray()), com.parksense.auth.Role.DEVELOPER));
        }
        List<Document> ticketDocs = database.getCollection("tickets").find()
                .into(new ArrayList<>());
        for (Document doc : ticketDocs) {
            Ticket ticket = ticketFrom(doc);
            if (ticket != null) {
                ticketStore.save(ticket);
                foundAny = true;
            }
        }
        for (Document doc : database.getCollection("audit_log").find()
                .sort(new Document("at", 1)).limit(1000).into(new ArrayList<>())) {
            auditTrail.record(new AuditEntry(
                    Instant.parse(doc.getString("at")), doc.getString("actor"),
                    doc.getString("action"), doc.getString("detail"), doc.getBoolean("allowed", true)));
        }
        Document counter = database.getCollection("counters")
                .find(new Document("_id", "ticketSeq")).first();
        if (counter != null && counter.containsKey("value")) {
            ticketNos.restore(counter.getLong("value"));
        }
        dirty.clear(); // loading must not schedule writes
        log.info("ParkSense storage: restored {} tickets, {} members, {} tariffs from '{}'",
                ticketDocs.size(), memberStore.all().size(), tariffStore.all().size(), mode);
        return foundAny;
    }

    /**
     * After a restored load: put open tickets back onto their bays, mark
     * the plates-inside set and re-apply maintenance states.
     */
    public void restoreRuntimeState(OccupancyLedger ledger, PlateRegistry plates) {
        if (!active()) {
            return;
        }
        for (Ticket ticket : ticketStore.open()) {
            ledger.slot(ticket.slotCode()).ifPresent(slot -> {
                boolean arrived = !"RESERVED".equals(ticket.stateName());
                ledger.restoreOccupancy(slot, ticket.ticketNo(), ticket.plate(),
                        ticket.entryTime(), arrived);
                plates.markInside(ticket.plate());
            });
        }
        Document lotState = database.getCollection("lot_state")
                .find(new Document("_id", "oos")).first();
        if (lotState != null && lotState.containsKey("codes")) {
            for (Object code : lotState.getList("codes", String.class)) {
                ledger.slot(String.valueOf(code))
                        .filter(slot -> slot.state() == SlotState.FREE)
                        .ifPresent(slot -> ledger.markOutOfService(slot.code()));
            }
        }
    }

    // ------------------------------------------------------------------
    // Write-behind mirroring
    // ------------------------------------------------------------------

    /** Subscribe to store changes and start the debounced flusher. */
    public void startSync() {
        if (!active() || flusher != null) {
            return;
        }
        ticketStore.onChange(() -> dirty.add("tickets"));
        memberStore.onChange(() -> dirty.add("members"));
        tariffStore.onChange(() -> dirty.add("tariffs"));
        userStore.onChange(() -> dirty.add("users"));
        auditTrail.onChange(() -> dirty.add("audit_log"));
        dirty.add("counters"); // sequence advances with every boot worth saving
        flusher = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread t = new Thread(task, "parksense-mongo-flush");
            t.setDaemon(true);
            return t;
        });
        flusher.scheduleWithFixedDelay(this::flushDirty, 3, 3, TimeUnit.SECONDS);
    }

    /** Mark everything for the next flush (after seeding or reset). */
    public void snapshotAll() {
        if (active()) {
            dirty.addAll(COLLECTIONS);
        }
    }

    private void flushDirty() {
        try {
            for (String name : dirty) {
                flushCollection(name);
            }
        } catch (Exception e) {
            log.warn("Mongo flush skipped: {}", e.getMessage());
        }
    }

    private void flushCollection(String name) {
        if (dirty.remove(name)) {
            switch (name) {
                case "tickets" -> replace("tickets",
                        ticketStore.all().stream().map(MongoSync::ticketTo).toList());
                case "members" -> replace("members",
                        memberStore.all().stream().map(MongoSync::memberTo).toList());
                case "tariffs" -> replace("tariffs",
                        tariffStore.all().stream().map(MongoSync::tariffTo).toList());
                case "users" -> replace("users",
                        userStore.all().stream().map(MongoSync::userTo).toList());
                case "audit_log" -> replace("audit_log",
                        auditTrail.all().stream().map(MongoSync::auditTo).toList());
                case "lot_state" -> replace("lot_state",
                        List.of(lotStateTo()));
                case "counters" -> database.getCollection("counters").replaceOne(
                        new Document("_id", "ticketSeq"),
                        new Document("_id", "ticketSeq").append("value", ticketSequence()),
                        new ReplaceOptions().upsert(true));
                default -> {
                }
            }
        }
    }

    private long ticketSequence() {
        // current count lives in the generator; ask whoever set it via reflection-free path
        return ticketSequenceSupplier == null ? 0 : ticketSequenceSupplier.getAsLong();
    }

    private java.util.function.LongSupplier ticketSequenceSupplier;

    /** The generator knows its sequence; AppConfig registers the reader here. */
    public void trackTicketSequence(TicketNoGenerator generator) {
        this.ticketSequenceSupplier = generator::currentCount;
    }

    private Document lotStateTo() {
        List<String> oos = ledgerOutOfServiceSupplier == null ? List.of() : ledgerOutOfServiceSupplier.get();
        return new Document("_id", "oos").append("codes", oos);
    }

    private java.util.function.Supplier<List<String>> ledgerOutOfServiceSupplier;

    /** Register the bay-maintenance reader (called from AppConfig). */
    public void trackLotState(OccupancyLedger ledger) {
        this.ledgerOutOfServiceSupplier = () -> ledger.lot().slots().stream()
                .filter(s -> s.state() == SlotState.OUT_OF_SERVICE)
                .map(s -> s.code())
                .toList();
    }

    private void replace(String collection, List<Document> docs) {
        var col = database.getCollection(collection);
        col.deleteMany(new Document());
        if (!docs.isEmpty()) {
            col.insertMany(docs);
        }
    }

    /** Final flush + close (Spring destroy method). */
    public void shutdown() {
        if (!active()) {
            return;
        }
        try {
            if (ledgerOutOfServiceSupplier != null) {
                dirty.add("lot_state");
            }
            if (ticketSequenceSupplier != null) {
                dirty.add("counters");
            }
            flushDirty();
        } finally {
            client.close();
        }
    }

    /** Drop every collection (system reset while persistence is active). */
    public void dropAll() {
        if (!active()) {
            return;
        }
        for (String name : COLLECTIONS) {
            database.getCollection(name).drop();
        }
        dirty.clear();
    }

    // ------------------------------------------------------------------
    // Document codecs
    // ------------------------------------------------------------------

    private static Document ticketTo(Ticket t) {
        Document doc = new Document("_id", t.ticketNo())
                .append("plate", t.plate())
                .append("vehicleType", t.vehicleType().name())
                .append("accessible", t.accessible())
                .append("entryTime", t.entryTime().toString())
                .append("entryGate", t.entryGateId())
                .append("slotCode", t.slotCode())
                .append("state", t.stateName())
                .append("exitGate", t.exitGateId())
                .append("exitTime", t.exitTime() == null ? null : t.exitTime().toString())
                .append("paidAt", t.paidAt() == null ? null : t.paidAt().toString())
                .append("tariffPlan", t.tariffPlanId())
                .append("tariffExplain", t.tariffExplain())
                .append("voidReason", t.voidReason())
                .append("feeTotal", t.feeTotal().toPlainString())
                .append("feeDescribe", t.feeChain() == null ? null : t.feeChain().describe());
        List<Document> lines = new ArrayList<>();
        t.feeLines().forEach(l -> lines.add(
                new Document("label", l.label()).append("amount", l.amount().toPlainString())));
        doc.append("feeLines", lines);
        List<Document> payments = new ArrayList<>();
        t.payments().forEach(p -> payments.add(new Document()
                .append("method", p.method() == null ? null : p.method().name())
                .append("amount", p.amount().toPlainString())
                .append("tendered", p.tendered().toPlainString())
                .append("change", p.changeDue().toPlainString())
                .append("at", p.at().toString())));
        doc.append("payments", payments);
        return doc;
    }

    private static Ticket ticketFrom(Document d) {
        try {
            Ticket t = new Ticket(
                    d.getString("_id"),
                    d.getString("plate"),
                    VehicleType.valueOf(d.getString("vehicleType")),
                    d.getBoolean("accessible", false),
                    Instant.parse(d.getString("entryTime")),
                    d.getString("entryGate"),
                    d.getString("slotCode"));
            String state = d.getString("state");
            if (!"ISSUED".equals(state)) {
                t.confirmEntry();
            }
            List<Document> payments = d.getList("payments", Document.class);
            if (payments == null) {
                payments = List.of();
            }
            List<PaymentRecord> records = payments.stream()
                    .map(p -> new PaymentRecord(
                            d.getString("_id"),
                            p.getString("method") == null ? null : PaymentMethod.valueOf(p.getString("method")),
                            new BigDecimal(p.getString("amount")),
                            new BigDecimal(p.getString("tendered")),
                            new BigDecimal(p.getString("change")),
                            Instant.parse(p.getString("at"))))
                    .toList();
            switch (state) {
                case "LOST" -> t.reportLost();
                case "VOID" -> t.voidTicket(d.getString("voidReason") == null
                        ? "restored" : d.getString("voidReason"));
                case "PAID" -> {
                    if (!records.isEmpty()) {
                        t.pay(records.get(0));
                    }
                }
                case "EXITED" -> {
                    if (!records.isEmpty()) {
                        t.pay(records.get(0));
                        for (int i = 1; i < records.size(); i++) {
                            t.addPayment(records.get(i)); // overstay supplements
                        }
                    } else {
                        t.pay(new PaymentRecord(d.getString("_id"), null,
                                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                                Instant.parse(d.getString("exitTime"))));
                    }
                    t.completeExit(d.getString("exitGate"),
                            Instant.parse(d.getString("exitTime")));
                }
                default -> {
                }
            }
            if ("EXITED".equals(state)) {
                List<FeeComponent.FeeLine> lines = new ArrayList<>();
                List<Document> feeLines = d.getList("feeLines", Document.class);
                if (feeLines != null) {
                    feeLines.forEach(l -> lines.add(new FeeComponent.FeeLine(
                            l.getString("label"), new BigDecimal(l.getString("amount")))));
                }
                t.setFee(new RestoredFee(new BigDecimal(d.getString("feeTotal")),
                        d.getString("feeDescribe") == null ? "Restored fee" : d.getString("feeDescribe"),
                        lines), d.getString("tariffPlan"), d.getString("tariffExplain"));
            }
            return t;
        } catch (IllegalTransitionException | IllegalArgumentException | NullPointerException e) {
            log.warn("Skipping unreadable persisted ticket {}: {}", d.getString("_id"), e.getMessage());
            return null;
        }
    }

    private static Document memberTo(Member m) {
        return new Document("_id", m.id())
                .append("name", m.name())
                .append("phone", m.phone())
                .append("plates", new ArrayList<>(m.plates()))
                .append("plan", m.planName())
                .append("validUntil", m.validUntil().toString())
                .append("active", m.active());
    }

    private static Member memberFrom(Document d) {
        Member m = new Member(d.getString("_id"), d.getString("name"), d.getString("phone"),
                Set.copyOf(d.getList("plates", String.class)),
                d.getString("plan"), LocalDate.parse(d.getString("validUntil")));
        m.setActive(d.getBoolean("active", true));
        return m;
    }

    private static Document tariffTo(TariffPlan p) {
        Document doc = new Document("_id", p.id())
                .append("name", p.name())
                .append("kind", p.kind().name())
                .append("baseFee", p.baseFee().toPlainString())
                .append("perHour", p.perHourFee().toPlainString())
                .append("graceMinutes", p.graceMinutes())
                .append("surge", p.surgeMultiplier().toPlainString())
                .append("active", p.active());
        if (p.dailyCap() != null) {
            doc.append("dailyCap", p.dailyCap().toPlainString());
        }
        if (p.flatFee() != null) {
            doc.append("flatFee", p.flatFee().toPlainString());
        }
        if (p.earlyBirdInBefore() != null) {
            doc.append("earlyIn", p.earlyBirdInBefore().toString());
            doc.append("earlyOut", p.earlyBirdOutAfter().toString());
        }
        return doc;
    }

    private static TariffPlan tariffFrom(Document d) {
        TariffPlanBuilder b = new TariffPlanBuilder(d.getString("_id"), d.getString("name"),
                TariffKind.valueOf(d.getString("kind")))
                .baseFee(Money.of(d.getString("baseFee")))
                .perHour(Money.of(d.getString("perHour")))
                .graceMinutes(d.getInteger("graceMinutes", 15))
                .surgeMultiplier(new BigDecimal(d.getString("surge")))
                .active(d.getBoolean("active", false));
        if (d.containsKey("dailyCap")) {
            b.dailyCap(Money.of(d.getString("dailyCap")));
        }
        if (d.containsKey("flatFee")) {
            b.flatFee(Money.of(d.getString("flatFee")));
        }
        if (d.containsKey("earlyIn") && d.containsKey("earlyOut")) {
            b.earlyBirdWindow(LocalTime.parse(d.getString("earlyIn")),
                    LocalTime.parse(d.getString("earlyOut")));
        }
        return b.build();
    }

    private static Document userTo(User u) {
        return new Document("_id", u.username())
                .append("fullName", u.fullName())
                .append("passwordHash", u.passwordHash())
                .append("role", u.role().name());
    }

    private static User userFrom(Document d) {
        return new User(d.getString("_id"), d.getString("fullName"),
                d.getString("passwordHash"), com.parksense.auth.Role.valueOf(d.getString("role")));
    }

    private static Document auditTo(AuditEntry e) {
        return new Document("at", e.at().toString())
                .append("actor", e.actor())
                .append("action", e.action())
                .append("detail", e.detail())
                .append("allowed", e.allowed());
    }
}
