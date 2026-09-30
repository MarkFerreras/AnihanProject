/*
 * SrmsCombobox — type-to-filter dropdown that replaces the browser's native <datalist> popup.
 *
 *   const cb = SrmsCombobox.attach(inputEl, {
 *       items: [{ value: 'B2026A', label: 'B2026A', hint: '2026' }],  // optional initial items
 *       emptyText: 'No matches',                                        // shown when nothing matches
 *       onSelect: function (item) {}                                    // optional, after a pick
 *   });
 *   cb.setItems(newItems);
 *
 * Free typing stays allowed (the input's value is what gets submitted). Picking an option sets
 * input.value = item.value and fires a bubbling 'input' event so existing listeners still run.
 * Keyboard: ArrowUp/ArrowDown move, Enter picks, Escape closes (without closing a Bootstrap modal).
 */
(function () {
    'use strict';

    const MAX_RESULTS = 50;
    let idCounter = 0;

    function attach(input, options) {
        const opts = Object.assign({ items: [], emptyText: 'No matches', onSelect: null }, options || {});
        let items = opts.items.slice();
        let visible = [];
        let activeIndex = -1;
        let selecting = false;

        input.removeAttribute('list');
        input.setAttribute('autocomplete', 'off');
        input.setAttribute('role', 'combobox');
        input.setAttribute('aria-autocomplete', 'list');
        input.setAttribute('aria-expanded', 'false');

        const wrapper = document.createElement('div');
        wrapper.className = 'srms-combobox';
        input.parentNode.insertBefore(wrapper, input);
        wrapper.appendChild(input);

        const menu = document.createElement('ul');
        menu.id = 'srmsCombobox' + (++idCounter);
        menu.className = 'dropdown-menu srms-combobox-menu';
        menu.setAttribute('role', 'listbox');
        wrapper.appendChild(menu);
        input.setAttribute('aria-controls', menu.id);

        function matches(item, query) {
            if (!query) return true;
            const haystack = (item.value + ' ' + (item.label || '') + ' ' + (item.hint || '')).toLowerCase();
            return haystack.indexOf(query) !== -1;
        }

        function render() {
            const query = input.value.trim().toLowerCase();
            visible = items.filter(function (item) { return matches(item, query); }).slice(0, MAX_RESULTS);
            menu.innerHTML = '';

            if (visible.length === 0) {
                const empty = document.createElement('li');
                empty.className = 'dropdown-item-text text-muted small';
                empty.textContent = opts.emptyText;
                menu.appendChild(empty);
            }

            visible.forEach(function (item, i) {
                const li = document.createElement('li');
                li.id = menu.id + '-opt' + i;
                li.className = 'dropdown-item srms-combobox-option' + (i === activeIndex ? ' active' : '');
                li.setAttribute('role', 'option');
                li.setAttribute('aria-selected', i === activeIndex ? 'true' : 'false');

                const main = document.createElement('span');
                main.textContent = item.label || item.value;
                li.appendChild(main);
                if (item.hint) {
                    const hint = document.createElement('span');
                    hint.className = 'srms-combobox-hint';
                    hint.textContent = item.hint;
                    li.appendChild(hint);
                }

                // mousedown + preventDefault keeps focus in the input, so blur doesn't close first.
                li.addEventListener('mousedown', function (e) {
                    e.preventDefault();
                    choose(i);
                });
                menu.appendChild(li);
            });

            if (activeIndex >= 0) {
                input.setAttribute('aria-activedescendant', menu.id + '-opt' + activeIndex);
                const activeEl = document.getElementById(menu.id + '-opt' + activeIndex);
                if (activeEl) activeEl.scrollIntoView({ block: 'nearest' });
            } else {
                input.removeAttribute('aria-activedescendant');
            }
        }

        function isOpen() {
            return menu.classList.contains('show');
        }

        function open() {
            if (input.readOnly || input.disabled) return;
            render();
            menu.classList.add('show');
            input.setAttribute('aria-expanded', 'true');
        }

        function close() {
            menu.classList.remove('show');
            input.setAttribute('aria-expanded', 'false');
            input.removeAttribute('aria-activedescendant');
            activeIndex = -1;
        }

        function choose(i) {
            const item = visible[i];
            if (!item) return;
            input.value = item.value;
            close();
            selecting = true;
            input.dispatchEvent(new Event('input', { bubbles: true }));
            selecting = false;
            if (opts.onSelect) opts.onSelect(item);
        }

        input.addEventListener('focus', open);
        input.addEventListener('click', function () { if (!isOpen()) open(); });
        input.addEventListener('blur', close);
        input.addEventListener('input', function () {
            if (selecting) return;
            activeIndex = -1;
            open();
        });

        input.addEventListener('keydown', function (e) {
            if (e.key === 'ArrowDown') {
                e.preventDefault();
                if (!isOpen()) open();
                activeIndex = Math.min(activeIndex + 1, visible.length - 1);
                render();
            } else if (e.key === 'ArrowUp') {
                e.preventDefault();
                if (isOpen() && visible.length > 0) {
                    activeIndex = Math.max(activeIndex - 1, 0);
                    render();
                }
            } else if (e.key === 'Enter') {
                if (isOpen() && activeIndex >= 0) {
                    e.preventDefault();
                    choose(activeIndex);
                }
            } else if (e.key === 'Escape') {
                if (isOpen()) {
                    e.preventDefault();
                    e.stopPropagation(); // don't let Bootstrap close the surrounding modal
                    close();
                }
            } else if (e.key === 'Tab') {
                close();
            }
        });

        return {
            setItems: function (newItems) {
                items = (newItems || []).slice();
                if (isOpen()) render();
            },
            close: close
        };
    }

    window.SrmsCombobox = { attach: attach };
})();
