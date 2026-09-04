# Software Design Document (SDD)
## 智能工厂 MES 系统 — Smart Factory Manufacturing Execution System

**Project:** MES System &nbsp;&nbsp; **Student:** student-java &nbsp;&nbsp;
**Language:** Java 17 &nbsp;&nbsp; **Framework:** Spring Boot 3.2.5 &nbsp;&nbsp;
**Build:** Maven multi-module &nbsp;&nbsp; **Architecture:** Microservices (TOGAF)

> AI-assisted design: high-level module contracts and metric formulas were
> generated/validated against the qwen2.5-coder:7b model served by the
> platform LiteLLM gateway (`http://litellm.ai-platform.svc.cluster.local:4000/v1`).
> Implementation, tests and this document were completed by hand following
> those verified specifications.

---

## 1. Introduction

### 1.1 Purpose
This Software Design Document describes the architecture, components,
interfaces and test strategy of the Smart Factory MES — a Manufacturing
Execution System that digitises the four core loops of a modern factory:
production scheduling, quality control, equipment monitoring and inventory
management. The system is delivered as five Spring Boot microservices behind
an API gateway so that each business capability can be developed, deployed
and scaled independently.

### 1.2 Scope
The MES covers the production-to-stock scenario end-to-end: a production order
is created, started on a work centre, receives quality inspections, consumes
materials from inventory and reports equipment telemetry. Cross-cutting metrics
(yield rate, first-pass yield, defect rate, OEE, inventory value) are computed
from the operational data. Out of scope: ERP/billing integration, MES-MOM
historian warehousing, and shop-floor PLC protocols — these are intentionally
left to the upstream SCADA layer.

### 1.3 Definitions and Acronyms
- **MES** — Manufacturing Execution System
- **TOGAF** — The Open Group Architecture Framework (used here for the
  Application Architecture viewpoint)
- **FPY** — First Pass Yield = passed/(passed+failed) × 100
- **OEE** — Overall Equipment Effectiveness = Availability × Performance × Quality
- **SKU** — Stock Keeping Unit
- **DTO** — Data Transfer Object
- **TDD** — Test-Driven Development

### 1.4 References
- Spring Boot 3.2.x reference documentation
- TOGAF 9.2 Application Architecture
- ISO 9001 (quality management) and IEC 62264 (MES integration of enterprise-control)

---

## 2. System Architecture (TOGAF Application Architecture)

### 2.1 Architectural Style
The MES is a microservices application. Each business capability is an
independently deployable Spring Boot service exposing a REST API. An edge
gateway (Spring Cloud Gateway) routes inbound HTTP traffic by path prefix and
decouples clients from the physical service topology. Inter-service
communication is synchronous HTTP (for queries) — kept deliberately simple so
the services can be tested in isolation. Data is private per service: each
service owns its aggregate tables (production_orders, inspection_records,
equipment, materials), enforcing the "share-nothing database" principle of
TOGAF's Application Platform services.

### 2.2 Service Catalogue

| # | Service | TOGAF Layer | Aggregates Owned |
|---|---------|-------------|------------------|
| 1 | gateway-service | Application Platform (edge) | none — routing only |
| 2 | production-service | Business Process (core) | ProductionOrder |
| 3 | quality-service | Business Process (core) | InspectionRecord |
| 4 | equipment-service | Business Process (core) | Equipment |
| 5 | inventory-service | Business Process (core) | Material |

### 2.3 Layered Architecture (per service)
Every business service applies the same four-layer pattern:

```
Controller (REST, DTO mapping, validation, HTTP status)
   │
Service   (business logic, transactional, metrics, invariants)
   │
Repository (Spring Data JPA, port interface for persistence)
   │
Entity    (JPA-mapped aggregate root)
```

- The **Controller** layer never touches the repository directly and never
  returns entities; it returns DTOs (Java `record`s) so the on-wire contract is
  decoupled from persistence.
- The **Service** layer is the transaction boundary (`@Transactional`) and
  encodes the domain invariants (e.g. "only CREATED orders can start",
  "passed+failed must equal sampleSize", "OUT cannot drive stock negative").
