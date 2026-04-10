/**
 * Shared authentication guard and account modal behavior.
 *
 * Expected page elements:
 * - <body data-required-role="ROLE_*">
 * - account dropdown nodes:
 *   #accountInitial, #accountDropdownName, #accountDropdownRole, #logoutBtn
 * - optional account modal nodes:
 *   #editAccountModal, #personalDetailsForm, #usernameChangeForm, #passwordChangeForm
 * - optional personal detail inputs:
 *   #lastName, #firstName, #middleName, #age, #birthdate
 * - optional trainer fields:
 *   #trainerFields, #subjectCode, #sectionCode
 */

(function () {
    'use strict';

    const ROLE_DASHBOARDS = {
        ROLE_ADMIN: '/admin.html',
        ROLE_REGISTRAR: '/registrar.html',
        ROLE_TRAINER: '/trainer.html'
    };

    const ROLE_DISPLAY = {
        ROLE_ADMIN: 'Admin',
        ROLE_REGISTRAR: 'Registrar',
        ROLE_TRAINER: 'Trainer'
    };

    let currentUserData = null;

    async function verifySession() {
        const requiredRole = document.body.getAttribute('data-required-role');

        try {
            const response = await fetch('/api/auth/me', { credentials: 'same-origin' });

            if (!response.ok) {
                window.location.replace('/index.html');
                return;
            }

            const data = await response.json();
            currentUserData = data;

            if (requiredRole && data.role !== requiredRole) {
                window.location.replace(ROLE_DASHBOARDS[data.role] || '/index.html');
                return;
            }

            populateAccountUI(data.username, data.role);
            populatePersonalDetailsForm(data);
            handleTrainerFields(data.role);
        } catch (error) {
            window.location.replace('/index.html');
        }
    }

    function populateAccountUI(username, role) {
        const initialEl = document.getElementById('accountInitial');
        const nameEl = document.getElementById('accountDropdownName');
        const roleEl = document.getElementById('accountDropdownRole');
        const newUsernameEl = document.getElementById('newUsername');

        if (initialEl) {
            initialEl.textContent = username ? username.charAt(0).toUpperCase() : '?';
        }

        if (nameEl) {
            nameEl.textContent = username || 'Unknown user';
        }

        if (roleEl) {
            roleEl.textContent = ROLE_DISPLAY[role] || role || '-';
        }

        if (newUsernameEl) {
            newUsernameEl.value = username || '';
        }
    }

    function populatePersonalDetailsForm(data) {
        const lastNameEl = document.getElementById('lastName');
        const firstNameEl = document.getElementById('firstName');
        const middleNameEl = document.getElementById('middleName');
        const ageEl = document.getElementById('age');
        const birthdateEl = document.getElementById('birthdate');

        if (lastNameEl) {
            lastNameEl.value = data.lastName || '';
        }

        if (firstNameEl) {
            firstNameEl.value = data.firstName || '';
        }

        if (middleNameEl) {
            middleNameEl.value = data.middleName || '';
        }

        if (ageEl) {
            ageEl.value = data.age || '';
        }

        if (birthdateEl) {
            birthdateEl.value = data.birthdate || '';
        }
    }

    async function handleTrainerFields(role) {
        const trainerFields = document.getElementById('trainerFields');
        if (!trainerFields) {
            return;
        }

        if (role !== 'ROLE_TRAINER') {
            trainerFields.style.display = 'none';
            return;
        }

        trainerFields.style.display = 'block';
        await Promise.all([loadSubjects(), loadSections()]);
    }

    async function loadSubjects() {
        const subjectEl = document.getElementById('subjectCode');
        if (!subjectEl) {
            return;
        }

        try {
            const response = await fetch('/api/lookup/subjects', { credentials: 'same-origin' });
            if (!response.ok) {
                return;
            }

            const subjects = await response.json();
            subjectEl.innerHTML = '<option value="">Select Subject</option>';

            subjects.forEach(function (subject) {
                const option = document.createElement('option');
                option.value = subject.code;
                option.textContent = subject.code + ' - ' + subject.name;
                subjectEl.appendChild(option);
            });
        } catch (error) {
            // Leave the dropdown empty if lookups are unavailable.
        }
    }

    async function loadSections() {
        const sectionEl = document.getElementById('sectionCode');
        if (!sectionEl) {
            return;
        }

        try {
            const response = await fetch('/api/lookup/sections', { credentials: 'same-origin' });
            if (!response.ok) {
                return;
            }

            const sections = await response.json();
            sectionEl.innerHTML = '<option value="">Select Section</option>';

            sections.forEach(function (section) {
                const option = document.createElement('option');
                option.value = section.code;
                option.textContent = section.name;
                sectionEl.appendChild(option);
            });
        } catch (error) {
            // Leave the dropdown empty if lookups are unavailable.
        }
    }

    function setupLogout() {
        const logoutBtn = document.getElementById('logoutBtn');
        if (!logoutBtn) {
            return;
        }

        logoutBtn.addEventListener('click', async function (event) {
            event.preventDefault();

            try {
                await fetch('/api/auth/logout', {
                    method: 'POST',
                    credentials: 'same-origin'
                });
            } catch (error) {
                // Redirect even if logout transport fails.
            }

            window.location.replace('/index.html?loggedOut=true');
        });
    }

    function showAlert(containerId, message, type) {
        const container = document.getElementById(containerId);
        if (!container) {
            return;
        }

        container.className = 'alert alert-' + type;
        container.textContent = message;
        container.classList.remove('d-none');
    }

    function hideAlert(containerId) {
        const container = document.getElementById(containerId);
        if (!container) {
            return;
        }

        container.classList.add('d-none');
        container.textContent = '';
    }

    function setupPersonalDetailsForm() {
        const form = document.getElementById('personalDetailsForm');
        if (!form) {
            return;
        }

        form.addEventListener('submit', async function (event) {
            event.preventDefault();
            hideAlert('personalDetailsAlert');

            const submitButton = form.querySelector('button[type="submit"]');
            const payload = {
                lastName: document.getElementById('lastName')?.value.trim() || null,
                firstName: document.getElementById('firstName')?.value.trim() || null,
                middleName: document.getElementById('middleName')?.value.trim() || null,
                age: document.getElementById('age')?.value
                    ? parseInt(document.getElementById('age').value, 10)
                    : null,
                birthdate: document.getElementById('birthdate')?.value || null
            };

            submitButton.disabled = true;
            submitButton.textContent = 'Saving...';

            try {
                const response = await fetch('/api/account/details', {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'same-origin',
                    body: JSON.stringify(payload)
                });

                const data = await response.json();

                if (response.ok) {
                    showAlert('personalDetailsAlert', 'Personal details updated successfully.', 'success');
                    currentUserData = {
                        ...currentUserData,
                        lastName: data.lastName,
                        firstName: data.firstName,
                        middleName: data.middleName,
                        age: data.age,
                        birthdate: data.birthdate
                    };
                } else {
                    const errorMessage = data.errors
                        ? Object.values(data.errors).join('. ')
                        : data.message || 'Failed to update details.';
                    showAlert('personalDetailsAlert', errorMessage, 'danger');
                }
            } catch (error) {
                showAlert('personalDetailsAlert', 'Unable to connect to the server.', 'danger');
            } finally {
                submitButton.disabled = false;
                submitButton.textContent = 'Save Details';
            }
        });
    }

    function setupUsernameChange() {
        const form = document.getElementById('usernameChangeForm');
        if (!form) {
            return;
        }

        form.addEventListener('submit', async function (event) {
            event.preventDefault();
            hideAlert('usernameChangeAlert');

            const username = document.getElementById('newUsername').value.trim();
            const currentPassword = document.getElementById('usernameCurrentPassword').value;
            const submitButton = form.querySelector('button[type="submit"]');

            if (!username || !currentPassword) {
                showAlert('usernameChangeAlert', 'Please fill in all fields.', 'danger');
                return;
            }

            submitButton.disabled = true;
            submitButton.textContent = 'Saving...';

            try {
                const response = await fetch('/api/account/profile', {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'same-origin',
                    body: JSON.stringify({ username, currentPassword })
                });

                const data = await response.json();

                if (response.ok) {
                    currentUserData = { ...currentUserData, username: data.username };
                    populateAccountUI(data.username, document.body.getAttribute('data-required-role'));
                    document.getElementById('usernameCurrentPassword').value = '';
                    showAlert('usernameChangeAlert', 'Username updated successfully.', 'success');
                } else {
                    const errorMessage = data.errors
                        ? Object.values(data.errors).join('. ')
                        : data.message || 'Failed to update username.';
                    showAlert('usernameChangeAlert', errorMessage, 'danger');
                }
            } catch (error) {
                showAlert('usernameChangeAlert', 'Unable to connect to the server.', 'danger');
            } finally {
                submitButton.disabled = false;
                submitButton.textContent = 'Save Username';
            }
        });
    }

    function setupPasswordChange() {
        const form = document.getElementById('passwordChangeForm');
        if (!form) {
            return;
        }

        form.addEventListener('submit', async function (event) {
            event.preventDefault();
            hideAlert('passwordChangeAlert');

            const currentPassword = document.getElementById('currentPassword').value;
            const newPassword = document.getElementById('changeNewPassword').value;
            const confirmNewPassword = document.getElementById('confirmNewPassword').value;
            const submitButton = form.querySelector('button[type="submit"]');

            if (!currentPassword || !newPassword || !confirmNewPassword) {
                showAlert('passwordChangeAlert', 'Please fill in all fields.', 'danger');
                return;
            }

            if (newPassword !== confirmNewPassword) {
                showAlert('passwordChangeAlert', 'New passwords do not match.', 'danger');
                return;
            }

            submitButton.disabled = true;
            submitButton.textContent = 'Saving...';

            try {
                const response = await fetch('/api/account/password', {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'same-origin',
                    body: JSON.stringify({ currentPassword, newPassword, confirmNewPassword })
                });

                const data = await response.json();

                if (response.ok) {
                    showAlert('passwordChangeAlert', 'Password updated. Redirecting to login...', 'success');
                    setTimeout(function () {
                        window.location.replace('/index.html');
                    }, 1500);
                } else {
                    const errorMessage = data.errors
                        ? Object.values(data.errors).join('. ')
                        : data.message || 'Failed to update password.';
                    showAlert('passwordChangeAlert', errorMessage, 'danger');
                }
            } catch (error) {
                showAlert('passwordChangeAlert', 'Unable to connect to the server.', 'danger');
            } finally {
                submitButton.disabled = false;
                submitButton.textContent = 'Save Password';
            }
        });
    }

    function setupModalReset() {
        const modal = document.getElementById('editAccountModal');
        if (!modal) {
            return;
        }

        modal.addEventListener('hidden.bs.modal', function () {
            hideAlert('personalDetailsAlert');
            hideAlert('usernameChangeAlert');
            hideAlert('passwordChangeAlert');

            modal.querySelectorAll('input[type="password"]').forEach(function (field) {
                field.value = '';
            });

            if (currentUserData) {
                populatePersonalDetailsForm(currentUserData);
                populateAccountUI(currentUserData.username, currentUserData.role);
            }
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        verifySession();
        setupLogout();
        setupPersonalDetailsForm();
        setupUsernameChange();
        setupPasswordChange();
        setupModalReset();
    });
})();
