(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', function () {
        let questionTexts = null;
        try {
            questionTexts = JSON.parse(sessionStorage.getItem('sq_questionTexts') || 'null');
        } catch (error) {
            questionTexts = null;
        }

        if (!questionTexts || questionTexts.length !== 2) {
            // Reached directly without going through the email step — nothing to answer.
            window.location.replace('/forgot-password.html');
            return;
        }

        document.getElementById('questionLabel1').textContent = questionTexts[0];
        document.getElementById('questionLabel2').textContent = questionTexts[1];

        const form = document.getElementById('verifyForm');
        const errorEl = document.getElementById('verifyError');
        const button = document.getElementById('verifyButton');

        form.addEventListener('submit', async function (event) {
            event.preventDefault();
            errorEl.classList.add('d-none');
            errorEl.textContent = '';

            const answers = [
                document.getElementById('answer1').value,
                document.getElementById('answer2').value
            ];

            button.disabled = true;
            button.textContent = 'Checking...';

            try {
                const response = await fetch('/api/password-recovery/verify', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'same-origin',
                    body: JSON.stringify({ answers: answers })
                });
                const data = await response.json();

                if (response.ok) {
                    sessionStorage.removeItem('sq_questionTexts');
                    window.location.href = '/reset-password.html';
                } else {
                    errorEl.textContent = data.message || 'One or more answers were incorrect.';
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
