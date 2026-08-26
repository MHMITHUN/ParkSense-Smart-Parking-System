package com.parksense.store;

import com.parksense.tickets.Ticket;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

/** In-memory home of every ticket, open or closed. */
public final class TicketStore {

    private final Map<String, Ticket> byNo = new ConcurrentHashMap<>();
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    /** Persistence hook: called after every mutation (save / clear). */
    public void onChange(Runnable listener) {
        changeListeners.add(listener);
    }

    private void fireChange() {
        changeListeners.forEach(Runnable::run);
    }

    public void save(Ticket ticket) {
        byNo.put(ticket.ticketNo(), ticket);
        fireChange();
    }

    public Optional<Ticket> find(String ticketNo) {
        return Optional.ofNullable(byNo.get(ticketNo));
    }

    public Optional<Ticket> findOpenByPlate(String plateNo) {
        return byNo.values().stream()
                .filter(t -> t.plate().equalsIgnoreCase(plateNo) && t.isOpen())
                .findFirst();
    }

    public List<Ticket> open() {
        return byNo.values().stream().filter(Ticket::isOpen)
                .sorted(Comparator.comparing(Ticket::entryTime).reversed())
                .toList();
    }

    public List<Ticket> all() {
        return byNo.values().stream()
                .sorted(Comparator.comparing(Ticket::entryTime).reversed())
                .toList();
    }

    /** Tickets exited inside a window — the revenue report's source. */
    public List<Ticket> exitedBetween(Instant from, Instant to) {
        return byNo.values().stream()
                .filter(t -> t.exitTime() != null)
                .filter(t -> !t.exitTime().isBefore(from) && t.exitTime().isBefore(to))
                .sorted(Comparator.comparing(Ticket::exitTime))
                .toList();
    }

    public Stream<Ticket> stream() {
        return byNo.values().stream();
    }

    public int size() {
        return byNo.size();
    }

    public void clear() {
        byNo.clear();
        fireChange();
    }
}
