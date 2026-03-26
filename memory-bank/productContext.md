# Product Context — Anihan SRMS

## School Context & Operating Environment
Anihan Technical School is an all-female Technical and Vocational Education and Training (TVET) institute operating for over forty years, founded by the Foundation for Professional Training, Inc. (FPTI). The school currently offers the Culinary Arts and Restaurant Services (CARS) course for roughly 160 active students, split into two alternating batches per year. Students receive a 1-year scholarship paid by partner companies (theoretical for 6 months, practical/OJT for 3 months). 

## Main Users & Departments
- **Registrar**: Central user handling document verification, file digitization, grade compiling, and TESDA SO processing.
- **Admin**: System manager assigning roles, handling user access, viewing activity logs, and managing subjects/batches.
- **Trainer (Faculty)**: Teaches theoretical subjects, evaluates students, and digitally inputs class grades.
- **Student**: Applicant/Enrollee submitting personal details online via the SRMS, gaining read-only access post-enrollment.
- **Recruitment**: Handles initial inquiries and onboarding.

## Current Workflow Pain Points
- The Registrar is a single point of failure manually copying/pasting across multiple sources (Google Forms, Excel, Physical Index Cards).
- Vulnerability to Water Leaks & Physical Damage: Student data since 2009 is at significant risk stored in cabinet folders.
- Overloaded Personnel: The Registrar also handles billing, orientation, scholarship management, and IT infrastructure.

## Business & Compliance Context
**TESDA Special Orders (SO)** are legally required to clear students for graduation. Compiling SOs demands an extensive checklist per student (e.g., TOR, Permanent Record, PSA Birth Certificate, Certificate of TVET program, OJT Report, start/end dates, etc.). Filing an SO manually takes immense effort; TESDA processing alone can take 5 months. The system must consolidate these documents centrally so the Registrar can bulk-encode them without error.

## Key Workflows
1. **Authentication:** Secure RBAC login for Student, Trainer, Registrar, and Admin.
2. **Student Enrollment & Record Capture:** Students submit details, Registrar cross-checks.
3. **Academic Evaluation:** Trainers input final grades directly.
4. **Document Digitization & Batch Encoding:** Registrar uploads batches of PDFs/images linked to students.
5. **TESDA SO Processing:** The system allows Registrar to monitor requirement completeness prior to SO submission.
