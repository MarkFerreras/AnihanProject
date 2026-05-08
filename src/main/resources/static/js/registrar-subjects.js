/**
 * registrar-subjects.js — Subjects DataTable + Trainer Assignment
 */
(function () {
    'use strict';

    let subjectsTable;
    let trainers = [];
    let currentSubjectCode = null;

    const assignTrainerModal = new bootstrap.Modal(document.getElementById('assignTrainerModal'));

    $(document).ready(function () {
        loadTrainers();
        initTable();
        setupAssignTrainer();
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
                        return '<button class="btn btn-surface btn-sm assign-trainer-btn" ' +
                               'data-code="' + escapeHtml(data.subjectCode) + '" ' +
                               'data-name="' + escapeHtml(data.subjectName) + '" ' +
                               'data-trainer="' + (data.trainerId || '') + '">' +
                               'Assign Trainer</button>';
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
