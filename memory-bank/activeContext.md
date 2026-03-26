# Active Context — Anihan SRMS

## Current Phase 
**Research Integration & Backend Auth Refinement** 

## Development Priorities Backed by Capstone
1. **User Authentication Module**: Resolving role-based login (Admin, Registrar, Trainer, Student).
2. **Student Onboarding**: Establishing a form for students to input personal info and trigger a "Pending Registration" state.
3. **Trainer Grading Module**: Providing a filtered view of assigned classes and enforcing numerical grade submission.
4. **Registrar Batch Document Module**: Implementation of the document upload system ensuring files are stored efficiently in the database as BLOBs to prevent physical green folder accumulation.

## Status (March 2026)
- **Completed**: DFD, ERD, and BPMNs identified from `Chapter-1.pdf` and `Chapter-3.pdf` have been synthesized into System Patterns. Required project data dictionary and infrastructure expectations added.
- **In-Progress**: Fixing IDE Java compiler error parsing issues, specifically "String cannot be resolved to a type" in `LoginRequest.java`.
- **Immediate Task**: Refine backend dashboard layouts now that we have exact functional and non-functional requirements (e.g. viewStudentList() load time <= 5 seconds).

## Open Questions & Unverified Items
1. **Tech Stack Adherence**: `Chapter 3` emphasizes the use of Python, but the user has explicitly confirmed continuing with **Java 25 and Spring Boot 4.0**. *Conflict resolved.*
2. **BLOB Storage**: `techContext` indicates document uploads are converted to BLOBs inside the DB. If documents become massive (many MBs each over 40 years), we may want to ask if filesystem secure storage + DB paths is preferred, though diagrams indicate direct database storing. *Currently marked as Confirmed to be encoded into the Database per `Chapter 3` diagrams, but poses a scaling risk.*
