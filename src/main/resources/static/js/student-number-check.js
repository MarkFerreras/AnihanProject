/*
 * SrmsStudentNumberCheck — while the registrar types in an Assign Student Number modal, warns
 * (and disables Save) if another student already holds that number. The PUT on Save remains
 * the authority; this only surfaces the clash earlier.
 *
 *   const check = SrmsStudentNumberCheck.attach({ input, saveBtn, getRecordId: () => id });
 *   check.reset();   // call when the modal opens
 */
(function () {
    'use strict';

    const DEBOUNCE_MS = 300;

    function attach(opts) {
        const input = opts.input;
        const saveBtn = opts.saveBtn;
        let timer = null;
        let seq = 0;

        const feedback = document.createElement('div');
        feedback.className = 'invalid-feedback';
        input.insertAdjacentElement('afterend', feedback);

        function reset() {
            window.clearTimeout(timer);
            seq++;
            input.classList.remove('is-invalid');
            feedback.textContent = '';
            saveBtn.disabled = false;
        }

        input.addEventListener('input', function () {
            reset();
            const value = input.value.trim();
            const recordId = opts.getRecordId();
            if (!value || !recordId) return;

            const mySeq = seq;
            timer = window.setTimeout(async function () {
                try {
                    const res = await fetch('/api/registrar/student-records/' + encodeURIComponent(recordId)
                            + '/student-number/availability?number=' + encodeURIComponent(value),
                        { credentials: 'same-origin' });
                    if (!res.ok || mySeq !== seq) return;
                    const body = await res.json();
                    if (mySeq !== seq || body.available) return;
                    input.classList.add('is-invalid');
                    feedback.textContent = 'Student number ' + value + ' is already assigned to '
                        + body.assignedTo + '.';
                    saveBtn.disabled = true;
                } catch (err) {
                    // Network hiccup: no inline warning; the server still rejects a clash on Save.
                }
            }, DEBOUNCE_MS);
        });

        return { reset: reset };
    }

    window.SrmsStudentNumberCheck = { attach: attach };
})();
