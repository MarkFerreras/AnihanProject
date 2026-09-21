/**
 * Password visibility toggle, for pages that don't include auth-guard.js
 * (which has its own copy of this for the authenticated dashboard pages).
 * Adds an eye-icon button next to every input[type="password"] on the page.
 */
(function () {
    'use strict';

    var EYE_ICON_SHOW = '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16">' +
        '<path d="M10.5 8a2.5 2.5 0 1 1-5 0 2.5 2.5 0 0 1 5 0z"/>' +
        '<path d="M0 8s3-5.5 8-5.5S16 8 16 8s-3 5.5-8 5.5S0 8 0 8zm8 3.5a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7z"/>' +
        '</svg>';

    var EYE_ICON_HIDE = '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16">' +
        '<path d="M13.359 11.238C15.06 9.72 16 8 16 8s-3-5.5-8-5.5a7 7 0 0 0-2.79.588l.77.771A6 6 0 0 1 8 3.5c2.12 0 3.879 1.168 5.168 2.457A13 13 0 0 1 14.828 8c-.058.087-.122.183-.195.288-.335.48-.83 1.12-1.465 1.755-.165.165-.337.328-.517.486z"/>' +
        '<path d="M11.297 9.176a3.5 3.5 0 0 0-4.474-4.474l.823.823a2.5 2.5 0 0 1 2.829 2.829zm-2.943 1.299l.822.822a3.5 3.5 0 0 1-4.474-4.474l.823.823a2.5 2.5 0 0 0 2.829 2.829z"/>' +
        '<path d="M3.35 5.47l-.77-.77A7 7 0 0 0 0 8s3 5.5 8 5.5c.966 0 1.889-.166 2.746-.45l-.727-.727A6 6 0 0 1 8 12.5C5.88 12.5 4.121 11.332 2.832 10.042A13 13 0 0 1 1.172 8c.058-.087.122-.183.195-.288.335-.48.83-1.12 1.465-1.755.165-.165.337-.328.517-.486z"/>' +
        '<path d="M14 1L1 14l.707.707L14.707 1.707z"/>' +
        '</svg>';

    function setupPasswordToggles() {
        document.querySelectorAll('input[type="password"]').forEach(function (input) {
            if (input.dataset.pwToggle === 'done') {
                return;
            }
            input.dataset.pwToggle = 'done';

            var wrapper = document.createElement('div');
            wrapper.className = 'input-group';
            input.parentElement.insertBefore(wrapper, input);
            wrapper.appendChild(input);

            var toggleBtn = document.createElement('button');
            toggleBtn.type = 'button';
            toggleBtn.className = 'btn btn-outline-secondary password-toggle-btn';
            toggleBtn.tabIndex = -1;
            toggleBtn.setAttribute('aria-label', 'Toggle password visibility');
            toggleBtn.innerHTML = EYE_ICON_SHOW;
            wrapper.appendChild(toggleBtn);

            toggleBtn.addEventListener('click', function () {
                var isHidden = input.type === 'password';
                input.type = isHidden ? 'text' : 'password';
                toggleBtn.innerHTML = isHidden ? EYE_ICON_HIDE : EYE_ICON_SHOW;
            });
        });
    }

    window.setupPasswordToggles = setupPasswordToggles;
})();