- The **Repository** is a Spring Data JPA interface — a TOGAF "port" that the
  H2/PostgreSQL adapter plugs into.
- The **Entity** is the only component aware of JPA.

This separation is what makes the services unit-testable: services are tested
with a mocked repository (fast, no I/O), and a separate `*IT` class tests the
whole stack through `MockMvc` against H2.

### 2.4 Deployment View
Services are packaged as executable JARs (`spring-boot-maven-plugin`) and
are language-level runnable (`mvn spring-boot:run`). In the cluster they run
inside the JupyterHub student pod; in production they map 1:1 to Kubernetes
Deployments behind an Ingress. Each service exposes Spring Boot Actuator
`/actuator/health` for liveness/readiness probes.

### 2.5 TOGAF Architecture Diagram

```
                        +-----------------------------+
                        |        Clients / UI         |
                        +--------------+--------------+
                                       |
                        +--------------v--------------+
                        |   gateway-service :8080     |   (Spring Cloud Gateway)
                        |  route by /api/v1/{domain}   |
                        +--+-------+-------+-------+--+
                           |       |       |       |
            +--------------+   +---+   +---+   +---+--------------+
            |                  |       |       |                  |
   +--------v------+   +-------v---+ +-v-----+ +--v-------+ +-----v------+
   | production-   |   | quality-  | |equip- | |inventory-| |  (future)   |
   | service :8081 |   | service   | |ment   | | service  | |            |
   |               |   | :8082     | |:8083   | | :8084    | |            |
   +-------+-------+   +-----+-----+ +---+----+ +-----+----+ +------------+
           |                 |           |            |
   +-------v-------+  +------v------+ +--v---+ +------v------+
   | H2/PostgreSQL |  | H2/Postgres  | |H2/Pg | | H2/Postgres |
   | production_    |  | inspection_  | |equip | | materials   |
   | orders        |  | records      | |      | |             |
   +---------------+  +--------------+ +------+ +-------------+
```

---

## 3. Module Specifications

### 3.1 Production Service (`com.mes.production`)
**Aggregate:** `ProductionOrder` (id, productCode, quantity, status, priority,
workCenter, operator, completedQuantity, defectCount, planned/actual start-end).

**State machine:**
```
   create()           start()         reportProgress() >= quantity
CREATED ────────► IN_PROGRESS ─────────────────────────► COMPLETED
   │                  │
   └────cancel()──► CANCELLED   (also from IN_PROGRESS)
```
Invariants: priority ∈ {LOW,NORMAL,HIGH,URGENT}; only CREATED→IN_PROGRESS;
progress only on IN_PROGRESS; cannot cancel COMPLETED; completed+defects
accumulate; auto-complete when completed≥quantity.

**Metrics:** yield rate = (completedQty−defects)/completedQty×100.

### 3.2 Quality Service (`com.mes.quality`)
**Aggregate:** `InspectionRecord`. An inspection belongs to a production order
and records sampleSize, passed, failed, result, inspector, defectType.
Invariant: passed+failed == sampleSize. Result is PASS when failed==0 else FAIL.
**Metrics:** FPY = passed/(passed+failed)×100; defectRate = failed/total×100.

### 3.3 Equipment Service (`com.mes.equipment`)
**Aggregate:** `Equipment` (code, name, status, location, temperature,
vibration, rpm, utilizationRate, lastMaintenance). Telemetry is ingested and a
predictive rule auto-flags FAULT when temperature ≥95°C or vibration ≥8.0 mm/s
(critical), or MAINTENANCE when ≥85°C / ≥5.0 (warning). IDLE↔RUNNING transitions
follow rpm. Utilization accrues while RUNNING.
**Metric:** OEE ≈ Availability × Utilization × Performance (simplified).

### 3.4 Inventory Service (`com.mes.inventory`)
**Aggregate:** `Material` (sku, name, unit, quantity, reorderPoint,
safetyStock, unitCost, warehouse). `StockMovement` (type IN/OUT/ADJUST)
mutates quantity transactionally; OUT cannot drive stock below zero. Low-stock
list = quantity ≤ reorderPoint; critical = quantity ≤ safetyStock.
**Metric:** inventory value = Σ(quantity × unitCost).

