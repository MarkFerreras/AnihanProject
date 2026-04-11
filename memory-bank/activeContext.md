# Active Context - Anihan SRMS

## Current Phase
**Admin User Management Enhancement — All Phases Complete**

## Active Branch
`feature/admin-password-delete-hover-fix`

## Status (April 2026)
All 3 phases complete. Build passes. Browser-tested and verified.

### Phase 1 (Done)
Admin password reset, soft/hard delete, button hover fix, 8-char minimum.

### Phase 2 (Done)
Re-enable button, password_changed_at timestamp, password visibility toggle, strong password policy.

### Phase 3 (Done)
- **Button hover fix (permanent)**: All custom buttons now use Bootstrap 5.3 CSS custom properties (`--bs-btn-color`, `--bs-btn-hover-color`, etc.) instead of regular `color`/`background` declarations. This permanently prevents Bootstrap's `.btn:hover` from overriding text colors. Fixed: `.btn-surface`, `.btn-surface-secondary`, `.btn-danger-surface`, `.btn-deactivate`, `.btn-permanent-delete`, `.btn-reenable`.
- **Admin username edit**: Username field on Edit User page is now editable with no-spaces validation at 3 levels: HTML pattern, client-side JS, backend @Pattern.

## Verified
- `./gradlew build` → BUILD SUCCESSFUL (7/7 tasks, all tests green)
- **Browser-tested**: Re-enable button green with white text on hover ✅, Delete button red with white text on hover ✅, Username field editable ✅
