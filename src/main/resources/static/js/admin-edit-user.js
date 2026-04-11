(function () {
    'use strict';

    let isDirty = false;
    let allowNavigation = false;

    function setAlert(id, message, type) {
        const element = document.getElementById(id);
        if (!element) {
            return;
        }

        element.className = 'alert alert-' + type;
        element.textContent = message;
        element.classList.remove('d-none');
    }

    function hideAlert(id) {
        const element = document.getElementById(id);
        if (!element) {
            return;
        }

        element.classList.add('d-none');
        element.textContent = '';
    }

    function readUserId() {
        return new URLSearchParams(window.location.search).get('id');
    }

    function buildPayload() {
        const payload = {
            username: document.getElementById('editUsernameDisplay').value.trim(),
            email: document.getElementById('editEmail').value.trim(),
            role: document.getElementById('editRole').value,
            lastName: document.getElementById('editLastName').value.trim(),
            firstName: document.getElementById('editFirstName').value.trim(),
            middleName: document.getElementById('editMiddleName').value.trim(),
            age: parseInt(document.getElementById('editAge').value, 10),
            birthdate: document.getElementById('editBirthdate').value
        };

        const passwordValue = document.getElementById('editPassword').value;
        if (passwordValue) {
            payload.password = passwordValue;
        }

        return payload;
    }

    function markDirty() {
        isDirty = true;
    }

    function setupDirtyTracking(form) {
        form.querySelectorAll('input, select, textarea').forEach(function (field) {
            field.addEventListener('input', markDirty);
            field.addEventListener('change', markDirty);
        });

        window.addEventListener('beforeunload', function (event) {
            if (!isDirty || allowNavigation) {
                return;
            }

            event.preventDefault();
            event.returnValue = '';
        });

        document.querySelectorAll('.admin-nav-link, .navbar-brand, #cancelEditLink').forEach(function (link) {
            link.addEventListener('click', function (event) {
                if (!isDirty || allowNavigation) {
                    return;
                }

                if (!window.confirm('You have unsaved changes. Leave this page?')) {
                    event.preventDefault();
                } else {
                    allowNavigation = true;
                }
            });
        });
    }

    function populateForm(user) {
        document.getElementById('editUserId').value = user.userId ?? '';
        document.getElementById('editUsernameDisplay').value = user.username ?? '';
        document.getElementById('editEmail').value = user.email ?? '';
        document.getElementById('editRole').value = user.role ?? 'ROLE_ADMIN';
        document.getElementById('editLastName').value = user.lastName ?? '';
        document.getElementById('editFirstName').value = user.firstName ?? '';
        document.getElementById('editMiddleName').value = user.middleName ?? '';
        document.getElementById('editAge').value = user.age ?? '';
        document.getElementById('editBirthdate').value = user.birthdate ?? '';
    }

    function applySelfRoleLock(me, user) {
        const roleSelect = document.getElementById('editRole');
        const roleLockMessage = document.getElementById('editRoleLockMessage');

        if (me.username === user.username) {
            roleSelect.disabled = true;
            roleLockMessage.classList.remove('d-none');
        } else {
            roleSelect.disabled = false;
            roleLockMessage.classList.add('d-none');
        }
    }

    async function loadPageData(userId) {
        const [meResponse, userResponse] = await Promise.all([
            fetch('/api/auth/me', { credentials: 'same-origin' }),
            fetch('/api/admin/users/' + encodeURIComponent(userId), { credentials: 'same-origin' })
        ]);

        if (!meResponse.ok || !userResponse.ok) {
            throw new Error('Unable to load the selected user.');
        }

        const me = await meResponse.json();
        const user = await userResponse.json();

        populateForm(user);
        applySelfRoleLock(me, user);
    }

    async function saveUser(userId) {
        hideAlert('editUserError');
        hideAlert('editUserSuccess');

        const saveButton = document.getElementById('saveEditButton');
        saveButton.disabled = true;
        saveButton.textContent = 'Saving...';

        // Client-side username validation (no spaces)
        const usernameField = document.getElementById('editUsernameDisplay');
        if (/\s/.test(usernameField.value)) {
            setAlert('editUserError', 'Username must not contain spaces.', 'danger');
            saveButton.disabled = false;
            saveButton.textContent = 'Save Changes';
            return;
        }

        // Client-side password length check
        const passwordField = document.getElementById('editPassword');
        if (passwordField.value && passwordField.value.length < 8) {
            setAlert('editUserError', 'Password must be at least 8 characters.', 'danger');
            saveButton.disabled = false;
            saveButton.textContent = 'Save Changes';
            return;
        }

        try {
            const response = await fetch('/api/admin/users/' + encodeURIComponent(userId), {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'same-origin',
                body: JSON.stringify(buildPayload())
            });

            const data = await response.json();

            if (!response.ok) {
                const errorMessage = data.errors
                    ? Object.values(data.errors).join('. ')
                    : data.message || 'Unable to update the selected user.';
                throw new Error(errorMessage);
            }

            isDirty = false;
            allowNavigation = true;
            setAlert('editUserSuccess', 'User updated successfully. Redirecting to the dashboard...', 'success');

            // Clear the password field after successful save
            document.getElementById('editPassword').value = '';

            window.setTimeout(function () {
                window.location.replace('admin.html');
            }, 1200);
        } catch (error) {
            setAlert('editUserError', error.message || 'Unable to update the selected user.', 'danger');
            saveButton.disabled = false;
            saveButton.textContent = 'Save Changes';
        }
    }

    document.addEventListener('DOMContentLoaded', async function () {
        const form = document.getElementById('editUserForm');
        if (!form) {
            return;
        }

        const userId = readUserId();
        if (!userId) {
            setAlert('editUserError', 'No user was selected for editing.', 'danger');
            return;
        }

        setupDirtyTracking(form);

        try {
            await loadPageData(userId);
        } catch (error) {
            setAlert('editUserError', error.message || 'Unable to load the selected user.', 'danger');
        }

        form.addEventListener('submit', function (event) {
            event.preventDefault();
            saveUser(userId);
        });
    });
})();