### 3.5 Gateway Service (`com.mes.gateway`)
Spring Cloud Gateway route table mapping path prefixes to service URIs. Pure
infrastructure — no business state — making it trivially replaceable by an
Ingress or service mesh, per TOGAF's guidance to keep the Application Platform
separate from business logic.

---

## 4. Class Diagram

```
ProductionOrder                InspectionRecord            Equipment
─────────────────             ───────────────             ───────────
id : String                   id : String                 id : String
productCode : String          productionOrderId : String  code : String
quantity : int                productCode : String        name : String
status : String               sampleSize : int            status : String
priority : String             passed : int                temperature : double
completedQuantity : int        failed : int                vibration : double
defectCount : int             result : String             rpm : int
workCenter : String           inspector : String          utilizationRate : double
assignedOperator : String     defectType : String         location : String
plannedStart/End : DateTime  inspectedAt : DateTime      lastMaintenance : DateTime
actualStart/End : DateTime
createdAt : DateTime
       ▲                            ▲                            ▲
       │ JpaRepository              │ JpaRepository              │ JpaRepository
       │                            │                            │
ProductionOrderRepository      InspectionRepository         EquipmentRepository
                               Material
                               ────────
                               id, sku, name, unit, quantity,
                               reorderPoint, safetyStock, unitCost,
                               warehouse, updatedAt
                                  ▲ JpaRepository
                                  │
                              MaterialRepository
```

Each service has a `Service` (business logic) + `Controller` (REST) pair that
depends on its repository; DTO `record`s (`ProductionOrderRequest/Response`,
`InspectionRequest`, `TelemetryRequest`, `StockMovementRequest`) bound the
on-wire contracts.

---

## 5. Sequence Diagrams

### 5.1 Create and start a production order
```
Client            Controller        Service           Repository        DB
  │  POST /orders   │  create(req)    │                   │            │
  │──────────────► │───────────────► │ save(order)      │            │
  │                 │                 │──────────────────►│ insert     │
  │                 │                 │                   │──────────►│
  │                 │                 │◄──────────────────│            │
  │                 │ 201 + resp      │◄──────────────    │            │
  │◄────────────── │◄────────────────│                                │
  │  POST /{id}/start                 │                   │            │
  │──────────────► │ start(id)       │ findById(id)      │            │
  │                 │────────────────►│──────────────────►│ select     │
  │                 │                 │ set IN_PROGRESS   │            │
  │                 │                 │ save(order) ──────►│ update    │
  │ 200 + resp     │◄────────────────│◄──────────────────│            │
  │◄────────────── │                                                    │
```

### 5.2 Equipment telemetry ingestion (predictive fault)
```
PLC/Monitor        EquipmentController     EquipmentService     Repository
  │  POST /{id}/telemetry                         │              │
  │  {temperature:96, vibration:2, rpm:1500}      │              │
  │────────────────────►│ ingestTelemetry(id,t)  │              │
  │                       │────────────────────► │ findById(id) │
  │                       │                       │─────────────►│
  │                       │                       │ predictStatus(t) → FAULT
  │                       │                       │ setStatus(FAULT)
  │                       │                       │ save(e) ─────►│
  │  200 + equipment     │◄──────────────────────│◄────────────│
  │◄─────────────────────│
```

### 5.3 Stock movement with invariant check
```
Client  InventoryController  InventoryService   MaterialRepository
  │ POST /movements {sku,OUT,10}        │                │
  │──────────►│ moveStock(req)         │                │
  │            │──────────────────────►│ findBySku(sku) │
  │            │                        │───────────────►│
  │            │                        │ quantity=3 < 10 → IllegalStateException
  │  409/conflict ◄────────────────────│◄───────────────│
```

---

## 6. Test Strategy (TDD)

Tests are first-class artifacts and are organised in two tiers:

