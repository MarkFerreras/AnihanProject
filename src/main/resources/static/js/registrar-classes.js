/**
 * registrar-classes.js — Classes DataTable + Create Class + Enrollment Management
 */
(function () {
    'use strict';

    let classesTable;
    let currentSemester = '';
    let currentClassId = null;
    let currentEditClassData = null;

    const createClassModal = new bootstrap.Modal(document.getElementById('createClassModal'));
    const editClassModal = new bootstrap.Modal(document.getElementById('editClassModal'));
    const enrollStudentModal = new bootstrap.Modal(document.getElementById('enrollStudentModal'));

    $(document).ready(function () {
        loadCurrentSemester();
        setupCreateClass();
        setupEditClass();
        setupEnrollment();
    });

    // -------------------------------------------------------
    // Current Semester + Table Init
    // -------------------------------------------------------

    function loadCurrentSemester() {
        $.ajax({
            url: '/api/registrar/classes/current-semester',
            method: 'GET',
            success: function (data) {
                currentSemester = data.semester;
                $('#currentSemesterLabel').text('Semester: ' + currentSemester);
                $('#classSemesterInput').val(currentSemester);
                initTable(currentSemester);
            },
            error: function () {
                currentSemester = String(new Date().getFullYear());
                $('#currentSemesterLabel').text('Semester: ' + currentSemester);
                $('#classSemesterInput').val(currentSemester);
                initTable(currentSemester);
            }
        });

        // Toggle switch
        $('#showAllClassesToggle').on('change', function () {
            if (this.checked) {
                reloadTable(null);
            } else {
                reloadTable(currentSemester);
            }
        });
    }

    function initTable(semester) {
        classesTable = $('#classesTable').DataTable({
            ajax: {
                url: '/api/registrar/classes' + (semester ? '?semester=' + semester : ''),
                dataSrc: ''
            },
            columns: [
                { data: 'classId' },
                {
                    data: null,
                    render: function (data) {
                        return escapeHtml(data.sectionName) +
                            ' <span class="text-muted">(' + escapeHtml(data.sectionCode) + ')</span>';
                    }
                },
                {
                    data: null,
                    render: function (data) {
                        return escapeHtml(data.subjectName) +
                            ' <span class="text-muted">(' + escapeHtml(data.subjectCode) + ')</span>';
                    }
                },
                {
                    data: 'trainerName',
                    render: function (data) {
                        if (data) {
                            return '<span class="status-badge status-badge-active">' + escapeHtml(data) + '</span>';
                        }
                        return '<span class="text-muted fst-italic">Unassigned</span>';
                    }
                },
                { data: 'semester', className: 'text-center' },
                {
                    data: 'enrolledCount',
                    className: 'text-center',
                    render: function (data) {
                        return '<span class="status-badge status-badge-graduated">' + data + '</span>';
                    }
                },
                {
                    data: null,
                    orderable: false,
                    className: 'text-end',
                    render: function (data) {
                        return '<div class="d-flex gap-2 justify-content-end">' +
                            '<button class="btn btn-surface btn-sm edit-class-btn" ' +
                                'data-id="' + data.classId + '" ' +
                                'data-section="' + escapeHtml(data.sectionName) + ' (' + escapeHtml(data.sectionCode) + ')" ' +
                                'data-subject="' + escapeHtml(data.subjectName) + ' (' + escapeHtml(data.subjectCode) + ')" ' +
                                'data-semester="' + escapeHtml(data.semester) + '" ' +
                                'data-trainer-id="' + (data.trainerId || '') + '">' +
                                'Edit Trainer</button>' +
                            '<button class="btn btn-surface btn-sm enroll-btn" data-id="' + data.classId + '">Manage Students</button>' +
                            '</div>';
                    }
                }
            ],
            order: [[0, 'desc']],
            language: {
                emptyTable: 'No classes found.',
                zeroRecords: 'No matching classes.'
            }
        });

        // Delegate click for edit trainer
        $('#classesTable').on('click', '.edit-class-btn', function () {
            currentEditClassData = {
                classId: parseInt($(this).data('id')),
                section: $(this).data('section'),
                subject: $(this).data('subject'),
                semester: $(this).data('semester'),
                trainerId: $(this).data('trainer-id') || null
            };
            openEditClassModal(currentEditClassData);
        });

        // Delegate click for enrollment
        $('#classesTable').on('click', '.enroll-btn', function () {
            currentClassId = parseInt($(this).data('id'));
            openEnrollmentModal(currentClassId);
        });
    }

    function reloadTable(semester) {
        if (classesTable) {
            const url = '/api/registrar/classes' + (semester ? '?semester=' + semester : '');
            classesTable.ajax.url(url).load();
        }
    }

    // -------------------------------------------------------
    // Create Class
    // -------------------------------------------------------

    function setupCreateClass() {
        // Load dropdowns when modal opens
        $('#createClassModal').on('show.bs.modal', function () {
            hideAlert('createClassAlert');
            loadSectionsDropdown();
            loadSubjectsDropdown();
            loadTrainersDropdown('classTrainerSelect');
            $('#classSemesterInput').val(currentSemester);
        });

        // When subject changes, auto-select subject's default trainer
        $('#classSubjectSelect').on('change', function () {
            const selected = $(this).find(':selected');
            const defaultTrainerId = selected.data('trainer');
            if (defaultTrainerId) {
                $('#classTrainerSelect').val(defaultTrainerId);
            }
        });

        $('#saveClassBtn').on('click', function () {
            const payload = {
                sectionCode: $('#classSectionSelect').val(),
                subjectCode: $('#classSubjectSelect').val(),
                trainerId: $('#classTrainerSelect').val() ? parseInt($('#classTrainerSelect').val()) : null,
                semester: $('#classSemesterInput').val()
            };

            if (!payload.sectionCode || !payload.subjectCode || !payload.semester) {
                showAlert('createClassAlert', 'Please fill in all required fields.', 'danger');
                return;
            }

            $.ajax({
                url: '/api/registrar/classes',
                method: 'POST',
                contentType: 'application/json',
                data: JSON.stringify(payload),
                success: function () {
                    createClassModal.hide();
                    classesTable.ajax.reload(null, false);
                },
                error: function (xhr) {
                    const msg = xhr.responseJSON?.message || 'Failed to create class.';
                    showAlert('createClassAlert', msg, 'danger');
                }
            });
        });
    }

    // -------------------------------------------------------
    // Edit Class Trainer
    // -------------------------------------------------------

    function setupEditClass() {
        $('#editClassModal').on('hidden.bs.modal', function () {
            hideAlert('editClassAlert');
            currentEditClassData = null;
        });

        $('#saveEditClassBtn').on('click', function () {
            if (!currentEditClassData) return;

            const trainerId = $('#editClassTrainerSelect').val()
                ? parseInt($('#editClassTrainerSelect').val())
                : null;

            $.ajax({
                url: '/api/registrar/classes/' + currentEditClassData.classId + '/trainer',
                method: 'PUT',
                contentType: 'application/json',
                data: JSON.stringify({ trainerId: trainerId }),
                success: function () {
                    editClassModal.hide();
                    classesTable.ajax.reload(null, false);
                },
                error: function (xhr) {
                    const msg = xhr.responseJSON?.message || 'Failed to update trainer.';
                    showAlert('editClassAlert', msg, 'danger');
                }
            });
        });
    }

    function openEditClassModal(data) {
        hideAlert('editClassAlert');
        $('#editClassSection').text(data.section || '—');
        $('#editClassSubject').text(data.subject || '—');
        $('#editClassSemester').text(data.semester || '—');

        loadTrainersDropdown('editClassTrainerSelect').done(function () {
            $('#editClassTrainerSelect').val(data.trainerId ? String(data.trainerId) : '');
        });

        editClassModal.show();
    }

    function loadSectionsDropdown() {
        $.ajax({
            url: '/api/registrar/sections?semester=' + currentSemester,
            method: 'GET',
            success: function (data) {
                const select = $('#classSectionSelect');
                select.find('option:not(:first)').remove();
                data.forEach(function (s) {
                    select.append('<option value="' + escapeHtml(s.sectionCode) + '">' +
                        escapeHtml(s.sectionName) + ' (' + escapeHtml(s.sectionCode) + ')</option>');
                });
            }
        });
    }

    function loadSubjectsDropdown() {
        $.ajax({
            url: '/api/registrar/subjects',
            method: 'GET',
            success: function (data) {
                const select = $('#classSubjectSelect');
                select.find('option:not(:first)').remove();
                data.forEach(function (s) {
                    select.append('<option value="' + escapeHtml(s.subjectCode) + '" data-trainer="' +
                        (s.trainerId || '') + '">' +
                        escapeHtml(s.subjectName) + ' (' + escapeHtml(s.subjectCode) + ')</option>');
                });
            }
        });
    }

    function loadTrainersDropdown(selectId) {
        return $.ajax({
            url: '/api/registrar/trainers',
            method: 'GET',
            success: function (data) {
                const select = $('#' + selectId);
                select.find('option:not(:first)').remove();
                data.forEach(function (t) {
                    select.append('<option value="' + t.userId + '">' + escapeHtml(t.fullName) + '</option>');
                });
            }
        });
    }

    // -------------------------------------------------------
    // Enrollment Management
    // -------------------------------------------------------

    function setupEnrollment() {
        $('#enrollStudentBtn').on('click', function () {
            const studentId = $('#eligibleStudentSelect').val();
            if (!studentId) return;

            $.ajax({
                url: '/api/registrar/classes/enroll',
                method: 'POST',
                contentType: 'application/json',
                data: JSON.stringify({ classId: currentClassId, studentId: studentId }),
                success: function () {
                    refreshEnrollmentData(currentClassId);
                    classesTable.ajax.reload(null, false);
                },
                error: function (xhr) {
                    const msg = xhr.responseJSON?.message || 'Failed to enroll student.';
                    showAlert('enrollAlert', msg, 'danger');
                }
            });
        });

        $('#enrollWholeSectionBtn').on('click', function () {
            const btn = $(this);
            btn.prop('disabled', true).text('Enrolling...');
            hideAlert('enrollSectionAlert');

            $.ajax({
                url: '/api/registrar/classes/' + currentClassId + '/enroll-section',
                method: 'POST',
                success: function (result) {
                    const msg = 'Enrolled: ' + result.enrolledCount +
                        ' | Already enrolled: ' + result.skippedAlreadyEnrolled +
                        ' | Ineligible: ' + result.skippedIneligible +
                        ' | Total considered: ' + result.totalConsidered;
                    showAlert('enrollSectionAlert', msg, 'success');
                    refreshEnrollmentData(currentClassId);
                    classesTable.ajax.reload(null, false);
                },
                error: function (xhr) {
                    const msg = xhr.responseJSON?.message || 'Failed to bulk-enroll section.';
                    showAlert('enrollSectionAlert', msg, 'danger');
                },
                complete: function () {
                    btn.prop('disabled', false).text('Enroll Whole Section');
                }
            });
        });
    }

    function openEnrollmentModal(classId) {
        hideAlert('enrollAlert');
        hideAlert('enrollSectionAlert');
        refreshEnrollmentData(classId);
        enrollStudentModal.show();
    }

    function refreshEnrollmentData(classId) {
        // Load enrolled students
        $.ajax({
            url: '/api/registrar/classes/' + classId + '/enrollments',
            method: 'GET',
            success: function (data) {
                const tbody = $('#enrolledTable tbody');
                tbody.empty();
                if (data.length === 0) {
                    tbody.append('<tr><td colspan="4" class="text-muted text-center">No students enrolled.</td></tr>');
                } else {
                    data.forEach(function (e) {
                        tbody.append(
                            '<tr>' +
                            '<td>' + escapeHtml(e.studentId) + '</td>' +
                            '<td>' + escapeHtml(e.lastName) + '</td>' +
                            '<td>' + escapeHtml(e.firstName) + '</td>' +
                            '<td class="text-end"><button class="btn btn-danger-surface btn-sm unenroll-btn" data-id="' + e.enrollmentId + '">Remove</button></td>' +
                            '</tr>'
                        );
                    });
                }
            }
        });

        // Load eligible students
        $.ajax({
            url: '/api/registrar/classes/' + classId + '/eligible-students',
            method: 'GET',
            success: function (data) {
                const select = $('#eligibleStudentSelect');
                select.find('option:not(:first)').remove();
                data.forEach(function (s) {
                    select.append('<option value="' + escapeHtml(s.studentId) + '">' +
                        escapeHtml(s.lastName) + ', ' + escapeHtml(s.firstName) +
                        ' (' + escapeHtml(s.studentId) + ')</option>');
                });
            }
        });
    }

    // Delegate unenroll clicks
    $(document).on('click', '.unenroll-btn', function () {
        const enrollmentId = $(this).data('id');
        $.ajax({
            url: '/api/registrar/enrollments/' + enrollmentId,
            method: 'DELETE',
            success: function () {
                refreshEnrollmentData(currentClassId);
                classesTable.ajax.reload(null, false);
            },
            error: function (xhr) {
                const msg = xhr.responseJSON?.message || 'Failed to remove student.';
                showAlert('enrollAlert', msg, 'danger');
            }
        });
    });

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
