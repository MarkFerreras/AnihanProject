(function () {
    'use strict';

    let detailsModal = null;
    let deleteConfirmModal = null;
    let currentRecordId = null;
    let currentRecordIdentifier = null;

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

        const f = r.father || {};
        setText('detailsFatherFamilyName', f.familyName);
        setText('detailsFatherFirstName',  f.firstName);
        setText('detailsFatherMiddleName', f.middleName);
        setText('detailsFatherBirthdate',  f.birthdate);
        setText('detailsFatherOccupation', f.occupation);
        setText('detailsFatherEstIncome',  f.estIncome);
        setText('detailsFatherContactNo',  f.contactNo);
        setText('detailsFatherEmail',      f.email);
        setText('detailsFatherAddress',    f.address);

        const m = r.mother || {};
        setText('detailsMotherFamilyName', m.familyName);
        setText('detailsMotherFirstName',  m.firstName);
        setText('detailsMotherMiddleName', m.middleName);
        setText('detailsMotherBirthdate',  m.birthdate);
        setText('detailsMotherOccupation', m.occupation);
        setText('detailsMotherEstIncome',  m.estIncome);
        setText('detailsMotherContactNo',  m.contactNo);
        setText('detailsMotherEmail',      m.email);
        setText('detailsMotherAddress',    m.address);

        const g = r.guardian || {};
        setText('detailsGuardianRelation',   g.relation);
        setText('detailsGuardianLastName',   g.lastName);
        setText('detailsGuardianFirstName',  g.firstName);
        setText('detailsGuardianMiddleName', g.middleName);
        setText('detailsGuardianBirthdate',  g.birthdate);
        setText('detailsGuardianAddress',    g.address);

        currentRecordId = r.recordId;
        currentRecordIdentifier = r.studentId
            ? (r.studentId + ' (' + (r.lastName || '') + ', ' + (r.firstName || '') + ')')
            : ('Record #' + r.recordId);

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
        const status = (document.getElementById('studentStatusFilter') || {}).value || '';
        if (fromYear) params.set('fromYear', fromYear);
        if (toYear) params.set('toYear', toYear);
        if (status) params.set('status', status);
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
                    render: function (_data, _type, row) {
                        return '<button class="btn btn-surface-secondary btn-sm" data-record-id="' +
                            escapeHtml(row.recordId) + '">Open Details</button>';
                    }
                }
            ],
            order: [[0, 'asc']],
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
                const statusEl = document.getElementById('studentStatusFilter');
                if (statusEl) statusEl.value = '';
                dataTable.ajax.url('/api/registrar/student-records').load(function (json) {
                    const count = Array.isArray(json) ? json.length : 0;
                    setFeedback('Filter cleared. Showing all ' + count + ' record(s).', 'info');
                });
            });
        }

        setupDeleteRecordFlow(dataTable);
    });

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
})();
