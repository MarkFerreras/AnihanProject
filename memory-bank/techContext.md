# Tech Context — Anihan SRMS

## Deployment & Infrastructure Constraints
- **Hosting Environment**: Dedicated on-premise server located strictly within the Registrar's office.
- **Network Setting**: Completely isolated from the public internet (Local Area Network ONLY) to prevent external cyber threats. No mobile accessibility (Desktop/Laptop use only).
- **Server Specifications**: High-performance multi-core processor, **Windows Server 2025 Standard**, min 16GB RAM. Data stored securely on a high-capacity SSD, backed up redundantly on a secondary HDD. 
- **Connectivity**: Local workstations link via CAT6 Ethernet switch. Uninterruptible Power Supply (UPS) is mandatory to prevent dataloss during encodings. 

## Demo/Staging Hosting on Render (2026-09-21, branch `render-test1`)
**Does not replace the on-premise target above.** A separate, publicly-reachable
demo/staging instance, deployed via Docker (Render has no native Java/Gradle runtime —
JVM apps there run as Docker images). Key differences from the on-premise setup:
- **Database:** Render has no managed MySQL. The app now reads
  `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` from the environment (falling back to the local
  Docker MySQL values for on-premise dev), so any external MySQL 8 host can be plugged in —
  none is committed.
- **Build:** `Dockerfile` — `eclipse-temurin:25-jdk` build stage, `eclipse-temurin:25-jre`
  run stage. `render.yaml` is the Render Blueprint (`runtime: docker`).
- **Port:** `server.port=${PORT:8080}` — Render assigns the port at runtime.
- **Client IP:** `server.forward-headers-strategy=native` added so `system_logs.ip_address`
  resolves the real client IP through Render's edge proxy.
- See `memory-bank/activeContext.md` (2026-09-21 session) for full detail.

## Confirmed Software Stack
| Layer       | Technology                |
|-------------|---------------------------|
| Front-End   | HTML5, CSS3, JS ES2024    |
| UI          | Bootstrap 5.3, DataTables 2, jQuery 4.0 |
| Back-End    | Java 25, Spring Boot 4.0  |
| Security    | Spring Security 7         |
| Validation  | Spring Validation         |
| Data        | Spring Data JPA           |
| Database    | MySQL 8 (Docker)          |
| Build       | Gradle 9.4.1 (Kotlin DSL) |
| IDE         | Antigravity               |

> **[Confirmed Stack Choice]**: The documented capstone research (`Chapter 3`, Coding Phase) lists **Python** as the primary programming language. However, the user has explicitly confirmed that the project will use **Java 25 and Spring Boot 4.0**. Work will proceed strictly with the Java/Spring Boot stack.

## Front-End Assets Structure
All libraries are served completely locally (no internet required):
- `src/main/resources/static/css/` (bootstrap, datatables, custom css)
- `src/main/resources/static/js/` (jquery, bootstrap bundle, datatables)
- Images/Logos stored locally to prevent external API calls.