1. **Unit tests** (`*ServiceTest`) — mock the JPA repository with Mockito, so
   they execute in milliseconds with no Spring context. They assert every
   branch of every business rule: state-machine transitions, invariant
   violations, metric formulas, parameterised threshold rules. Coverage
   targets: 100% of service public methods, all exception paths.
2. **Integration tests** (`*IT`, `@SpringBootTest + MockMvc`) — verify the
   full HTTP→Controller→Service→JPA→H2 stack including JSON serialization,
   validation, HTTP status codes and exception handlers.

The test catalogue by service:
- production: 15 unit tests + 2 integration tests (create/retrieve/start/
  progress/complete/yield-rate, validation)
- quality: 7 unit tests (PASS/FAIL recording, count mismatch, metrics)
- equipment: 7 unit tests including parameterised `predictStatus` table
- inventory: 7 unit tests (IN/OUT/ADJUST, insufficient stock, unknown SKU)
- gateway: 1 context-loads test (route configuration validity)

Run: `mvn clean test`.

---

## 7. Non-Functional Requirements

- **Performance:** stateless services scale horizontally; JPA query methods use
  indexed lookups (findByCode/findBySku) avoiding table scans.
- **Security:** validation on every input (`jakarta.validation`), a
  `@RestControllerAdvice` maps domain exceptions to 400/409, no raw entity
  exposure.
- **Observability:** Actuator health/info/metrics endpoints on every service.
- **Portability:** H2 in dev/test, switchable to PostgreSQL by changing the
  `spring.datasource` properties — no code changes.
- **Maintainability:** the uniform four-layer template means a new
  capability (e.g. maintenance scheduling) is added by copying the module
  scaffold.

---

## 8. Future Work
- Replace H2 with PostgreSQL per service; add Flyway migrations.
- Introduce an event bus (e.g. completion events → inventory auto-deduction).
- Add a React/Vue operator dashboard consuming the gateway.
- Continuous integration: `mvn verify` + JaCoCo coverage gate in CI.

## 9. Revision History
| Version | Date | Author | Notes |
|---------|------|--------|-------|
| 1.0 | 2026-08-31 | student-java | Initial SDD, 5 microservices, full test suite |
---

## 10. Design Rationale & Trade-offs

### 10.1 Microservices over a monolith
Each business capability (production, quality, equipment, inventory) is an
independently deployable service. The rationale is independent lifecycles:
quality inspectors rarely change at the same cadence as production schedulers,
and equipment telemetry scales differently from material master data. The
cost is operational — five deployments instead of one, inter-service network
calls, and per-service database schemas that cannot share joins. This cost is
paid deliberately because the alternative (a single monolithic MES) couples
release trains and forces big-bang deploys that the factory floor cannot
tolerate. The "share-nothing database" rule is the hard boundary that keeps
the services truly independent: no service may reach into another's tables,
so a refactor of the production schema cannot break quality reporting.

### 10.2 In-memory H2 for dev/test, PostgreSQL for prod
Every service uses H2 with `ddl-auto: update` in development and
`create-drop` in tests, so the suite runs with zero external infrastructure.
The JPA abstraction means switching to PostgreSQL is a single
`spring.datasource` property change — no code touch. This is what makes the
integration tests (`*IT`) both realistic (they exercise the full
Controller→Service→JPA→SQL stack) and hermetic (each test gets a fresh
in-memory database, no state leaks between tests).

### 10.3 DTOs as Java records
The API boundary returns Java `record`s rather than JPA entities. This is a
defence-in-depth measure: even if a developer accidentally adds a sensitive
field to an entity, it cannot leak over HTTP because the entity is never
serialized directly. The record also makes the on-wire contract explicit and
immutable, which simplifies client code generation and contract testing.

### 10.4 Mocked repositories in unit tests
`*ServiceTest` classes mock `JpaRepository` with Mockito. This makes the
service tests fast (milliseconds, no Spring context startup) and lets them
assert every branch of the business logic in isolation. The complementary
`*IT` classes then test the wiring — that the controller binds the request,
the service is injected, and the JPA layer persists correctly. Splitting
unit and integration tests this way keeps the feedback loop tight: a logic
regression fails in seconds, a wiring regression fails in a few seconds more.
