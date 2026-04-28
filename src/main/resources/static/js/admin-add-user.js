(function () {
    'use strict';

    function setAlert(id, message, type) {
        var element = document.getElementById(id);
        if (!element) {
            return;
        }

        element.className = 'alert alert-' + type;
        element.textContent = message;
        element.classList.remove('d-none');
    }

    function hideAlert(id) {
        var element = document.getElementById(id);
        if (!element) {
            return;
        }

        element.classList.add('d-none');
        element.textContent = '';
    }

    function buildPayload() {
        return {
            username: document.getElementById('addUsername').value.trim(),
            password: document.getElementById('addPassword').value,
            role: document.getElementById('addRole').value,
            lastName: document.getElementById('addLastName').value.trim() || null,
            firstName: document.getElementById('addFirstName').value.trim() || null,
            middleName: document.getElementById('addMiddleName').value.trim() || null,
            email: document.getElementById('addEmail').value.trim() || null,
            birthdate: document.getElementById('addBirthdate').value || null
        };
    }

    document.addEventListener('DOMContentLoaded', function () {
        var form = document.getElementById('addUserForm');
        if (!form) {
            return;
        }

        form.addEventListener('submit', async function (event) {
            event.preventDefault();
            hideAlert('addUserError');
            hideAlert('addUserSuccess');

            var usernameInput = document.getElementById('addUsername');
            var passwordInput = document.getElementById('addPassword');

            // Client-side validation: username no spaces
            if (/\s/.test(usernameInput.value)) {
                setAlert('addUserError', 'Username must not contain spaces.', 'danger');
                return;
            }

            // Client-side validation: password min 8 chars
            if (passwordInput.value.length < 8) {
                setAlert('addUserError', 'Password must be at least 8 characters.', 'danger');
                return;
            }

            // Client-side validation: birthdate is required
            var birthdateInput = document.getElementById('addBirthdate');
            if (!birthdateInput.value) {
                setAlert('addUserError', 'Birthdate is required. Age is calculated automatically.', 'danger');
                return;
            }

            var submitButton = document.getElementById('createAccountButton');
            submitButton.disabled = true;
            submitButton.textContent = 'Creating...';

            try {
                var response = await fetch('/api/admin/users', {
                    method: 'POST',
                    credentials: 'same-origin',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(buildPayload())
                });

                if (!response.ok) {
                    var errorData = await response.json().catch(function () { return {}; });

                    // Build error message from validation errors or generic message
                    var message = errorData.message || 'Failed to create account.';
                    if (errorData.errors) {
                        var fieldErrors = Object.values(errorData.errors).join('. ');
                        message = fieldErrors || message;
                    }

                    setAlert('addUserError', message, 'danger');
                    return;
                }

                // Success — redirect to admin dashboard with success flag
                window.location.href = '/admin?created=true';
            } catch (error) {
                setAlert('addUserError', 'An unexpected error occurred. Please try again.', 'danger');
            } finally {
                submitButton.disabled = false;
                submitButton.textContent = 'Create Account';
            }
        });
    });
})();
