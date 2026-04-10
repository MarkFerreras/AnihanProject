/**
 * auth-guard.js — Shared Authentication Guard & Account UI
 * 
 * Include this script on every authenticated page (admin.html, registrar.html, trainer.html).
 * The page must have:
 *   - data-required-role attribute on <body> (e.g., "ROLE_ADMIN")
 *   - #accountInitial, #accountDropdownName, #accountDropdownRole
 *   - #logoutBtn, #editAccountBtn, #editAccountModal
 *   - Tab 1 (Personal Details): #personalDetailsForm, #fullName, #age, #dateOfBirth
 *   - Trainer-only: #trainerFields, #subjectCode, #sectionCode
 *   - Tab 2 (Account Settings): username + password change forms
 */

(function () {
    'use strict';

    const ROLE_DASHBOARDS = {
        'ROLE_ADMIN': '/admin.html',
        'ROLE_REGISTRAR': '/registrar.html',
        'ROLE_TRAINER': '/trainer.html'
    };

    const ROLE_DISPLAY = {
        'ROLE_ADMIN': 'Admin',
        'ROLE_REGISTRAR': 'Registrar',
        'ROLE_TRAINER': 'Trainer'
    };

    // Store current user data globally within the IIFE
    let currentUserData = null;

    // ──────────────────────────────────────────────
    // SESSION VERIFICATION
    // ──────────────────────────────────────────────

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
                const correctDashboard = ROLE_DASHBOARDS[data.role] || '/index.html';
                window.location.replace(correctDashboard);
                return;
            }

            populateAccountUI(data.username, data.role);
            populatePersonalDetailsForm(data);
            handleTrainerFields(data.role);

        } catch (error) {
            window.location.replace('/index.html');
        }
    }

    // ──────────────────────────────────────────────
    // UI POPULATION
    // ──────────────────────────────────────────────

    function populateAccountUI(username, role) {
        const initialEl = document.getElementById('accountInitial');
        const nameEl = document.getElementById('accountDropdownName');
        const roleEl = document.getElementById('accountDropdownRole');
        const newUsernameEl = document.getElementById('newUsername');

        if (initialEl) initialEl.textContent = username.charAt(0).toUpperCase();
        if (nameEl) nameEl.textContent = username;
        if (roleEl) roleEl.textContent = ROLE_DISPLAY[role] || role;
        if (newUsernameEl) newUsernameEl.value = username;
    }

    function populatePersonalDetailsForm(data) {
        const lastNameEl = document.getElementById('lastName');
        const firstNameEl = document.getElementById('firstName');
        const middleNameEl = document.getElementById('middleName');
        const ageEl = document.getElementById('age');
        const dobEl = document.getElementById('birthdate');

        if (lastNameEl) lastNameEl.value = data.lastName || '';
        if (firstNameEl) firstNameEl.value = data.firstName || '';
        if (middleNameEl) middleNameEl.value = data.middleName || '';
        if (ageEl) ageEl.value = data.age || '';
        if (dobEl) dobEl.value = data.birthdate || '';
    }

    /**
     * Shows trainer-only fields and loads dropdowns if the user is a trainer.
     */
    async function handleTrainerFields(role) {
        const trainerFields = document.getElementById('trainerFields');
        if (!trainerFields) return;

        if (role !== 'ROLE_TRAINER') {
            trainerFields.style.display = 'none';
            return;
        }

        trainerFields.style.display = 'block';

        // Load dropdown data
        await Promise.all([loadSubjects(), loadSections()]);

        // Note: Trainer subject assignments are managed in classess table, not user table.
        // Pre-selection of current class is removed for now until class manager is implemented.
    }

    async function loadSubjects() {
        const subjectEl = document.getElementById('subjectCode');
        if (!subjectEl) return;

        try {
            const response = await fetch('/api/lookup/subjects', { credentials: 'same-origin' });
            if (!response.ok) return;
            const subjects = await response.json();

            // Clear existing options except the placeholder
            subjectEl.innerHTML = '<option value="">— Select Subject —</option>';
            subjects.forEach(function (s) {
                const opt = document.createElement('option');
                opt.value = s.code;
                opt.textContent = s.code + ' — ' + s.name;
                subjectEl.appendChild(opt);
            });
        } catch (error) {
            // Silently fail — dropdown will be empty
        }
    }

    async function loadSections() {
        const sectionEl = document.getElementById('sectionCode');
        if (!sectionEl) return;

        try {
            const response = await fetch('/api/lookup/sections', { credentials: 'same-origin' });
            if (!response.ok) return;
            const sections = await response.json();

            sectionEl.innerHTML = '<option value="">— Select Section —</option>';
            sections.forEach(function (s) {
                const opt = document.createElement('option');
                opt.value = s.code;
                opt.textContent = s.name;
                sectionEl.appendChild(opt);
            });
        } catch (error) {
            // Silently fail
        }
    }

    // ──────────────────────────────────────────────
    // LOGOUT
    // ──────────────────────────────────────────────

    function setupLogout() {
        const logoutBtn = document.getElementById('logoutBtn');
        if (!logoutBtn) return;

        logoutBtn.addEventListener('click', async function (e) {
            e.preventDefault();

            try {
                await fetch('/api/auth/logout', {
                    method: 'POST',
                    credentials: 'same-origin'
                });
            } catch (error) {
                // Even if request fails, redirect
            }

            // Redirect to login with logout flag for notification
            window.location.replace('/index.html?loggedOut=true');
        });
    }

    // ──────────────────────────────────────────────
    // ALERTS
    // ──────────────────────────────────────────────

    function showAlert(containerId, message, type) {
        const container = document.getElementById(containerId);
        if (!container) return;
        container.className = 'alert alert-' + type;
        container.textContent = message;
        container.classList.remove('d-none');
    }

    function hideAlert(containerId) {
        const container = document.getElementById(containerId);
        if (!container) return;
        container.classList.add('d-none');
        container.textContent = '';
    }

    // ──────────────────────────────────────────────
    // PERSONAL DETAILS FORM
    // ──────────────────────────────────────────────

    function setupPersonalDetailsForm() {
        const form = document.getElementById('personalDetailsForm');
        if (!form) return;

        form.addEventListener('submit', async function (e) {
            e.preventDefault();
            hideAlert('personalDetailsAlert');

            const lastNameVal = document.getElementById('lastName').value.trim();
            const firstNameVal = document.getElementById('firstName').value.trim();
            const middleNameVal = document.getElementById('middleName').value.trim();
            const ageVal = document.getElementById('age').value;
            const birthdateVal = document.getElementById('birthdate').value;
            const submitBtn = form.querySelector('button[type="submit"]');

            const payload = {
                lastName: lastNameVal || null,
                firstName: firstNameVal || null,
                middleName: middleNameVal || null,
                age: ageVal ? parseInt(ageVal, 10) : null,
                birthdate: birthdateVal || null
            };

            submitBtn.disabled = true;
            submitBtn.textContent = 'Saving...';

            try {
                const response = await fetch('/api/account/details', {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'same-origin',
                    body: JSON.stringify(payload)
                });

                const data = await response.json();

                if (response.ok) {
                    showAlert('personalDetailsAlert', 'Personal details updated successfully!', 'success');
                    // Update stored data
                    currentUserData = { ...currentUserData, ...data };
                } else {
                    const errorMsg = data.errors
                        ? Object.values(data.errors).join('. ')
                        : data.message || 'Failed to update details.';
                    showAlert('personalDetailsAlert', errorMsg, 'danger');
                }
            } catch (error) {
                showAlert('personalDetailsAlert', 'Unable to connect to the server.', 'danger');
            } finally {
                submitBtn.disabled = false;
                submitBtn.textContent = 'Save Details';
            }
        });
    }

    // ──────────────────────────────────────────────
    // USERNAME CHANGE
    // ──────────────────────────────────────────────

    function setupUsernameChange() {
        const form = document.getElementById('usernameChangeForm');
        if (!form) return;

        form.addEventListener('submit', async function (e) {
            e.preventDefault();
            hideAlert('usernameChangeAlert');

            const username = document.getElementById('newUsername').value.trim();
            const currentPassword = document.getElementById('usernameCurrentPassword').value;
            const submitBtn = form.querySelector('button[type="submit"]');

            if (!username || !currentPassword) {
                showAlert('usernameChangeAlert', 'Please fill in all fields.', 'danger');
                return;
            }

            submitBtn.disabled = true;
            submitBtn.textContent = 'Saving...';

            try {
                const response = await fetch('/api/account/profile', {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'same-origin',
                    body: JSON.stringify({ username, currentPassword })
                });

                const data = await response.json();

                if (response.ok) {
                    showAlert('usernameChangeAlert', 'Username updated successfully!', 'success');
                    populateAccountUI(data.username, document.body.getAttribute('data-required-role'));
                    document.getElementById('usernameCurrentPassword').value = '';
                } else {
                    const errorMsg = data.errors
                        ? Object.values(data.errors).join('. ')
                        : data.message || 'Failed to update username.';
                    showAlert('usernameChangeAlert', errorMsg, 'danger');
                }
            } catch (error) {
                showAlert('usernameChangeAlert', 'Unable to connect to the server.', 'danger');
            } finally {
                submitBtn.disabled = false;
                submitBtn.textContent = 'Save Username';
            }
        });
    }

    // ──────────────────────────────────────────────
    // PASSWORD CHANGE
    // ──────────────────────────────────────────────

    function setupPasswordChange() {
        const form = document.getElementById('passwordChangeForm');
        if (!form) return;

        form.addEventListener('submit', async function (e) {
            e.preventDefault();
            hideAlert('passwordChangeAlert');

            const currentPassword = document.getElementById('currentPassword').value;
            const newPassword = document.getElementById('changeNewPassword').value;
            const confirmNewPassword = document.getElementById('confirmNewPassword').value;
            const submitBtn = form.querySelector('button[type="submit"]');

            if (!currentPassword || !newPassword || !confirmNewPassword) {
                showAlert('passwordChangeAlert', 'Please fill in all fields.', 'danger');
                return;
            }

            if (newPassword !== confirmNewPassword) {
                showAlert('passwordChangeAlert', 'New passwords do not match.', 'danger');
                return;
            }

            submitBtn.disabled = true;
            submitBtn.textContent = 'Saving...';

            try {
                const response = await fetch('/api/account/password', {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'same-origin',
                    body: JSON.stringify({ currentPassword, newPassword, confirmNewPassword })
                });

                const data = await response.json();

                if (response.ok) {
                    showAlert('passwordChangeAlert', 'Password updated! Redirecting to login...', 'success');
                    setTimeout(function () {
                        window.location.replace('/index.html');
                    }, 1500);
                } else {
                    const errorMsg = data.errors
                        ? Object.values(data.errors).join('. ')
                        : data.message || 'Failed to update password.';
                    showAlert('passwordChangeAlert', errorMsg, 'danger');
                }
            } catch (error) {
                showAlert('passwordChangeAlert', 'Unable to connect to the server.', 'danger');
            } finally {
                submitBtn.disabled = false;
                submitBtn.textContent = 'Save Password';
            }
        });
    }

    // ──────────────────────────────────────────────
    // MODAL RESET
    // ──────────────────────────────────────────────

    function setupModalReset() {
        const modal = document.getElementById('editAccountModal');
        if (!modal) return;

        modal.addEventListener('hidden.bs.modal', function () {
            hideAlert('personalDetailsAlert');
            hideAlert('usernameChangeAlert');
            hideAlert('passwordChangeAlert');
            // Clear password fields
            const pwFields = modal.querySelectorAll('input[type="password"]');
            pwFields.forEach(function (field) { field.value = ''; });
            // Re-populate personal details from stored data
            if (currentUserData) {
                populatePersonalDetailsForm(currentUserData);
            }
        });
    }

    // ──────────────────────────────────────────────
    // INIT
    // ──────────────────────────────────────────────

    document.addEventListener('DOMContentLoaded', function () {
        verifySession();
        setupLogout();
        setupPersonalDetailsForm();
        setupUsernameChange();
        setupPasswordChange();
        setupModalReset();
    });
})();
