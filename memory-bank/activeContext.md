# Active Context - Anihan SRMS

## Current Phase
**UI Styling Adjustments — Complete, Verified**

## Active Branch
`ui-style/fix`

## Status (April 16, 2026)
All HTML and CSS modified. `./gradlew build -x test` passes. Navbars standardized across the system. 
"ANIHAN SRMS" text has been removed, and the `navbar-logo` has been enlarged (height: 85px) using negative vertical margins to prevent the navbar container from growing.

### What Was Built
- **HTML Cleanup**: Removed `<span class="brand-title">Anihan SRMS</span>` from `admin.html`, `add-user.html`, `edit-user.html`, `logs.html`, `student-records.html`, and `subjects.html`. Removed `max-height` inline style restriction from `logs.html`.
- **CSS Standardization**: Enlarged `.navbar-logo` height to `85px` and applied top/bottom negative margins (`-22px`) in `css/login.css`. This ensures the logo is clearly readable without disproportionately expanding the `login-navbar` element's height.
- **CSS CleanUp**: Removed unused `.brand-title` class from `css/dashboard.css`.

## Verified
- `./gradlew build -x test` → BUILD SUCCESSFUL
- No remaining "Anihan SRMS" spans in dashboard navbar code.
