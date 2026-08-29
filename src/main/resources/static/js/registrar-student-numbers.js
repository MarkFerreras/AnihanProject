/**
 * registrar-student-numbers.js
 *
 * The Student Numbers report page: filter students, export the filtered set for
 * encoding, import the completed sheet back (preview then apply), and assign a single
 * number through the same audited endpoint the dashboard uses.
 *
 * Reads the existing GET /api/registrar/student-records — no dedicated list endpoint.
 */
(function () {
    'use strict';

    let assignNumberModal = null;
    let assignTargetRecordId = null;
    let dataTable = null;

    /** Set once a preview reports applicable rows; cleared whenever the file changes. */
    let previewedFile = null;

    // Outcomes that cause a write. Must mirror StudentNumberImportOutcome.applicable().
    const APPLICABLE_OUTCOMES = ['WILL_ASSIGN', 'WILL_OVERWRITE', 'NAME_MISMATCH'];

    const OUTCOME_LABELS = {
        WILL_ASSIGN: { text: 'Will assign', cls: 'text-bg-success' },
        WILL_OVERWRITE: { text: 'Will replace', cls: 'text-bg-success' },
        NAME_MISMATCH: { text: 'Name mismatch', cls: 'text-bg-warning' },
        CONFLICT_IN_USE: { text: 'Number in use', cls: 'text-bg-danger' },
        CONFLICT_EXISTING: { text: 'Already has one', cls: 'text-bg-danger' },
        UNCHANGED: { text: 'Unchanged', cls: 'text-bg-secondary' },
        UNKNOWN_REFERENCE: { text: 'Unknown student', cls: 'text-bg-danger' },
        DUPLICATE_IN_FILE: { text: 'Duplicate in file', cls: 'text-bg-danger' },
        INVALID_FORMAT: { text: 'Invalid format', cls: 'text-bg-danger' },
        BLANK: { text: 'Blank — skipped', cls: 'text-bg-secondary' }
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
        } else if (status === 'Graduated') {
            cls = 'status-badge-graduated';
        } else {
            cls = 'status-badge-disabled';
        }
        return '<span class="status-badge ' + cls + '">' + escapeHtml(status) + '</span>';
    }

    function showAlert(id, message, type) {
        const el = document.getElementById(id);
        if (!el) return;
        el.className = 'alert alert-' + type + ' mt-3';
        el.textContent = message;
        el.classList.remove('d-none');
    }

    function hideAlert(id) {
        const el = document.getElementById(id);
        if (!el) return;
        el.classList.add('d-none');
        el.textContent = '';
    }

    /** Query string shared by the table load and the export, so they always agree. */
    function buildFilterParams() {
        const params = new URLSearchParams();
        const fromYear = (document.getElementById('batchFromYear') || {}).value || '';
        const toYear = (document.getElementById('batchToYear') || {}).value || '';
        const status = (document.getElementById('studentStatusFilter') || {}).value || '';
        const hasStudentNumber = (document.getElementById('studentNumberFilter') || {}).value || '';
        if (fromYear) params.set('fromYear', fromYear);
        if (toYear) params.set('toYear', toYear);
        if (status) params.set('status', status);
        if (hasStudentNumber) params.set('hasStudentNumber', hasStudentNumber);
        return params;
    }

    function buildAjaxUrl() {
        const qs = buildFilterParams().toString();
        return '/api/registrar/student-records' + (qs ? '?' + qs : '');
    }

    /**
     * The headline number the page exists to answer: how many students still need one.
     * Counted from an unfiltered fetch so the figure does not move with the filters.
     */
    async function refreshSummary() {
        const el = document.getElementById('snSummary');
        if (!el) return;
        try {
            const res = await fetch('/api/registrar/student-records', { credentials: 'same-origin' });
            if (!res.ok) throw new Error('load failed');
            const all = await res.json();
            const missing = all.filter(r => isBlank(r.studentNumber)).length;

            el.classList.toggle('has-missing', missing > 0);
            el.textContent = missing === 0
                ? 'All ' + all.length + ' students have a student number.'
                : missing + ' of ' + all.length + ' students still need a student number.';
        } catch (err) {
            el.textContent = 'Could not load the student number summary.';
        }
    }

    // ----- Export -----

    async function exportSheet() {
        hideAlert('exportAlert');
        const btn = document.getElementById('exportBtn');
        const format = document.getElementById('exportFormat').value;
        const original = btn.textContent;
        btn.disabled = true;
        btn.textContent = 'Exporting...';

        try {
            const params = buildFilterParams();
            params.set('format', format);
            const res = await fetch('/api/registrar/student-numbers/export?' + params.toString(),
                { credentials: 'same-origin' });

            if (res.status === 401) {
                showAlert('exportAlert', 'Session expired. Redirecting to login...', 'danger');
                setTimeout(function () { window.location.href = '/index.html'; }, 1500);
                return;
            }
            if (!res.ok) {
                const body = await res.json().catch(function () { return null; });
                showAlert('exportAlert', (body && body.message) || 'Export failed. Please try again.', 'danger');
                return;
            }

            // Same download approach as the system-logs export.
            const blob = await res.blob();
            const objectUrl = window.URL.createObjectURL(blob);
            const disposition = res.headers.get('Content-Disposition') || '';
            const match = disposition.match(/filename="?([^"]+)"?/i);
            const link = document.createElement('a');
            link.href = objectUrl;
            link.download = match ? match[1] : 'student-numbers.' + format;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            window.URL.revokeObjectURL(objectUrl);

            showAlert('exportAlert', 'Sheet downloaded. Fill in the Student Number column, then import it back.', 'success');
        } catch (err) {
            showAlert('exportAlert', 'Network error. Could not export the sheet.', 'danger');
        } finally {
            btn.disabled = false;
            btn.textContent = original;
        }
    }

    // ----- Import -----

    function selectedFile() {
        const input = document.getElementById('importFile');
        return input && input.files && input.files.length ? input.files[0] : null;
    }

    function renderImportReport(report) {
        const card = document.getElementById('importResultCard');
        const title = document.getElementById('importResultTitle');
        const summary = document.getElementById('importResultSummary');
        const tbody = document.querySelector('#importResultTable tbody');

        title.textContent = report.applied ? 'Import Applied' : 'Import Preview';

        const skipped = report.totalRows - report.applicableRows;
        summary.textContent = report.fileName + ' — ' + report.totalRows + ' row(s) read; '
            + report.applicableRows + (report.applied ? ' applied' : ' ready to apply')
            + ', ' + skipped + ' skipped.';

        tbody.innerHTML = report.rows.map(function (row) {
            const label = OUTCOME_LABELS[row.outcome] || { text: row.outcome, cls: 'text-bg-secondary' };
            return '<tr>'
                + '<td>' + escapeHtml(row.rowNumber) + '</td>'
                + '<td>' + renderNullable(row.reference) + '</td>'
                + '<td>' + renderNullable(row.studentName) + '</td>'
                + '<td>' + renderNullable(row.studentNumber) + '</td>'
                + '<td><span class="badge ' + label.cls + '">' + escapeHtml(label.text) + '</span></td>'
                + '<td>' + escapeHtml(row.message || '') + '</td>'
                + '</tr>';
        }).join('');

        card.classList.remove('d-none');
        card.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }

    async function postImport(path) {
        const file = selectedFile();
        if (!file) {
            showAlert('importAlert', 'Choose a .csv or .xlsx file first.', 'warning');
            return null;
        }

        const formData = new FormData();
        formData.append('file', file);
        formData.append('allowOverwrite', document.getElementById('allowOverwrite').checked);

        const res = await fetch('/api/registrar/student-numbers/' + path, {
            method: 'POST',
            credentials: 'same-origin',
            body: formData
        });

        if (res.status === 401) {
            showAlert('importAlert', 'Session expired. Redirecting to login...', 'danger');
            setTimeout(function () { window.location.href = '/index.html'; }, 1500);
            return null;
        }
        if (!res.ok) {
            const body = await res.json().catch(function () { return null; });
            showAlert('importAlert',
                (body && body.message) || 'The file could not be read. Please check the format.', 'danger');
            return null;
        }
        return res.json();
    }

    async function previewImport() {
        hideAlert('importAlert');
        const btn = document.getElementById('previewImportBtn');
        const applyBtn = document.getElementById('applyImportBtn');
        const original = btn.textContent;
        btn.disabled = true;
        btn.textContent = 'Checking...';
        applyBtn.disabled = true;

        try {
            const report = await postImport('import/preview');
            if (!report) return;

            renderImportReport(report);

            if (report.applicableRows > 0) {
                previewedFile = selectedFile();
                applyBtn.disabled = false;
                showAlert('importAlert',
                    'Review the preview below, then choose Apply to save ' + report.applicableRows + ' change(s).',
                    'info');
            } else {
                previewedFile = null;
                showAlert('importAlert',
                    'Nothing in this file can be applied. See the reasons below.', 'warning');
            }
        } catch (err) {
            showAlert('importAlert', 'Network error. Could not read the file.', 'danger');
        } finally {
            btn.disabled = false;
            btn.textContent = original;
        }
    }

    async function applyImport() {
        hideAlert('importAlert');
        const btn = document.getElementById('applyImportBtn');
        const original = btn.textContent;
        btn.disabled = true;
        btn.textContent = 'Applying...';

        try {
            const report = await postImport('import/apply');
            if (!report) return;

            renderImportReport(report);
            showAlert('importAlert',
                report.applicableRows + ' student number(s) assigned.', 'success');

            previewedFile = null;
            dataTable.ajax.url(buildAjaxUrl()).load(null, false);
            refreshSummary();
        } catch (err) {
            showAlert('importAlert', 'Network error. The import may not have been applied — re-check the table.', 'danger');
        } finally {
            btn.textContent = original;
            // Stays disabled until a fresh preview: applying the same sheet twice is
            // harmless but confusing, and the file may have been changed meanwhile.
            btn.disabled = true;
        }
    }

    // ----- Single assignment (same endpoint as the dashboard) -----

    function openAssignNumberModal(recordId, studentName, studentNumber) {
        if (!assignNumberModal) return;
        assignTargetRecordId = recordId;
        document.getElementById('assignStudentName').value = studentName || ('Record #' + recordId);
        document.getElementById('assignStudentNumberInput').value = studentNumber || '';
        hideAlert('assignStudentNumberAlert');
        assignNumberModal.show();
    }

    function setupAssignStudentNumber() {
        const modalEl = document.getElementById('assignStudentNumberModal');
        const saveBtn = document.getElementById('saveStudentNumberBtn');
        const inputEl = document.getElementById('assignStudentNumberInput');
        if (!modalEl || !saveBtn || !inputEl) return;

        assignNumberModal = new bootstrap.Modal(modalEl);

        async function save() {
            if (!assignTargetRecordId) return;
            saveBtn.disabled = true;
            const original = saveBtn.textContent;
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
                    const body = await res.json().catch(function () { return null; });
                    const fieldError = body && body.errors && body.errors.studentNumber;
                    showAlert('assignStudentNumberAlert',
                        fieldError || (body && body.message) || 'Could not save the student number.', 'danger');
                    return;
                }

                const saved = await res.json();
                showAlert('assignStudentNumberAlert',
                    saved.studentNumber
                        ? 'Student number saved as ' + saved.studentNumber + '.'
                        : 'Student number cleared.',
                    'success');

                window.setTimeout(function () {
                    assignNumberModal.hide();
                    dataTable.ajax.reload(null, false);
                    refreshSummary();
                }, 900);
            } catch (err) {
                showAlert('assignStudentNumberAlert', 'Network error. Could not save the student number.', 'danger');
            } finally {
                saveBtn.disabled = false;
                saveBtn.textContent = original;
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

    // ----- Boot -----

    document.addEventListener('DOMContentLoaded', function () {
        setupAssignStudentNumber();

        dataTable = window.jQuery('#studentNumbersTable').DataTable({
            ajax: { url: '/api/registrar/student-records', dataSrc: '' },
            columns: [
                { data: 'studentId', render: renderNullable },
                { data: 'studentNumber', render: renderStudentNumber },
                { data: 'lastName', render: renderNullable },
                { data: 'firstName', render: renderNullable },
                { data: 'batchCode', render: renderNullable },
                { data: 'courseCode', render: renderNullable },
                { data: 'sectionCode', render: renderNullable },
                { data: 'studentStatus', render: function (data) { return renderStatusBadge(data); } },
                {
                    data: null,
                    orderable: false,
                    className: 'text-end',
                    render: function (_data, _type, row) {
                        const name = (row.lastName || '') + ', ' + (row.firstName || '');
                        return '<div class="record-actions d-flex gap-1 justify-content-end flex-nowrap">'
                            + '<button class="btn btn-surface btn-sm js-assign-number" data-record-id="'
                            + escapeHtml(row.recordId) + '" data-student-name="' + escapeHtml(name)
                            + '" data-student-number="' + escapeHtml(row.studentNumber ?? '')
                            + '">Assign Number</button>'
                            + '</div>';
                    }
                }
            ],
            // Students without a number first — the ones needing attention.
            order: [[1, 'asc']],
            language: { emptyTable: 'No student records match these filters.' }
        });

        // Move the filter bar into the DataTables search row, as the other pages do.
        (function () {
            const filterBar = document.getElementById('snFilterBar');
            const searchCell = document.querySelector('.dt-search') || document.querySelector('.dataTables_filter');
            if (filterBar && searchCell) {
                searchCell.parentNode.insertBefore(filterBar, searchCell);
            }
        }());

        window.jQuery('#studentNumbersTable tbody').on('click', 'button.js-assign-number', function () {
            openAssignNumberModal(
                this.getAttribute('data-record-id'),
                this.getAttribute('data-student-name'),
                this.getAttribute('data-student-number')
            );
        });

        const feedback = document.getElementById('snFilterFeedback');
        function setFeedback(message, type) {
            if (!feedback) return;
            feedback.textContent = message || '';
            feedback.style.color = type === 'danger' ? '#b02a37' : '';
        }

        document.getElementById('snFilterApplyBtn').addEventListener('click', function () {
            const fromYear = document.getElementById('batchFromYear').value;
            const toYear = document.getElementById('batchToYear').value;
            if (fromYear && toYear && Number(fromYear) > Number(toYear)) {
                setFeedback('"From" year must not be greater than "To" year.', 'danger');
                return;
            }
            setFeedback('');
            dataTable.ajax.url(buildAjaxUrl()).load(function (json) {
                const count = Array.isArray(json) ? json.length : 0;
                setFeedback(count + ' student(s) match these filters.');
            }, false);
        });

        document.getElementById('snFilterResetBtn').addEventListener('click', function () {
            document.getElementById('batchFromYear').value = '';
            document.getElementById('batchToYear').value = '';
            document.getElementById('studentStatusFilter').value = '';
            document.getElementById('studentNumberFilter').value = '';
            setFeedback('');
            dataTable.ajax.url('/api/registrar/student-records').load(null, false);
        });

        document.getElementById('exportBtn').addEventListener('click', exportSheet);
        document.getElementById('previewImportBtn').addEventListener('click', previewImport);
        document.getElementById('applyImportBtn').addEventListener('click', applyImport);

        // Choosing a different file invalidates the previous preview — Apply must not
        // silently act on a sheet the registrar has not reviewed.
        document.getElementById('importFile').addEventListener('change', function () {
            previewedFile = null;
            document.getElementById('applyImportBtn').disabled = true;
            document.getElementById('importResultCard').classList.add('d-none');
            hideAlert('importAlert');
        });

        // Same reasoning: the overwrite flag changes what would be applied.
        document.getElementById('allowOverwrite').addEventListener('change', function () {
            previewedFile = null;
            document.getElementById('applyImportBtn').disabled = true;
        });

        refreshSummary();
    });
})();
