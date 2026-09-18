(function () {
    'use strict';

    const ROLE_DASHBOARDS = {
        ROLE_ADMIN: '/admin.html',
        ROLE_REGISTRAR: '/registrar.html',
        ROLE_TRAINER: '/trainer.html'
    };

    function isStrongPassword(password) {
        return /[a-z]/.test(password) &&
               /[A-Z]/.test(password) &&
               /\d/.test(password) &&
               /[^a-zA-Z0-9]/.test(password);
    }

    document.addEventListener('DOMContentLoaded', function () {
        const form = document.getElementById('resetForm');
        const errorEl = document.getElementById('resetError');
        const button = document.getElementById('resetButton');

        function showError(message) {
            errorEl.textContent = message;
            errorEl.classList.remove('d-none');
        }

        form.addEventListener('submit', async function (event) {
            event.preventDefault();
            errorEl.classList.add('d-none');
            errorEl.textContent = '';

            const newPassword = document.getElementById('newPassword').value;
            const confirmNewPassword = document.getElementById('confirmNewPassword').value;

            if (newPassword !== confirmNewPassword) {
                showError('New passwords do not match.');
                return;
            }
            if (!isStrongPassword(newPassword)) {
                showError('Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character.');
                return;
            }

            button.disabled = true;
            button.textContent = 'Saving...';

            try {
                const response = await fetch('/api/password-recovery/reset', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'same-origin',
                    body: JSON.stringify({ newPassword: newPassword, confirmNewPassword: confirmNewPassword })
                });

                if (response.status === 401 || response.status === 403) {
                    // The temporary reset session expired or was skipped — there's
                    // nothing left to do on this page. Send them back to the start.
                    window.location.replace('/forgot-password.html');
                    return;
                }

                const data = await response.json();

                if (response.ok) {
                    window.location.href = ROLE_DASHBOARDS[data.role] || '/index.html';
                } else {
                    const errorMessage = data.errors
                        ? Object.values(data.errors).join('. ')
                        : data.message || 'Failed to reset password.';
                    showError(errorMessage);
                }
            } catch (error) {
                showError('Unable to connect to the server. Please try again.');
            } finally {
                button.disabled = false;
                button.textContent = 'Reset Password';
            }
        });
    });
})();
