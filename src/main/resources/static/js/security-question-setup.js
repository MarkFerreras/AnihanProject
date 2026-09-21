(function () {
    'use strict';

    const ROLE_DASHBOARDS = {
        ROLE_ADMIN: '/admin.html',
        ROLE_REGISTRAR: '/registrar.html',
        ROLE_TRAINER: '/trainer.html'
    };

    async function guardPage() {
        try {
            const response = await fetch('/api/auth/me', { credentials: 'same-origin' });
            if (!response.ok) {
                window.location.replace('/index.html');
                return false;
            }
            const data = await response.json();
            if (data.role !== 'ROLE_PENDING_SETUP') {
                // Already finished setup (or never needed to) — nothing to do here.
                window.location.replace(ROLE_DASHBOARDS[data.role] || '/index.html');
                return false;
            }
            return true;
        } catch (error) {
            window.location.replace('/index.html');
            return false;
        }
    }

    async function loadDefaultQuestions() {
        try {
            const response = await fetch('/api/account/security-questions/default-questions', {
                credentials: 'same-origin'
            });
            return response.ok ? await response.json() : [];
        } catch (error) {
            return [];
        }
    }

    function renderSlotOptions(selectEl, questions) {
        selectEl.innerHTML = '<option value="">Select a question...</option>';
        questions.forEach(function (q) {
            const option = document.createElement('option');
            option.value = q.questionId;
            option.textContent = q.questionText;
            selectEl.appendChild(option);
        });
        const customOption = document.createElement('option');
        customOption.value = 'custom';
        customOption.textContent = 'Write your own question';
        selectEl.appendChild(customOption);
    }

    function wireSlotToggle(slotEl) {
        const select = slotEl.querySelector('.sq-question-select');
        const customInput = slotEl.querySelector('.sq-custom-question');
        select.addEventListener('change', function () {
            const isCustom = select.value === 'custom';
            customInput.classList.toggle('d-none', !isCustom);
            customInput.required = isCustom;
            if (!isCustom) {
                customInput.value = '';
            }
        });
    }

    function readSlot(slotEl) {
        const select = slotEl.querySelector('.sq-question-select');
        const customInput = slotEl.querySelector('.sq-custom-question');
        const answerInput = slotEl.querySelector('.sq-answer');
        const isCustom = select.value === 'custom';
        return {
            questionId: isCustom || !select.value ? null : parseInt(select.value, 10),
            customQuestion: isCustom ? customInput.value.trim() : null,
            answer: answerInput.value
        };
    }

    function showError(message) {
        const errorEl = document.getElementById('setupError');
        errorEl.textContent = message;
        errorEl.classList.remove('d-none');
    }

    function hideError() {
        const errorEl = document.getElementById('setupError');
        errorEl.classList.add('d-none');
        errorEl.textContent = '';
    }

    document.addEventListener('DOMContentLoaded', async function () {
        const canProceed = await guardPage();
        if (!canProceed) {
            return;
        }

        const questions = await loadDefaultQuestions();
        const slots = document.querySelectorAll('.sq-slot');
        slots.forEach(function (slot) {
            renderSlotOptions(slot.querySelector('.sq-question-select'), questions);
            wireSlotToggle(slot);
        });

        const form = document.getElementById('setupForm');
        const submitButton = document.getElementById('setupButton');

        form.addEventListener('submit', async function (event) {
            event.preventDefault();
            hideError();

            const slotData = Array.from(slots).map(readSlot);
            if (slotData.some(function (s) { return !s.questionId && !s.customQuestion; })) {
                showError('Please choose or write both questions.');
                return;
            }

            submitButton.disabled = true;
            submitButton.textContent = 'Saving...';

            try {
                const response = await fetch('/api/account/security-questions/setup', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'same-origin',
                    body: JSON.stringify({ slots: slotData })
                });
                const data = await response.json();

                if (response.ok) {
                    window.location.href = ROLE_DASHBOARDS[data.role] || '/index.html';
                } else {
                    const errorMessage = data.errors
                        ? Object.values(data.errors).join('. ')
                        : data.message || 'Failed to save security questions.';
                    showError(errorMessage);
                }
            } catch (error) {
                showError('Unable to connect to the server. Please try again.');
            } finally {
                submitButton.disabled = false;
                submitButton.textContent = 'Save and Continue';
            }
        });
    });
})();
