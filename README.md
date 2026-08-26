<div align="center">

# 🅿️ ParkSense

### **Smart Parking Management System**

*A multi-floor parking facility — ANPR-simulated gates, a live bay-level map, tariffed
ticketing, monthly members and a real-time control room — engineered around
**16 Gang-of-Four design patterns** that each carry real load in a real flow.*

**CSE 463 : Software Design Pattern Make-up Assignment** · Summer 2026

<img src="https://img.shields.io/badge/Java-17-%23f89820?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 17">
<img src="https://img.shields.io/badge/Spring_Boot-3.4-%236DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot 3.4">
<img src="https://img.shields.io/badge/MongoDB-Atlas-%2347A248?style=for-the-badge&logo=mongodb&logoColor=white" alt="MongoDB">
<img src="https://img.shields.io/badge/Design_Patterns-16-%23818cf8?style=for-the-badge" alt="16 Patterns">
<img src="https://img.shields.io/badge/Tests-91%20passing-%233ee6a0?style=for-the-badge" alt="Tests">
<img src="https://img.shields.io/badge/UI-Dark%20Glass%20SPA-%2338d9f5?style=for-the-badge" alt="UI">

**[Quick Start](#-quick-start) · [Demo Script](#-demo-script) · [Architecture](#%EF%B8%8F-3d-architecture) · [16 Patterns](#-the-16-design-patterns) · [Roles](#-roles--security)**

</div>

---

## ✨ Highlights

|  | Feature | What it does |
|---|---|---|
| 🗺️ | **Live Parking Map** | All **132 bays** across 3 floors, colour-coded by state, with a click-to-inspect **Bay Inspector** panel |
| 🚧 | **Gate Simulator** | ANPR camera-read entry, animated barrier arms, entry-chain verdict trace, undoable command queue |
| 🎫 | **Ticketing & Checkout** | Add-on services that stack as fee lines, cash/card/mobile settlement with change, printable receipt |
| 💰 | **Tariff Engine** | Five interchangeable strategies — hourly, daily cap, early-bird, event surge, member pass |
| 👥 | **Members & Express Lane** | Monthly passes; recognised plates exit at zero through the express lane |
| 📊 | **Reports** | Revenue, occupancy, utilisation, peak hours, add-on uptake — tables, charts and CSV export |
| 🚨 | **Control Room** | KPI tiles with count-up animation, LED boards, capacity alerts, live event feed |
| 🛡️ | **Guarded Overrides** | ADMIN-only barrier force-open/close behind a **protection proxy** with an append-only audit trail |
| 🧩 | **Pattern Catalogue** | The system documents its own 16 patterns — served live from the code (`dev` role) |
| 💾 | **Atlas Persistence** | Write-behind sync into a dedicated `parksense` database — state survives restarts |

---

## 🚀 Quick Start

> **Requires JDK 17+ — nothing else.** No database, no config, no build tools to install.

```powershell
.\mvnw.cmd spring-boot:run        # → http://localhost:8080
```

<details>
<summary><b>💾 Optional — enable MongoDB persistence</b></summary>

Copy `.env.example` → `.env` and set your Atlas connection string:

```ini
MONGODB_URI=mongodb+srv://USER:PASSWORD@YOUR-CLUSTER.mongodb.net/?appName=ParkSense
MONGODB_DB=parksense
```

State (tickets, members, tariffs, users, audit trail, bay maintenance, even the
ticket-number sequence) then lives in a dedicated **`parksense`** database and
**survives restarts**. If the cluster is unreachable, ParkSense logs a friendly
warning and falls back to zero-setup in-memory mode — the demo never breaks.

</details>

<details>
<summary><b>🧪 Run the test suite</b></summary>

```powershell
.\mvnw.cmd clean test     # 91 tests · 15 classes · 0 failures
```

</details>

### 🔑 Demo Accounts

| Username | Password | Role | Access |
|:--------:|:--------:|:-----|:-------|
| `admin` | `admin123` | **ADMIN** | Everything — tariff editing, barrier override, voids, audit trail |
| `operator` | `operator123` | **OPERATOR** | Lanes, tickets, checkout, live views |
| `dev` | `dev123` | **DEVELOPER** | Everything above **+ the live Design-Patterns catalogue tab** |

> The system boots fully seeded: 132 bays, ~35 % occupied right now, a week of
> ticket history (so reports have data on first load), 4 members, 2 blacklisted
> plates and 5 tariff plans (3 active).

---

## 🎬 Demo Script

1. **Gates → Trigger ANPR** — a car enters: watch the entry chain verdict
   (✓/✗ per rule), the barrier arm animate, the command queue fill, and the
   map bay turn red within seconds.
2. Enter plate **`STOLEN-01`** — refused with `VEHICLE BLOCKED` right on the
   lane display.
3. **Tickets → pick the new ticket → add-ons → Pay** — fee lines stack
   (Decorator in action), the receipt prints, a 15-minute exit grace begins.
4. **Gates → GATE-OUT-1 → Process Exit** (ticket no. or plate) — the slot
   frees and the map turns green.
5. Sign in as `operator` and press **Force open** — the protection proxy
   refuses with **403** and writes an audit line; as `admin` it works.
6. **Reports** — revenue by day, peak-hour histogram, utilisation + CSV export.
7. Sign in as `dev` → **Patterns** — all 16 patterns with their concrete
   classes and the flows that exercise them.

---

## 🏗️ 3D Architecture

```
                          ┌───────────────────────────────────────────────┐
                          │            🖥️  PRESENTATION TIER              │
                          │     Vanilla-JS SPA · 9 views · dark glass     │
                          │   hash router · 3-second live polling store   │
                      ┌───┴───────────────────────────────────────────────┘
                      │             🔐  APPLICATION EDGE
                      │   TokenAuthFilter · 9 REST controllers · 36 routes
                      │        everything behind ONE OperationsFacade
                  ┌───┴───────────────────────────────────────────────────┘
                  │              ⚙️   PATTERN DOMAIN — plain Java 17
                  │  ControlRoomMediator ── Entry Chain ── TariffSelector
                  │  OccupancyLedger ── Lot Composite ── Gate Commands
                  │  Ticket State Machine ── Fee Decorators ── Visitors
              ┌───┴───────────────────────────────────────────────────────┘
              │                  💾   DATA TIER
              │    7 in-memory stores ⇄ MongoSync (write-behind) ⇄ Atlas
              └─────────────────────────────────────────────────────────────┘
```

**Zero framework annotations in the domain.** Spring appears only in the boot
class, one `@Configuration` (`AppConfig`), thin controllers and one filter —
every pattern stays ordinary, readable, unit-testable Java.

---

## 🧩 The 16 Design Patterns

| # | Pattern | Where it lives |
|:---:|---|---|
| 1 | **Singleton** | `occupancy.OccupancyLedger` — one truth for slot states |
| 2 | **Factory Method** | `gates.hardware.GateHardwareFactory` → Simulated / Vendor families |
| 3 | **Builder** | `tariff.builder.TariffPlanBuilder` — invalid plans are unrepresentable |
| 4 | **Adapter** | `hardware.adapter.*` — PlateSense ANPR camera, Parktron sensors & barrier |
| 5 | **Decorator** | `tickets.addon.*` — car wash / valet / EV top-up / lost-ticket penalty |
| 6 | **Facade** | `app.OperationsFacade` — one method per UI action |
| 7 | **Proxy** | `guard.GateControlProxy` — ADMIN-only override, audited denials |
| 8 | **Composite** | `lot.*` — ParkingLot → Floor → Zone → Slot |
| 9 | **Mediator** | `controlroom.ControlRoomMediator` — coordinates gates, kiosk, boards, sensors |
| 10 | **Observer** | `occupancy.*` — events fan out to boards, feed, capacity alerts |
| 11 | **Strategy** | `tariff.strategy.*` — hourly, daily cap, early-bird, surge, member pass |
| 12 | **Command** | `gates.command.*` — queued, audited, undoable barrier operations |
| 13 | **Template Method** | `exitlane.ExitProcessor` → staffed / express / lost-ticket lanes |
| 14 | **Chain of Responsibility** | `entrycheck.*` — 6 ordered entry rules, first failure shown |
| 15 | **State** | `tickets.state.*` — ISSUED → ACTIVE → PAID → EXITED (+ LOST, VOID) |
| 16 | **Visitor** | `reports.visitor.*` — every report walks the domain |

*Also served live by the system itself:* `GET /api/system/patterns` → **Patterns** tab (`dev` login).

---

## 🔐 Roles & Security

| Control | Mechanism |
|---|---|
| Passwords | **PBKDF2-HMAC-SHA256**, 120 k iterations, per-user salt — plaintext never stored |
| Sessions | Opaque random **bearer tokens**, invalidated on logout / restart |
| Route guards | `TokenAuthFilter` publishes the caller to a thread-local role context |
| Barrier override | Enforced **at the object boundary** by the protection proxy — not just in the UI |
| Audit | **Append-only** trail: every command, denial, payment, void and override leaves a line |
| Money | `BigDecimal` (BDT, 2 dp) — floating point never touches money |

---

## 📂 Project Structure

```
ParkSense/
├── 📄 ParkSense-Report.tex        # the full LaTeX report (compile on Overleaf)
├── 🖼️ figs/                       # 10 UI screenshots used by the report
├── 📊 diagrams/                   # 17 rendered Mermaid diagrams
└── 📦 src/
    ├── main/java/com/parksense/
    │   ├── app/          # AppConfig wiring, SeedData, OperationsFacade  [Facade]
    │   ├── lot/          # lot tree                                    [Composite]
    │   ├── occupancy/    # ledger [Singleton], boards, feed, alerts     [Observer]
    │   ├── controlroom/  # mediator + colleagues                       [Mediator]
    │   ├── gates/        # lanes, command queue [Command], guard       [Proxy]
    │   ├── hardware/     # SPI ports, vendor SDKs, adapters            [Adapter]
    │   │   └── hardware/ # + factory families                    [Factory Method]
    │   ├── entrycheck/   # 6 ordered entry rules            [Chain of Resp.]
    │   ├── exitlane/     # exit pipeline                      [Template Method]
    │   ├── tickets/      # state machine [State], fee add-ons   [Decorator]
    │   ├── tariff/       # selector [Strategy], plan builder        [Builder]
    │   ├── members/      # pass registry
    │   ├── reports/      # visitors [Visitor], CSV renderer
    │   ├── store/        # 7 in-memory repositories
    │   ├── persistence/  # MongoSync write-behind bridge → Atlas
    │   ├── auth/         # PBKDF2, tokens, 3 roles
    │   ├── audit/        # append-only trail
    │   └── api/          # 9 thin REST controllers + DTOs
    ├── main/resources/static/   # vanilla-JS SPA — dark-glass control room
    └── test/java/               # 15 test classes · 91 tests
```

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Java 17 (records, switch expressions) |
| **Framework** | Spring Boot 3.4 — `spring-boot-starter-web` only |
| **Persistence** | In-memory repositories + `mongodb-driver-sync` 5.2 (optional Atlas mode) |
| **Frontend** | Hand-written vanilla JS + CSS — zero dependencies, zero CDNs |
| **Testing** | JUnit 5 + MockMvc — 91 tests across 15 classes |
| **Build** | Apache Maven (wrapper bundled — one command runs everything) |

---

## 📄 Report

The full project report (`ParkSense-Report.tex`) follows the BUBT six-chapter
format: analysis & design (use-case, DFD, ER, sequence, activity, state
diagrams), all 16 patterns mapped to classes and flows, implementation,
user manual with screenshots, and future work. Upload `ParkSense-Report.zip`
to [Overleaf](https://overleaf.com) as a new project → compiler **pdfLaTeX**
→ recompile twice.

---

<div align="center">

**ParkSense** · built for the CSE 463 Software Design Pattern Make-up Assignment · Summer 2026

*Every pattern load-bearing. Every rule visible. Every override audited.* 🅿️

</div>
