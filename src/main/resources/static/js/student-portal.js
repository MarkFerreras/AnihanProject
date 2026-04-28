/**
 * student-portal.js — Student Portal Welcome Page Logic
 *
 * Handles:
 * 1. Form submission with duplicate-name check via /api/student-portal/check-duplicate
 * 2. If duplicate found → shows warning alert, does NOT navigate
 * 3. If no duplicate → redirects to student-details.html with names as URL params
 */
(function () {
    'use strict';

    const form = document.getElementById('studentPortalForm');
    const submitBtn = document.getElementById('submitButton');
    const duplicateAlert = document.getElementById('duplicateAlert');
    const errorAlert = document.getElementById('portalError');

    form.addEventListener('submit', async function (e) {
        e.preventDefault();

        // Hide previous alerts
        duplicateAlert.classList.add('d-none');
        errorAlert.classList.add('d-none');
        errorAlert.textContent = '';

        const lastName = document.getElementById('lastName').value.trim();
        const firstName = document.getElementById('firstName').value.trim();
        const middleName = document.getElementById('middleName').value.trim();

        // Client-side validation
        if (!lastName || !firstName || !middleName) {
            errorAlert.textContent = 'Please fill in all name fields.';
            errorAlert.classList.remove('d-none');
            return;
        }

        // Disable button during request
        submitBtn.disabled = true;
        submitBtn.textContent = 'Checking...';

        try {
            const params = new URLSearchParams({
                lastName: lastName,
                firstName: firstName,
                middleName: middleName
            });

            const response = await fetch('/api/student-portal/check-duplicate?' + params.toString(), {
                method: 'GET',
                headers: { 'Accept': 'application/json' }
            });

            if (!response.ok) {
                throw new Error('Server error. Please try again.');
            }

            const data = await response.json();

            if (data.exists) {
                // Show duplicate warning — do NOT navigate
                duplicateAlert.classList.remove('d-none');
            } else {
                // No duplicate — pass names to the next page via URL params
                const nextParams = new URLSearchParams({
                    lastName: lastName,
                    firstName: firstName,
                    middleName: middleName
                });
                window.location.href = '/student-details.html?' + nextParams.toString();
            }
        } catch (error) {
            errorAlert.textContent = error.message || 'Unable to connect to the server. Please try again later.';
            errorAlert.classList.remove('d-none');
        } finally {
            submitBtn.disabled = false;
            submitBtn.textContent = 'Continue';
        }
    });
})();
