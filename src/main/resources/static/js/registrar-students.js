(function () {
    'use strict';

    let detailsModal = null;
    let deleteConfirmModal = null;
    let assignNumberModal = null;
    let numberCheck = null;
    let assignBatchModal = null;
    let editStatusModal = null;
    let currentRecordId = null;
    let currentRecordIdentifier = null;
    let currentRecordStatus = null;
    let assignTargetRecordId = null;
    let assignBatchTargetRecordId = null;
    let soChecklistLoadedFor = null;
    let soChecklistRequest = 0;

    const SO_CHECKLIST_STATUSES = ['Active', 'Completed', 'Graduated'];
    const SO_STATE_DISPLAY = {
        MET: { icon: '✓', text: 'Met' },
        WARNING: { icon: '⚠', text: 'Met with a warning' },
        UNMET: { icon: '✗', text: 'Not met' },
        NOT_DUE: { icon: '—', text: 'Not yet due' }
    };

    function escapeHtml(value) {
        return String(value ?? '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function isBlank(value) {
        return value === null || value === undefined || value === '';
    }

    function renderNullable(value) {
        if (isBlank(value)) {
            return '<span class="text-muted fst-italic">Not Available</span>';
        }
        return escapeHtml(value);
    }

    /**
     * A missing student number is a state the registrar needs to act on, not just
     * absent data — so it gets a warning badge rather than the muted "Not Available"
     * used for optional fields.
     *
     * Orthogonal data (same pattern as the Record ID column): sort and filter on the
     * raw value so the badge markup never reaches DataTables' client-side search.
     */
    function renderStudentNumber(data, type) {
        if (type === 'sort' || type === 'type' || type === 'filter') {
            return isBlank(data) ? '' : data;
        }
        if (isBlank(data)) {
            return '<span class="badge text-bg-warning">Not Assigned</span>';
        }
        return escapeHtml(data);
    }

    function renderStatusBadge(status) {
        if (isBlank(status)) {
            return '<span class="status-badge status-badge-disabled">Not Available</span>';
        }
        let cls;
        if (status === 'Active') {
            cls = 'status-badge-active';
        } else if (status === 'Enrolling' || status === 'Submitted') {
            cls = 'status-badge-enrolling';
        } else if (status === 'Completed') {
            cls = 'status-badge-completed';
        } else if (status === 'Graduated') {
            cls = 'status-badge-graduated';
        } else {
            cls = 'status-badge-disabled';
        }
        return '<span class="status-badge ' + cls + '">' + escapeHtml(status) + '</span>';
    }

    function setText(id, value) {
        const el = document.getElementById(id);
        if (el) {
            el.textContent = isBlank(value) ? 'Not Available' : value;
        }
    }


    function hideAlert(id) {
        const el = document.getElementById(id);
        if (!el) return;
        el.classList.add('d-none');
        el.textContent = '';
    }

    function makeEl(tag, className, text) {
        const node = document.createElement(tag);
        if (className) node.className = className;
        if (text !== undefined) node.textContent = text;
        return node;
    }

    /**
     * Shows the SO Checklist tab only for statuses that have a checklist, always reopens
     * on the Details tab, and forgets any loaded checklist (it reloads on first view).
     */
    function resetSoChecklistTab(status) {
        soChecklistLoadedFor = null;
        soChecklistRequest++; // invalidates any in-flight checklist request
        const content = document.getElementById('soChecklistContent');
        if (content) content.replaceChildren();
        const hasChecklist = SO_CHECKLIST_STATUSES.indexOf(status) !== -1;
        const tabItem = document.getElementById('soChecklistTabItem');
        if (tabItem) tabItem.classList.toggle('d-none', !hasChecklist);
        // Disabled too, so Bootstrap's arrow/Home/End key handling skips the hidden tab.
        const soTab = document.getElementById('tab-details-so');
        if (soTab) soTab.disabled = !hasChecklist;
        const detailsTab = document.getElementById('tab-details-info');
        if (detailsTab) bootstrap.Tab.getOrCreateInstance(detailsTab).show();
    }

    async function loadSoChecklist(recordId) {
        const content = document.getElementById('soChecklistContent');
        if (!content) return;
        const ticket = ++soChecklistRequest;
        content.replaceChildren(makeEl('p', 'text-muted mb-0', 'Loading checklist…'));

        let res;
        try {
            res = await fetch('/api/registrar/student-records/' + encodeURIComponent(recordId) + '/so-checklist', {
                credentials: 'same-origin'
            });
        } catch (err) {
            if (ticket !== soChecklistRequest || recordId !== currentRecordId) return;
            content.replaceChildren(makeEl('div', 'alert alert-danger mb-0',
                'Could not load the SO checklist. Check the connection and try again.'));
            return;
        }
        // Stale: the Registrar opened another student or changed the status meanwhile.
        if (ticket !== soChecklistRequest || recordId !== currentRecordId) return;

        if (res.status === 401) {
            const box = makeEl('div', 'alert alert-warning mb-0');
            box.appendChild(document.createTextNode('Your session has expired. '));
            const link = makeEl('a', '', 'Log in again');
            link.href = 'index.html';
            box.appendChild(link);
            content.replaceChildren(box);
            return;
        }
        if (res.status === 404) {
            content.replaceChildren(makeEl('div', 'alert alert-warning mb-0',
                'This student is no longer available — refresh.'));
            return;
        }
        if (!res.ok) {
            content.replaceChildren(makeEl('div', 'alert alert-danger mb-0', 'Could not load the SO checklist.'));
            return;
        }

        let data;
        try {
            data = await res.json();
        } catch (err) {
            if (ticket !== soChecklistRequest || recordId !== currentRecordId) return;
            content.replaceChildren(makeEl('div', 'alert alert-danger mb-0', 'Could not load the SO checklist.'));
            return;
        }
        if (ticket !== soChecklistRequest || recordId !== currentRecordId) return;
        try {
            renderSoChecklist(content, data);
        } catch (err) {
            // Leave soChecklistLoadedFor unset so revisiting the tab retries.
            content.replaceChildren(makeEl('div', 'alert alert-danger mb-0', 'Could not load the SO checklist.'));
            return;
        }
        soChecklistLoadedFor = recordId;
    }

    function renderSoChecklist(container, data) {
        container.replaceChildren();
        if (data.stage === 'NOT_APPLICABLE') {
            container.appendChild(makeEl('p', 'text-muted mb-0', 'There is no SO checklist for this status.'));
            return;
        }

        if (data.stage === 'PREVIEW') {
            container.appendChild(makeEl('div', 'alert alert-info',
                'Preview — the full checklist applies once the student is Completed.'));
        } else {
            const unmet = data.items.filter(function (i) { return i.state === 'UNMET'; }).length;
            const warnings = data.warningCount
                ? ' — ' + data.warningCount + ' warning' + (data.warningCount === 1 ? '' : 's')
                : '';
            // Never "Ready to file": batch-level SO items are not checked here.
            const summary = data.complete
                ? 'Student requirements: Complete' + warnings
                : 'Student requirements: ' + unmet + ' of ' + data.items.length + ' unmet' + warnings;
            const tone = data.complete ? (data.warningCount ? 'alert-warning' : 'alert-success') : 'alert-danger';
            container.appendChild(makeEl('div', 'alert ' + tone, summary));
        }

        const list = makeEl('ul', 'so-checklist');
        data.items.forEach(function (item) {
            const display = SO_STATE_DISPLAY[item.state] || { icon: '?', text: item.state };
            const row = makeEl('li', 'so-checklist-row');
            const icon = makeEl('span', 'so-state-icon so-state-' + String(item.state).toLowerCase(), display.icon);
            icon.setAttribute('aria-hidden', 'true');
            row.appendChild(icon);
            const body = makeEl('div', '');
            body.appendChild(makeEl('span', 'visually-hidden', display.text + ': '));
            body.appendChild(makeEl('strong', '', item.label));
            body.appendChild(makeEl('p', 'so-checklist-detail', item.detail));
            row.appendChild(body);
            list.appendChild(row);
        });
        container.appendChild(list);
        container.appendChild(makeEl('p', 'text-muted small mt-3 mb-0',
            'Batch-level SO items (List of Students, Attendance Sheet, Registry of Workers, '
            + 'Registry of Workers Assessed) are not checked here.'));
    }

    async function loadRecordDetails(recordId) {
        hideAlert('studentDetailsAlert');

        const response = await fetch('/api/registrar/student-records/' + encodeURIComponent(recordId), {
            credentials: 'same-origin'
        });

        if (!response.ok) {
            throw new Error('Failed to load student record details.');
        }

        const r = await response.json();

        setText('detailsRecordId', r.recordId);
        setText('detailsStudentId', r.studentId);

        // ID picture: HEAD first so a missing picture never renders a broken image.
        // Falls back to the TempProfile placeholder when the student has none on file.
        const idPictureImg = document.getElementById('detailsIdPicture');
        const ID_PICTURE_PLACEHOLDER = 'images/TempProfile%201.webp';
        if (idPictureImg && r.studentId) {
            const pictureUrl = '/api/registrar/documents/id-picture/'
                + encodeURIComponent(r.studentId);
            fetch(pictureUrl, { method: 'HEAD', credentials: 'same-origin' })
                .then(function (response) {
                    if (response.ok) {
                        idPictureImg.src = pictureUrl + '?t=' + Date.now();
                        idPictureImg.alt = 'Student ID picture';
                    } else {
                        idPictureImg.src = ID_PICTURE_PLACEHOLDER;
                        idPictureImg.alt = 'No ID picture on file';
                    }
                })
                .catch(function () {
                    idPictureImg.src = ID_PICTURE_PLACEHOLDER;
                    idPictureImg.alt = 'No ID picture on file';
                });
        }
        setText('detailsStudentNumber', isBlank(r.studentNumber) ? 'Not Assigned' : r.studentNumber);
        setText('detailsLastName', r.lastName);
        setText('detailsFirstName', r.firstName);
        setText('detailsMiddleName', r.middleName);
        setText('detailsBatchCode', r.batchCode);
        setText('detailsCourseCode', r.courseCode);
        setText('detailsSectionCode', r.sectionCode);
        setText('detailsStudentStatus', r.studentStatus);
        setText('detailsBirthdate', r.birthdate);
        setText('detailsAge', r.age);
        setText('detailsSex', r.sex);
        setText('detailsEmail', r.email);
        setText('detailsContactNo', r.contactNo);
        setText('detailsPermanentAddress', r.permanentAddress);

        currentRecordId = r.recordId;
        currentRecordIdentifier = r.studentId
            ? (r.studentId + ' (' + (r.lastName || '') + ', ' + (r.firstName || '') + ')')
            : ('Record #' + r.recordId);
        currentRecordStatus = r.studentStatus;

        const editLink = document.getElementById('studentDetailsEditLink');
        if (editLink) {
            editLink.href = 'student-records.html?id=' + encodeURIComponent(r.recordId);
        }

        resetSoChecklistTab(r.studentStatus);
        detailsModal.show();
    }

    function buildAjaxUrl() {
        const params = new URLSearchParams();
        const fromYear = (document.getElementById('batchFromYear') || {}).value || '';
        const toYear = (document.getElementById('batchToYear') || {}).value || '';
        const status = (document.getElementById('studentStatusFilter') || {}).value || '';
        const hasStudentNumber = (document.getElementById('studentNumberFilter') || {}).value || '';
        if (fromYear) params.set('fromYear', fromYear);
        if (toYear) params.set('toYear', toYear);
        if (status) params.set('status', status);
        if (hasStudentNumber) params.set('hasStudentNumber', hasStudentNumber);
        const qs = params.toString();
        return '/api/registrar/student-records' + (qs ? '?' + qs : '');
    }

    function setFeedback(message, type) {
        const el = document.getElementById('batchFilterFeedback');
        if (!el) return;
        el.textContent = message || '';
        el.style.color = type === 'danger' ? '#b02a37' : '';
    }

    document.addEventListener('DOMContentLoaded', function () {
        if (new URLSearchParams(window.location.search).get('updated') === 'true') {
            const successAlert = document.getElementById('recordUpdatedAlert');
            if (successAlert) {
                successAlert.classList.remove('d-none');
            }
            history.replaceState(null, '', window.location.pathname);
        }

        const tableElement = document.getElementById('studentRecordsTable');
        if (!tableElement || !window.jQuery || !window.bootstrap) {
            return;
        }

        detailsModal = new bootstrap.Modal(document.getElementById('studentRecordDetailsModal'));
        const deleteConfirmEl = document.getElementById('deleteRecordConfirmModal');
        if (deleteConfirmEl) {
            deleteConfirmModal = new bootstrap.Modal(deleteConfirmEl);
        }
        const editStatusEl = document.getElementById('editStatusModal');
        if (editStatusEl) {
            editStatusModal = new bootstrap.Modal(editStatusEl);
        }

        const soTab = document.getElementById('tab-details-so');
        if (soTab) {
            // Load on first view of the tab, not on modal open (spec §8).
            soTab.addEventListener('shown.bs.tab', function () {
                if (currentRecordId && soChecklistLoadedFor !== currentRecordId) {
                    loadSoChecklist(currentRecordId);
                }
            });
        }

        const dataTable = window.jQuery('#studentRecordsTable').DataTable({
            ajax: {
                url: '/api/registrar/student-records',
                dataSrc: ''
            },
            columns: [
                {
                    data: 'recordId',
                    // Orthogonal data: sort/filter on the raw number so ordering is
                    // numeric (17 > 9), not lexical ("9" > "17"); display stays the same.
                    render: function (data, type) {
                        if (type === 'sort' || type === 'type') {
                            return data == null ? -1 : Number(data);
                        }
                        return renderNullable(data);
                    }
                },
                { data: 'studentId', render: renderNullable },
                { data: 'studentNumber', render: renderStudentNumber },
                { data: 'lastName', render: renderNullable },
                { data: 'firstName', render: renderNullable },
                { data: 'batchCode', render: renderNullable },
                { data: 'courseCode', render: renderNullable },
                { data: 'sectionCode', render: renderNullable },
                {
                    data: 'studentStatus',
                    render: function (data) {
                        return renderStatusBadge(data);
                    }
                },
                {
                    data: null,
                    orderable: false,
                    className: 'text-end',
                    render: function (_data, _type, row) {
                        const name = (row.lastName || '') + ', ' + (row.firstName || '');
                        return '<div class="record-actions d-flex gap-1 justify-content-end flex-nowrap">' +
                            '<button class="btn btn-surface-secondary btn-sm js-open-details" data-record-id="' +
                            escapeHtml(row.recordId) + '">Details</button>' +
                            '<button class="btn btn-surface btn-sm js-assign-number" data-record-id="' +
                            escapeHtml(row.recordId) + '" data-student-name="' + escapeHtml(name) +
                            '" data-student-number="' + escapeHtml(row.studentNumber ?? '') +
                            '">Assign Number</button>' +
                            '<button class="btn btn-surface btn-sm js-assign-batch" data-record-id="' +
                            escapeHtml(row.recordId) + '" data-student-name="' + escapeHtml(name) +
                            '" data-batch-code="' + escapeHtml(row.batchCode ?? '') + '">Assign Batch</button>' +
                            '</div>';
                    }
                }
            ],
            // Default sort: newest first (Record ID is auto-increment, so
            // descending puts the most recently created record at the top).
            order: [[0, 'desc']],
            language: {
                emptyTable: 'No student records found.'
            }
        });

        // DataTable init is synchronous — .dt-search is in the DOM right now.
        // Move the batch year filter into the same row as the search input.
        (function () {
            var filterBar = document.getElementById('batchFilterBar');
            var searchCell = document.querySelector('.dt-search')
                          || document.querySelector('.dataTables_filter');
            if (filterBar && searchCell) {
                searchCell.parentNode.insertBefore(filterBar, searchCell);
            }
        }());

        window.jQuery('#studentRecordsTable tbody').on('click', 'button.js-open-details', async function () {
            try {
                await loadRecordDetails(this.getAttribute('data-record-id'));
            } catch (error) {
                window.alert('Unable to load the selected student record right now.');
            }
        });

        window.jQuery('#studentRecordsTable tbody').on('click', 'button.js-assign-number', function () {
            openAssignNumberModal(
                this.getAttribute('data-record-id'),
                this.getAttribute('data-student-name'),
                this.getAttribute('data-student-number')
            );
        });

        window.jQuery('#studentRecordsTable tbody').on('click', 'button.js-assign-batch', function () {
            openAssignBatchModal(
                this.getAttribute('data-record-id'),
                this.getAttribute('data-student-name'),
                this.getAttribute('data-batch-code')
            );
        });

        const applyBtn = document.getElementById('batchFilterApplyBtn');
        if (applyBtn) {
            applyBtn.addEventListener('click', function () {
                const fromYear = document.getElementById('batchFromYear').value;
                const toYear = document.getElementById('batchToYear').value;
                if (fromYear && toYear && Number(fromYear) > Number(toYear)) {
                    setFeedback('"From" year must not be greater than "To" year.', 'danger');
                    return;
                }
                const url = buildAjaxUrl();
                dataTable.ajax.url(url).load(function (json) {
                    const count = Array.isArray(json) ? json.length : 0;
                    if (fromYear || toYear) {
                        const range = (fromYear || '?') + ' – ' + (toYear || '?');
                        setFeedback('Showing ' + count + ' record(s) for batch year ' + range + '.', 'info');
                    } else {
                        setFeedback('Showing ' + count + ' record(s).', 'info');
                    }
                });
            });
        }

        const resetBtn = document.getElementById('batchFilterResetBtn');
        if (resetBtn) {
            resetBtn.addEventListener('click', function () {
                document.getElementById('batchFromYear').value = '';
                document.getElementById('batchToYear').value = '';
                const statusEl = document.getElementById('studentStatusFilter');
                if (statusEl) statusEl.value = '';
                const numberEl = document.getElementById('studentNumberFilter');
                if (numberEl) numberEl.value = '';
                dataTable.ajax.url('/api/registrar/student-records').load(function (json) {
                    const count = Array.isArray(json) ? json.length : 0;
                    setFeedback('Filter cleared. Showing all ' + count + ' record(s).', 'info');
                });
            });
        }

        setupDeleteRecordFlow(dataTable);
        setupAssignStudentNumber(dataTable);
        setupAssignBatch(dataTable);
        setupEditStatus(dataTable);
    });

    function openAssignNumberModal(recordId, studentName, studentNumber) {
        if (!assignNumberModal) return;
        assignTargetRecordId = recordId;

        const nameEl = document.getElementById('assignStudentName');
        const inputEl = document.getElementById('assignStudentNumberInput');
        if (nameEl) nameEl.value = studentName || ('Record #' + recordId);
        if (inputEl) inputEl.value = studentNumber || '';
        hideAlert('assignStudentNumberAlert');

        if (numberCheck) numberCheck.reset();
        assignNumberModal.show();
    }

    function setupAssignStudentNumber(dataTable) {
        const modalEl = document.getElementById('assignStudentNumberModal');
        const saveBtn = document.getElementById('saveStudentNumberBtn');
        const inputEl = document.getElementById('assignStudentNumberInput');
        const alertEl = document.getElementById('assignStudentNumberAlert');

        if (!modalEl || !saveBtn || !inputEl) return;
        assignNumberModal = new bootstrap.Modal(modalEl);
        numberCheck = SrmsStudentNumberCheck.attach({
            input: inputEl,
            saveBtn: saveBtn,
            getRecordId: function () { return assignTargetRecordId; }
        });

        function showAssignAlert(message, type) {
            if (!alertEl) return;
            alertEl.className = 'alert alert-' + type + ' mt-3';
            alertEl.textContent = message;
            alertEl.classList.remove('d-none');
        }

        async function save() {
            if (!assignTargetRecordId) return;
            if (saveBtn.disabled) return; // Enter must not bypass a disabled Save (clash warning or save in flight)

            saveBtn.dataset.busy = '1';
            saveBtn.disabled = true;
            const originalLabel = saveBtn.textContent;
            saveBtn.textContent = 'Saving...';
            hideAlert('assignStudentNumberAlert');

            try {
                const res = await fetch(
                    '/api/registrar/student-records/' + encodeURIComponent(assignTargetRecordId) + '/student-number',
                    {
                        method: 'PUT',
                        credentials: 'same-origin',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ studentNumber: inputEl.value.trim() })
                    }
                );

                if (!res.ok) {
                    // Duplicate-number 400s carry the actionable text in `message`;
                    // Bean Validation 400s put it under `errors.studentNumber`, with
                    // `message` only saying "Validation failed".
                    const body = await res.json().catch(function () { return null; });
                    const fieldError = body && body.errors && body.errors.studentNumber;
                    showAssignAlert(
                        fieldError || (body && body.message) || 'Could not save the student number.',
                        'danger'
                    );
                    return;
                }

                const saved = await res.json();
                showAssignAlert(
                    saved.studentNumber
                        ? 'Student number saved as ' + saved.studentNumber + '.'
                        : 'Student number cleared.',
                    'success'
                );
                window.setTimeout(function () {
                    assignNumberModal.hide();
                    dataTable.ajax.reload(null, false);
                }, 900);
            } catch (err) {
                showAssignAlert('Network error. Could not save the student number.', 'danger');
            } finally {
                delete saveBtn.dataset.busy;
                saveBtn.disabled = inputEl.classList.contains('is-invalid');
                saveBtn.textContent = originalLabel;
            }
        }

        saveBtn.addEventListener('click', save);
        inputEl.addEventListener('keydown', function (e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                save();
            }
        });

        modalEl.addEventListener('hidden.bs.modal', function () {
            assignTargetRecordId = null;
            inputEl.value = '';
            hideAlert('assignStudentNumberAlert');
        });
    }

    let batchLookupLoaded = false;
    let batchCombobox = null;

    function openAssignBatchModal(recordId, studentName, batchCode) {
        if (!assignBatchModal) return;
        assignBatchTargetRecordId = recordId;

        const nameEl = document.getElementById('assignBatchStudentName');
        const inputEl = document.getElementById('assignBatchInput');
        if (nameEl) nameEl.value = studentName || ('Record #' + recordId);
        if (inputEl) inputEl.value = batchCode || '';
        hideAlert('assignBatchAlert');

        if (!batchLookupLoaded) {
            loadBatchLookupOptions();
        }

        assignBatchModal.show();
    }

    async function loadBatchLookupOptions() {
        try {
            const response = await fetch('/api/lookup/batches', { credentials: 'same-origin' });
            if (!response.ok || !batchCombobox) return;
            const items = await response.json();
            batchCombobox.setItems(items.map(function (item) {
                return { value: item.code, label: item.code, hint: item.name };
            }));
            batchLookupLoaded = true;
        } catch (error) {
            // List stays empty; registrar can still type a free-text code.
        }
    }

    function setupAssignBatch(dataTable) {
        const modalEl = document.getElementById('assignBatchModal');
        const saveBtn = document.getElementById('saveBatchBtn');
        const inputEl = document.getElementById('assignBatchInput');
        const alertEl = document.getElementById('assignBatchAlert');

        if (!modalEl || !saveBtn || !inputEl) return;
        assignBatchModal = new bootstrap.Modal(modalEl);
        batchCombobox = SrmsCombobox.attach(inputEl, {
            emptyText: 'No existing batch matches — saving will create it.'
        });

        function showBatchAlert(message, type) {
            if (!alertEl) return;
            alertEl.className = 'alert alert-' + type + ' mt-3';
            alertEl.textContent = message;
            alertEl.classList.remove('d-none');
        }

        async function save() {
            if (!assignBatchTargetRecordId) return;

            saveBtn.disabled = true;
            const originalLabel = saveBtn.textContent;
            saveBtn.textContent = 'Saving...';
            hideAlert('assignBatchAlert');

            try {
                const res = await fetch(
                    '/api/registrar/student-records/' + encodeURIComponent(assignBatchTargetRecordId) + '/batch',
                    {
                        method: 'PUT',
                        credentials: 'same-origin',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ batchCode: inputEl.value.trim() })
                    }
                );

                if (!res.ok) {
                    const body = await res.json().catch(function () { return null; });
                    const fieldError = body && body.errors && body.errors.batchCode;
                    showBatchAlert(
                        fieldError || (body && body.message) || 'Could not save the batch.',
                        'danger'
                    );
                    return;
                }

                const saved = await res.json();
                batchLookupLoaded = false;
                showBatchAlert(
                    saved.batchCode
                        ? 'Batch saved as ' + saved.batchCode + '.'
                        : 'Batch cleared.',
                    'success'
                );
                window.setTimeout(function () {
                    assignBatchModal.hide();
                    dataTable.ajax.reload(null, false);
                }, 900);
            } catch (err) {
                showBatchAlert('Network error. Could not save the batch.', 'danger');
            } finally {
                saveBtn.disabled = false;
                saveBtn.textContent = originalLabel;
            }
        }

        saveBtn.addEventListener('click', save);
        inputEl.addEventListener('keydown', function (e) {
            if (e.defaultPrevented) return; // combobox already consumed this Enter to pick an option
            if (e.key === 'Enter') {
                e.preventDefault();
                save();
            }
        });

        modalEl.addEventListener('hidden.bs.modal', function () {
            assignBatchTargetRecordId = null;
            inputEl.value = '';
            hideAlert('assignBatchAlert');
        });
    }

    function setupDeleteRecordFlow(dataTable) {
        const deleteBtn = document.getElementById('deleteRecordBtn');
        const confirmInput = document.getElementById('deleteRecordConfirmInput');
        const confirmBtn = document.getElementById('confirmDeleteRecordBtn');
        const identifierEl = document.getElementById('deleteRecordIdentifier');
        const resultAlert = document.getElementById('deleteRecordResultAlert');
        const modalEl = document.getElementById('deleteRecordConfirmModal');

        if (!deleteBtn || !confirmInput || !confirmBtn || !deleteConfirmModal) return;

        deleteBtn.addEventListener('click', function () {
            if (!currentRecordId) return;
            if (identifierEl) {
                identifierEl.textContent = currentRecordIdentifier || ('Record #' + currentRecordId);
            }
            confirmInput.value = '';
            confirmBtn.disabled = true;
            if (resultAlert) {
                resultAlert.classList.add('d-none');
                resultAlert.textContent = '';
            }
            detailsModal.hide();
            deleteConfirmModal.show();
        });

        confirmInput.addEventListener('input', function () {
            confirmBtn.disabled = confirmInput.value.trim().toLowerCase() !== 'delete';
        });

        confirmBtn.addEventListener('click', async function () {
            if (!currentRecordId) return;
            if (confirmInput.value.trim().toLowerCase() !== 'delete') return;

            confirmBtn.disabled = true;
            const originalLabel = confirmBtn.textContent;
            confirmBtn.textContent = 'Deleting...';

            try {
                const res = await fetch('/api/registrar/student-records/' + encodeURIComponent(currentRecordId), {
                    method: 'DELETE',
                    credentials: 'same-origin'
                });
                if (!res.ok) {
                    const msg = await res.text().catch(function () { return 'Delete failed.'; });
                    if (resultAlert) {
                        resultAlert.className = 'alert alert-danger mt-3';
                        resultAlert.textContent = msg || 'Delete failed.';
                        resultAlert.classList.remove('d-none');
                    }
                    confirmBtn.disabled = false;
                    confirmBtn.textContent = originalLabel;
                    return;
                }
                if (resultAlert) {
                    resultAlert.className = 'alert alert-success mt-3';
                    resultAlert.textContent = 'Student record deleted successfully.';
                    resultAlert.classList.remove('d-none');
                }
                window.setTimeout(function () {
                    deleteConfirmModal.hide();
                    dataTable.ajax.reload(null, false);
                }, 900);
            } catch (err) {
                if (resultAlert) {
                    resultAlert.className = 'alert alert-danger mt-3';
                    resultAlert.textContent = 'Network error. Could not delete the record.';
                    resultAlert.classList.remove('d-none');
                }
                confirmBtn.disabled = false;
                confirmBtn.textContent = originalLabel;
            }
        });

        if (modalEl) {
            modalEl.addEventListener('hidden.bs.modal', function () {
                confirmInput.value = '';
                confirmBtn.disabled = true;
                confirmBtn.textContent = 'Permanently Delete';
                if (resultAlert) {
                    resultAlert.classList.add('d-none');
                    resultAlert.textContent = '';
                }
            });
        }
    }

    // Mirrors service/StudentStatusTransitions.java so the dialog only offers moves the
    // server will accept. The server stays the authority and re-checks every change.
    function statusRule(from, to) {
        if (from === to) return { allowed: true };
        if (to === 'Completed') {
            if (from === 'Active') return { allowed: true, needsDate: true };
            if (from === 'Graduated') return { allowed: true, needsReason: true };
            return { allowed: false };
        }
        if (to === 'Graduated') {
            if (from === 'Completed') return { allowed: true };
            if (from === 'Active') return { allowed: true, needsDate: true, needsReason: true, archive: true };
            return { allowed: false };
        }
        if (from === 'Graduated') return { allowed: false };
        if (from === 'Completed') return { allowed: to === 'Active' };
        return { allowed: true };
    }

    /** Today as yyyy-mm-dd in the browser's local time (toISOString would give the UTC date). */
    function todayIso() {
        const d = new Date();
        return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-'
            + String(d.getDate()).padStart(2, '0');
    }

    function setupEditStatus(dataTable) {
        const editBtn = document.getElementById('editStatusBtn');
        const modalEl = document.getElementById('editStatusModal');
        const nameEl = document.getElementById('editStatusStudentName');
        const selectEl = document.getElementById('editStatusSelect');
        const saveBtn = document.getElementById('saveStatusBtn');
        const alertEl = document.getElementById('editStatusAlert');
        const dateGroup = document.getElementById('editStatusDateGroup');
        const dateInput = document.getElementById('editStatusCompletionDate');
        const dateHelp = document.getElementById('editStatusDateHelp');
        const reasonGroup = document.getElementById('editStatusReasonGroup');
        const reasonInput = document.getElementById('editStatusReason');

        if (!editBtn || !modalEl || !selectEl || !saveBtn || !editStatusModal
                || !dateGroup || !dateInput || !reasonGroup || !reasonInput) return;

        function showStatusAlert(message, type) {
            if (!alertEl) return;
            alertEl.className = 'alert alert-' + type + ' mt-0 mb-3';
            alertEl.textContent = message;
            alertEl.classList.remove('d-none');
        }

        // True while the date input holds the auto-filled "today" rather than a typed date.
        let dateIsDefault = false;

        function currentRule() {
            return statusRule(currentRecordStatus, selectEl.value);
        }

        function updateStatusFields() {
            const rule = currentRule();
            dateGroup.classList.toggle('d-none', !rule.needsDate);
            reasonGroup.classList.toggle('d-none', !rule.needsReason);
            saveBtn.disabled = !selectEl.value;
            if (rule.needsDate) {
                if (rule.archive) {
                    // Archive records need the real, long-past date: never default it, but keep
                    // anything the Registrar typed themselves.
                    if (dateIsDefault) {
                        dateInput.value = '';
                        dateIsDefault = false;
                    }
                } else if (!dateInput.value) {
                    dateInput.value = todayIso();
                    dateIsDefault = true;
                }
            }
            if (dateHelp) {
                dateHelp.textContent = rule.archive
                    ? 'Moving an Active record straight to Graduated is for digitized archive records. '
                        + 'Enter the date this student actually finished training and OJT.'
                    : 'The day the student finished training and OJT. Defaults to today.';
            }
        }

        editBtn.addEventListener('click', function () {
            if (!currentRecordId) return;
            if (nameEl) {
                nameEl.textContent = currentRecordIdentifier || ('Record #' + currentRecordId);
            }
            Array.prototype.forEach.call(selectEl.options, function (option) {
                option.disabled = !statusRule(currentRecordStatus, option.value).allowed;
            });
            selectEl.value = currentRecordStatus || 'Enrolling';
            dateInput.value = '';
            dateIsDefault = false;
            dateInput.max = todayIso();
            reasonInput.value = '';
            updateStatusFields();
            hideAlert('editStatusAlert');
            detailsModal.hide();
            editStatusModal.show();
        });

        selectEl.addEventListener('change', function () {
            hideAlert('editStatusAlert');
            updateStatusFields();
        });

        dateInput.addEventListener('input', function () {
            dateIsDefault = false;
        });

        saveBtn.addEventListener('click', async function () {
            if (!currentRecordId || !selectEl.value) return;

            const rule = currentRule();
            const payload = { studentStatus: selectEl.value };
            if (rule.needsDate) {
                if (!dateInput.value) {
                    showStatusAlert('Enter the completion date.', 'danger');
                    return;
                }
                payload.completionDate = dateInput.value;
            }
            if (rule.needsReason) {
                if (!reasonInput.value.trim()) {
                    showStatusAlert('Enter a reason for this change.', 'danger');
                    return;
                }
                payload.reason = reasonInput.value.trim();
            }

            saveBtn.disabled = true;
            const originalLabel = saveBtn.textContent;
            saveBtn.textContent = 'Saving...';
            hideAlert('editStatusAlert');

            try {
                const res = await fetch(
                    '/api/registrar/student-records/' + encodeURIComponent(currentRecordId) + '/status',
                    {
                        method: 'PUT',
                        credentials: 'same-origin',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(payload)
                    }
                );

                if (!res.ok) {
                    const body = await res.json().catch(function () { return null; });
                    const fieldErrors = body && body.errors ? Object.values(body.errors).join(' ') : '';
                    showStatusAlert(
                        fieldErrors || (body && body.message) || 'Could not save the status.',
                        'danger'
                    );
                    return;
                }

                const saved = await res.json();
                currentRecordStatus = saved.studentStatus;
                setText('detailsStudentStatus', saved.studentStatus);
                resetSoChecklistTab(saved.studentStatus);
                dataTable.ajax.reload(null, false);
                editStatusModal.hide();
            } catch (err) {
                showStatusAlert('Network error. Could not save the status.', 'danger');
            } finally {
                saveBtn.disabled = false;
                saveBtn.textContent = originalLabel;
            }
        });

        // The status modal is opened from within the details modal (details hides first
        // to avoid stacked-modal focus issues), so whenever it closes — Cancel, X, or a
        // successful save above — bring the details modal back rather than leaving the
        // registrar with nothing open.
        modalEl.addEventListener('hidden.bs.modal', function () {
            hideAlert('editStatusAlert');
            detailsModal.show();
        });
    }
})();
