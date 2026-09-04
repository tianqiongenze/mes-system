# 智能工厂 MES 系统 (Smart Factory Manufacturing Execution System)

> Student: `student-java` — Language: Java 17 + Spring Boot 3.2.5 + Maven
> Architecture: Microservices (TOGAF Application Architecture)

A complete Manufacturing Execution System (MES) for an industrial smart factory,
implemented as five independently deployable Spring Boot microservices. Each
service follows the layered Controller / Service / Repository / Entity pattern
and ships with comprehensive JUnit 5 unit tests (mocked repositories) plus
Spring Boot integration tests backed by an in-memory H2 database.

## Modules

| Service | Port | Package | Responsibility |
|---------|------|---------|----------------|
| `gateway-service` | 8080 | `com.mes.gateway` | Spring Cloud Gateway edge router, routes `/api/v1/{production,quality,equipment,inventory}/**` |
| `production-service` | 8081 | `com.mes.production` | Production order lifecycle: create → start → progress → complete/cancel, yield-rate metrics |
| `quality-service` | 8082 | `com.mes.quality` | Inspection records, first-pass-yield (FPY), defect rate |
| `equipment-service` | 8083 | `com.mes.equipment` | Equipment registry, telemetry ingestion, predictive fault detection, OEE |
| `inventory-service` | 8084 | `com.mes.inventory` | Material master data, stock movements (IN/OUT/ADJUST), reorder/safety stock, inventory value |

## Build & Test

```bash
# from project root (mes-system/)
mvn clean test            # compile + run all unit & integration tests
mvn -pl production-service test     # run tests for a single module
mvn -pl equipment-service -am package  # build one service jar with deps
```

The build requires Java 17 and Maven 3.8+. Dependencies resolve from the Aliyun
Maven mirror (`https://maven.aliyun.com/repository/public`). Tests use an
in-memory H2 database, so no external database is required.

## Running a service

```bash
cd production-service
mvn spring-boot:run   # starts on http://localhost:8081
```

## API Example

```bash
# Create a production order
curl -X POST localhost:8081/api/v1/production/orders \
  -H 'Content-Type: application/json' \
  -d '{"productCode":"P-A100","quantity":100,"priority":"HIGH"}'
# Start production
curl -X POST localhost:8081/api/v1/production/orders/<id>/start
# Report progress (60 good, 2 defects)
curl -X POST "localhost:8081/api/v1/production/orders/<id>/progress?completed=60&defects=2"
# Yield-rate metric
curl localhost:8081/api/v1/production/orders/metrics/yield-rate
```

## Documentation
See `docs/SDD.md` for the full Software Design Document (TOGAF architecture,
class & sequence diagrams, module specifications) and `docs/diagrams/` for
PlantUML sources.

## Test-Driven Development
Tests were written alongside/preceding the implementation: pure unit tests
mock the JPA repositories (`@Mock` + `Mockito`), while `*IT` classes verify
the full HTTP+JPA stack with `MockMvc` + H2. Run coverage with
`mvn test jacoco:report` (add the jacoco plugin if desired).
