# Tech Context — Anihan SRMS

## Deployment & Infrastructure Constraints
- **Hosting Environment**: Dedicated on-premise server located strictly within the Registrar's office.
- **Network Setting**: Completely isolated from the public internet (Local Area Network ONLY) to prevent external cyber threats. No mobile accessibility (Desktop/Laptop use only).
- **Server Specifications**: High-performance multi-core processor, **Windows Server 2025 Standard**, min 16GB RAM. Data stored securely on a high-capacity SSD, backed up redundantly on a secondary HDD. 
- **Connectivity**: Local workstations link via CAT6 Ethernet switch. Uninterruptible Power Supply (UPS) is mandatory to prevent dataloss during encodings. 

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
