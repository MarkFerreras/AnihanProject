/**
 * registrar-subjects.js — Subjects DataTable + Create / Edit / Delete + Trainer Assignment
 */
(function () {
    'use strict';

    let subjectsTable;
    let trainers = [];
    let qualifications = [];
    let currentSubjectCode = null;

    let createSubjectModal = null;
    let editSubjectModal = null;
    let editSubjectCurrentCode = null;
    let deleteSubjectModal = null;
    let deleteSubjectCurrentCode = null;

    const assignTrainerModal = new bootstrap.Modal(document.getElementById('assignTrainerModal'));

    $(document).ready(function () {
        loadTrainers();
        initTable();
        setupAssignTrainer();
        // CRUD modals — must be called after initTable() so subjectsTable is defined
        setupCreateSubject(subjectsTable);
        setupEditSubject(subjectsTable);
        setupDeleteSubject(subjectsTable);
    });

    function initTable() {
        subjectsTable = $('#subjectsTable').DataTable({
            ajax: {
                url: '/api/registrar/subjects',
                dataSrc: ''
            },
            columns: [
                { data: 'subjectCode' },
                { data: 'subjectName' },
                {
                    data: 'qualificationName',
                    render: function (data) {
                        return data || '<span class="text-muted fst-italic">Not Available</span>';
                    }
                },
                { data: 'units', className: 'text-center' },
                {
                    data: 'trainerName',
                    render: function (data) {
                        if (data) {
                            return '<span class="status-badge status-badge-active">' + escapeHtml(data) + '</span>';
                        }
                        return '<span class="text-muted fst-italic">Unassigned</span>';
                    }
                },
                {
                    data: null,
                    orderable: false,
                    className: 'text-end',
                    render: function (data) {
                        return (
                            '<button class="btn btn-surface btn-sm edit-subject-btn me-1" ' +
                            'data-code="' + escapeHtml(data.subjectCode) + '">Edit</button>' +
                            '<button class="btn btn-surface btn-sm assign-trainer-btn me-1" ' +
                            'data-code="' + escapeHtml(data.subjectCode) + '" ' +
                            'data-name="' + escapeHtml(data.subjectName) + '" ' +
                            'data-trainer="' + (data.trainerId || '') + '">Assign Trainer</button>' +
                            '<button class="btn btn-sm btn-outline-danger delete-subject-btn" ' +
                            'data-code="' + escapeHtml(data.subjectCode) + '" ' +
                            'data-name="' + escapeHtml(data.subjectName) + '">Delete</button>'
                        );
                    }
                }
            ],
            order: [[0, 'asc']],
            language: {
                emptyTable: 'No subjects found.',
                zeroRecords: 'No matching subjects.'
            }
        });

        // Delegate click on assign buttons
        $('#subjectsTable').on('click', '.assign-trainer-btn', function () {
            currentSubjectCode = $(this).data('code');
            const subjectName = $(this).data('name');
            const trainerId = $(this).data('trainer');

            $('#assignTrainerSubjectName').text(subjectName);
            $('#trainerSelect').val(trainerId || '');
            hideAlert('assignTrainerAlert');
            assignTrainerModal.show();
        });
    }

    function loadTrainers() {
        $.ajax({
            url: '/api/registrar/trainers',
            method: 'GET',
            success: function (data) {
                trainers = data;
                const select = $('#trainerSelect');
                select.find('option:not(:first)').remove();
                trainers.forEach(function (t) {
                    select.append('<option value="' + t.userId + '">' + escapeHtml(t.fullName) + '</option>');
                });
            }
        });
    }

    function setupAssignTrainer() {
        $('#saveTrainerBtn').on('click', function () {
            const trainerId = $('#trainerSelect').val();
            const payload = { trainerId: trainerId ? parseInt(trainerId) : null };

            $.ajax({
                url: '/api/registrar/subjects/' + encodeURIComponent(currentSubjectCode) + '/trainer',
                method: 'PUT',
                contentType: 'application/json',
                data: JSON.stringify(payload),
                success: function () {
                    assignTrainerModal.hide();
                    subjectsTable.ajax.reload(null, false);
                },
                error: function (xhr) {
                    const msg = xhr.responseJSON?.message || 'Failed to assign trainer.';
                    showAlert('assignTrainerAlert', msg, 'danger');
                }
            });
        });
    }

    // -------------------------------------------------------
    // Qualifications dropdown loader (shared by Create + Edit)
    // -------------------------------------------------------

    function loadQualificationsDropdown(selectId) {
        const $sel = $('#' + selectId);
        $sel.empty().append('<option value="">-- Select Qualification --</option>');
        return $.ajax({
            url: '/api/registrar/qualifications',
            method: 'GET'
        }).done(function (data) {
            qualifications = data;
            data.forEach(function (q) {
                $sel.append(
                    '<option value="' + q.qualificationCode + '">' +
                    escapeHtml(q.qualificationName) +
                    '</option>'
                );
            });
        });
    }

    // -------------------------------------------------------
    // Create Subject
    // -------------------------------------------------------

    function setupCreateSubject(dataTable) {
        createSubjectModal = new bootstrap.Modal(document.getElementById('createSubjectModal'));

        $('#createSubjectModal').on('show.bs.modal', function () {
            hideAlert('createSubjectAlert');
            $('#createSubjectCode').val('');
            $('#createSubjectName').val('');
            $('#createSubjectUnits').val(3);
            loadQualificationsDropdown('createSubjectQualification');
        });

        $('#saveCreateSubjectBtn').on('click', function () {
            const payload = {
                subjectCode: $('#createSubjectCode').val().trim(),
                subjectName: $('#createSubjectName').val().trim(),
                qualificationCode: parseInt($('#createSubjectQualification').val(), 10) || null,
                units: parseInt($('#createSubjectUnits').val(), 10) || null
            };

            if (!payload.subjectCode || !payload.subjectName || !payload.qualificationCode || !payload.units) {
                showAlert('createSubjectAlert', 'Please fill in all required fields.', 'danger');
                return;
            }

            $.ajax({
                url: '/api/registrar/subjects',
                method: 'POST',
                contentType: 'application/json',
                data: JSON.stringify(payload),
                success: function () {
                    createSubjectModal.hide();
                    dataTable.ajax.reload(null, false);
                },
                error: function (xhr) {
                    const msg = xhr.responseJSON?.message || 'Failed to create subject.';
                    showAlert('createSubjectAlert', msg, 'danger');
                }
            });
        });
    }

    // -------------------------------------------------------
    // Edit Subject
    // -------------------------------------------------------

    function setupEditSubject(dataTable) {
        editSubjectModal = new bootstrap.Modal(document.getElementById('editSubjectModal'));

        $('#subjectsTable').on('click', '.edit-subject-btn', function () {
            const code = $(this).data('code');
            editSubjectCurrentCode = code;
            hideAlert('editSubjectAlert');

            const rowData = dataTable.rows().data().toArray()
                .find(function (r) { return r.subjectCode === code; });
            if (!rowData) return;

            $('#editSubjectCode').val(rowData.subjectCode);
            $('#editSubjectName').val(rowData.subjectName);
            $('#editSubjectUnits').val(rowData.units);

            loadQualificationsDropdown('editSubjectQualification').done(function () {
                const matching = qualifications
                    .find(function (q) { return q.qualificationName === rowData.qualificationName; });
                if (matching) {
                    $('#editSubjectQualification').val(matching.qualificationCode);
                }
            });

            editSubjectModal.show();
        });

        $('#saveEditSubjectBtn').on('click', function () {
            if (!editSubjectCurrentCode) return;

            const payload = {
                subjectName: $('#editSubjectName').val().trim(),
                qualificationCode: parseInt($('#editSubjectQualification').val(), 10) || null,
                units: parseInt($('#editSubjectUnits').val(), 10) || null
            };

            if (!payload.subjectName || !payload.qualificationCode || !payload.units) {
                showAlert('editSubjectAlert', 'Please fill in all required fields.', 'danger');
                return;
            }

            $.ajax({
                url: '/api/registrar/subjects/' + encodeURIComponent(editSubjectCurrentCode),
                method: 'PUT',
                contentType: 'application/json',
                data: JSON.stringify(payload),
                success: function () {
                    editSubjectModal.hide();
                    dataTable.ajax.reload(null, false);
                },
                error: function (xhr) {
                    const msg = xhr.responseJSON?.message || 'Failed to update subject.';
                    showAlert('editSubjectAlert', msg, 'danger');
                }
            });
        });
    }

    // -------------------------------------------------------
    // Delete Subject (strict type-to-confirm)
    // -------------------------------------------------------

    function setupDeleteSubject(dataTable) {
        deleteSubjectModal = new bootstrap.Modal(document.getElementById('deleteSubjectConfirmModal'));
        const input = document.getElementById('deleteSubjectConfirmInput');
        const btn = document.getElementById('confirmDeleteSubjectBtn');
        const identifierEl = document.getElementById('deleteSubjectIdentifier');
        const resultAlert = document.getElementById('deleteSubjectResultAlert');
        const modalEl = document.getElementById('deleteSubjectConfirmModal');

        $('#subjectsTable').on('click', '.delete-subject-btn', function () {
            deleteSubjectCurrentCode = $(this).data('code');
            const name = $(this).data('name');
            identifierEl.textContent = name + ' (' + deleteSubjectCurrentCode + ')';
            input.value = '';
            btn.disabled = true;
            resultAlert.classList.add('d-none');
            resultAlert.textContent = '';
            deleteSubjectModal.show();
        });

        input.addEventListener('input', function () {
            btn.disabled = input.value.trim().toLowerCase() !== 'delete';
        });

        btn.addEventListener('click', async function () {
            if (!deleteSubjectCurrentCode) return;
            if (input.value.trim().toLowerCase() !== 'delete') return;

            btn.disabled = true;
            const originalLabel = btn.textContent;
            btn.textContent = 'Deleting...';

            try {
                const res = await fetch('/api/registrar/subjects/' + encodeURIComponent(deleteSubjectCurrentCode), {
                    method: 'DELETE',
                    credentials: 'same-origin'
                });
                if (!res.ok) {
                    let msg = 'Delete failed.';
                    try {
                        const body = await res.json();
                        if (body && body.message) msg = body.message;
                    } catch (e) { /* ignore */ }
                    resultAlert.className = 'alert alert-danger mt-3';
                    resultAlert.textContent = msg;
                    resultAlert.classList.remove('d-none');
                    btn.disabled = false;
                    btn.textContent = originalLabel;
                    return;
                }
                resultAlert.className = 'alert alert-success mt-3';
                resultAlert.textContent = 'Subject deleted successfully.';
                resultAlert.classList.remove('d-none');
                window.setTimeout(function () {
                    deleteSubjectModal.hide();
                    dataTable.ajax.reload(null, false);
                }, 900);
            } catch (err) {
                resultAlert.className = 'alert alert-danger mt-3';
                resultAlert.textContent = 'Network error. Could not delete the subject.';
                resultAlert.classList.remove('d-none');
                btn.disabled = false;
                btn.textContent = originalLabel;
            }
        });

        modalEl.addEventListener('hidden.bs.modal', function () {
            input.value = '';
            btn.disabled = true;
            btn.textContent = 'Permanently Delete';
            resultAlert.classList.add('d-none');
            resultAlert.textContent = '';
        });
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------

    function escapeHtml(str) {
        if (!str) return '';
        const div = document.createElement('div');
        div.appendChild(document.createTextNode(str));
        return div.innerHTML;
    }

    function showAlert(id, message, type) {
        const el = document.getElementById(id);
        el.className = 'alert alert-' + type;
        el.textContent = message;
        el.classList.remove('d-none');
    }

    function hideAlert(id) {
        const el = document.getElementById(id);
        el.className = 'alert d-none';
        el.textContent = '';
    }
})();
