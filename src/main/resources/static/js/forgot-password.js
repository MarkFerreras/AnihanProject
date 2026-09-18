(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', function () {
        const form = document.getElementById('lookupForm');
        const errorEl = document.getElementById('lookupError');
        const button = document.getElementById('lookupButton');

        form.addEventListener('submit', async function (event) {
            event.preventDefault();
            errorEl.classList.add('d-none');
            errorEl.textContent = '';

            const email = document.getElementById('email').value.trim();

            button.disabled = true;
            button.textContent = 'Checking...';

            try {
                const response = await fetch('/api/password-recovery/lookup', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'same-origin',
                    body: JSON.stringify({ email: email })
                });
                const data = await response.json();

                if (response.ok) {
                    // Question text is not secret — passing it forward this way
                    // avoids a second round trip. The account this session is
                    // scoped to is tracked server-side, not by anything stored here.
                    sessionStorage.setItem('sq_questionTexts', JSON.stringify(data.questionTexts));
                    window.location.href = '/forgot-password-questions.html';
                } else {
                    errorEl.textContent = data.message || 'We couldn\'t find an account matching that information.';
                    errorEl.classList.remove('d-none');
                }
            } catch (error) {
                errorEl.textContent = 'Unable to connect to the server. Please try again.';
                errorEl.classList.remove('d-none');
            } finally {
                button.disabled = false;
                button.textContent = 'Continue';
            }
        });
    });
})();
