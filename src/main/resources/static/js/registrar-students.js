(function () {
    'use strict';

    let detailsModal = null;

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
            return 'null';
        }
        return escapeHtml(value);
    }

    function renderStatusBadge(status) {
        if (isBlank(status)) {
            return '<span class="status-badge status-badge-disabled">null</span>';
        }
        const cls = (status === 'Active' || status === 'Submitted')
            ? 'status-badge-active'
            : 'status-badge-disabled';
        return '<span class="status-badge ' + cls + '">' + escapeHtml(status) + '</span>';
    }

    function setText(id, value) {
        const el = document.getElementById(id);
        if (el) {
            el.textContent = isBlank(value) ? 'null' : value;
        }
    }

    function setAlert(id, message, type) {
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
        setText('detailsLastName', r.lastName);
        setText('detailsFirstName', r.firstName);
        setText('detailsMiddleName', r.middleName);
        setText('detailsBirthdate', r.birthdate);
        setText('detailsAge', r.age);
        setText('detailsSex', r.sex);
        setText('detailsCivilStatus', r.civilStatus);
        setText('detailsPermanentAddress', r.permanentAddress);
        setText('detailsTemporaryAddress', r.temporaryAddress);
        setText('detailsEmail', r.email);
        setText('detailsContactNo', r.contactNo);
        setText('detailsReligion', r.religion);
        setText('detailsBaptized', r.baptized === null || r.baptized === undefined
            ? null
            : (r.baptized ? 'Yes' : 'No'));
        setText('detailsBaptismDate', r.baptismDate);
        setText('detailsBaptismPlace', r.baptismPlace);
        setText('detailsSiblingCount', r.siblingCount);
        setText('detailsBrotherCount', r.brotherCount);
        setText('detailsSisterCount', r.sisterCount);
        setText('detailsBatchCode', r.batchCode);
        setText('detailsCourseCode', r.courseCode);
        setText('detailsSectionCode', r.sectionCode);
        setText('detailsEnrollmentDate', r.enrollmentDate);
        setText('detailsStudentStatus', r.studentStatus);

        const editLink = document.getElementById('studentDetailsEditLink');
        if (editLink) {
            editLink.href = 'student-records.html?id=' + encodeURIComponent(r.recordId);
        }

        detailsModal.show();
    }

    function buildAjaxUrl() {
        const params = new URLSearchParams();
        const fromYear = (document.getElementById('batchFromYear') || {}).value || '';
        const toYear = (document.getElementById('batchToYear') || {}).value || '';
        if (fromYear) params.set('fromYear', fromYear);
        if (toYear) params.set('toYear', toYear);
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
        const tableElement = document.getElementById('studentRecordsTable');
        if (!tableElement || !window.jQuery || !window.bootstrap) {
            return;
        }

        detailsModal = new bootstrap.Modal(document.getElementById('studentRecordDetailsModal'));

        const dataTable = window.jQuery('#studentRecordsTable').DataTable({
            ajax: {
                url: '/api/registrar/student-records',
                dataSrc: ''
            },
            columns: [
                { data: 'recordId', render: renderNullable },
                { data: 'studentId', render: renderNullable },
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
                    render: function (data, type, row) {
                        return '<button class="btn btn-surface-secondary btn-sm" data-record-id="' +
                            escapeHtml(row.recordId) + '">Open Details</button>';
                    }
                }
            ],
            order: [[0, 'asc']],
            language: {
                emptyTable: 'No student records found.'
            },
            initComplete: function () {
                const filterBar = document.getElementById('batchFilterBar');
                if (!filterBar) return;
                const wrapper = this.table().container();
                // Works for DataTables 2 (.dt-search) and DT1 compat (.dataTables_filter)
                const searchCell = wrapper.querySelector('.dt-search')
                                || wrapper.querySelector('.dataTables_filter');
                if (!searchCell) return;
                const row = searchCell.parentNode;
                row.insertBefore(filterBar, searchCell);
                // DT1 compat: the parent may use floats; force flex so items share a row
                if (!row.classList.contains('dt-layout-row')) {
                    row.style.cssText += ';display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;gap:0.75rem;';
                }
            }
        });

        window.jQuery('#studentRecordsTable tbody').on('click', 'button[data-record-id]', async function () {
            try {
                await loadRecordDetails(this.getAttribute('data-record-id'));
            } catch (error) {
                window.alert('Unable to load the selected student record right now.');
            }
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
                dataTable.ajax.url('/api/registrar/student-records').load(function (json) {
                    const count = Array.isArray(json) ? json.length : 0;
                    setFeedback('Filter cleared. Showing all ' + count + ' record(s).', 'info');
                });
            });
        }
    });
})();
