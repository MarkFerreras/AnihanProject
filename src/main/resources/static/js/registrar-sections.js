/**
 * registrar-sections.js — Sections DataTable + Create / Edit / Delete / Manage Students
 */
(function () {
    'use strict';

    let sectionsTable;
    let currentSemester = '';
    let deleteSectionCode = null;
    let editSectionCode = null;
    let manageSectionCode = null;

    const createSectionModal = new bootstrap.Modal(document.getElementById('createSectionModal'));
    const editSectionModal   = new bootstrap.Modal(document.getElementById('editSectionModal'));
    const manageSectionModal = new bootstrap.Modal(document.getElementById('manageSectionModal'));
    const deleteSectionModal = new bootstrap.Modal(document.getElementById('deleteSectionConfirmModal'));

    $(document).ready(function () {
        loadCurrentSemester();
        setupCreateSection();
        setupEditSection();
        setupManageStudents();
        setupDeleteSection();
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
                initTable(currentSemester);
            },
            error: function () {
                currentSemester = String(new Date().getFullYear());
                $('#currentSemesterLabel').text('Semester: ' + currentSemester);
                initTable(currentSemester);
            }
        });

        $('#showAllSectionsToggle').on('change', function () {
            reloadTable(this.checked ? null : currentSemester);
        });
    }

    function initTable(semester) {
        sectionsTable = $('#sectionsTable').DataTable({
            ajax: {
                url: '/api/registrar/sections' + (semester ? '?semester=' + semester : ''),
                dataSrc: ''
            },
            columns: [
                { data: 'sectionCode' },
                { data: 'sectionName' },
                {
                    data: 'batchYear',
                    className: 'text-center',
                    render: function (data) { return data || '—'; }
                },
                {
                    data: null,
                    render: function (data) {
                        if (data.courseName) {
                            return escapeHtml(data.courseName) +
                                ' <span class="text-muted">(' + escapeHtml(data.courseCode) + ')</span>';
                        }
                        return '<span class="text-muted fst-italic">—</span>';
                    }
                },
                {
                    data: null,
                    orderable: false,
                    className: 'text-end',
                    render: function (data) {
                        const code = escapeHtml(data.sectionCode);
                        const name = escapeHtml(data.sectionName);
                        return '<div class="d-flex gap-1 justify-content-end">' +
                            '<button class="btn btn-surface btn-sm edit-section-btn" ' +
                                'data-code="' + code + '" data-name="' + name + '">Edit</button>' +
                            '<button class="btn btn-surface btn-sm manage-students-btn" ' +
                                'data-code="' + code + '" data-name="' + name + '">Manage Students</button>' +
                            '<button class="btn btn-danger-surface btn-sm delete-section-btn" ' +
                                'data-code="' + code + '" data-name="' + name + '">Delete</button>' +
                            '</div>';
                    }
                }
            ],
            order: [[2, 'desc'], [0, 'asc']],
            language: {
                emptyTable: 'No sections found.',
                zeroRecords: 'No matching sections.'
            }
        });

        $('#sectionsTable').on('click', '.edit-section-btn', function () {
            editSectionCode = $(this).data('code');
            $('#editSectionCodeDisplay').text(editSectionCode);
            $('#editSectionNameInput').val($(this).data('name'));
            hideAlert('editSectionAlert');
            editSectionModal.show();
        });

        $('#sectionsTable').on('click', '.manage-students-btn', function () {
            manageSectionCode = $(this).data('code');
            $('#manageSectionModalLabel').text('Manage Students — ' + $(this).data('name'));
            hideAlert('manageStudentAlert');
            $('#tab-current-students').tab('show');
            manageSectionModal.show();
            refreshCurrentStudents();
            loadFilterDropdowns();
            refreshEligibleStudents();
        });

        $('#sectionsTable').on('click', '.delete-section-btn', function () {
            deleteSectionCode = $(this).data('code');
            $('#deleteSectionName').text($(this).data('name') + ' (' + deleteSectionCode + ')');
            hideAlert('deleteSectionAlert');
            deleteSectionModal.show();
        });
    }

    function reloadTable(semester) {
        if (sectionsTable) {
            const url = '/api/registrar/sections' + (semester ? '?semester=' + semester : '');
            sectionsTable.ajax.url(url).load();
        }
    }

    // -------------------------------------------------------
    // Create Section
    // -------------------------------------------------------

    function setupCreateSection() {
        $('#createSectionModal').on('show.bs.modal', function () {
            hideAlert('createSectionAlert');
            $('#sectionCodeInput').val('');
            $('#sectionNameInput').val('');
            loadBatchesDropdown();
            loadCoursesDropdown();
        });

        $('#saveSectionBtn').on('click', function () {
            const payload = {
                sectionCode: $('#sectionCodeInput').val().trim(),
                sectionName: $('#sectionNameInput').val().trim(),
                batchCode: $('#sectionBatchSelect').val(),
                courseCode: $('#sectionCourseSelect').val()
            };

            if (!payload.sectionCode || !payload.sectionName || !payload.batchCode || !payload.courseCode) {
                showAlert('createSectionAlert', 'Please fill in all required fields.', 'danger');
                return;
            }

            $.ajax({
                url: '/api/registrar/sections',
                method: 'POST',
                contentType: 'application/json',
                data: JSON.stringify(payload),
                success: function () {
                    createSectionModal.hide();
                    sectionsTable.ajax.reload(null, false);
                },
                error: function (xhr) {
                    const msg = xhr.responseJSON?.message || 'Failed to create section.';
                    showAlert('createSectionAlert', msg, 'danger');
                }
            });
        });
    }

    function loadBatchesDropdown() {
        $.ajax({
            url: '/api/lookup/batches',
            method: 'GET',
            success: function (data) {
                const select = $('#sectionBatchSelect');
                select.find('option:not(:first)').remove();
                data.forEach(function (b) {
                    select.append('<option value="' + escapeHtml(b.code) + '">' +
                        escapeHtml(b.code) + ' (' + escapeHtml(b.name) + ')</option>');
                });
            }
        });
    }

    function loadCoursesDropdown() {
        $.ajax({
            url: '/api/lookup/courses',
            method: 'GET',
            success: function (data) {
                const select = $('#sectionCourseSelect');
                select.find('option:not(:first)').remove();
                data.forEach(function (c) {
                    select.append('<option value="' + escapeHtml(c.code) + '">' +
                        escapeHtml(c.name) + ' (' + escapeHtml(c.code) + ')</option>');
                });
            }
        });
    }

    // -------------------------------------------------------
    // Edit Section
    // -------------------------------------------------------

    function setupEditSection() {
        $('#saveEditSectionBtn').on('click', function () {
            const name = $('#editSectionNameInput').val().trim();
            if (!name) {
                showAlert('editSectionAlert', 'Section name is required.', 'danger');
                return;
            }

            $.ajax({
                url: '/api/registrar/sections/' + encodeURIComponent(editSectionCode),
                method: 'PUT',
                contentType: 'application/json',
                data: JSON.stringify({ sectionName: name }),
                success: function () {
                    editSectionModal.hide();
                    sectionsTable.ajax.reload(null, false);
                },
                error: function (xhr) {
                    const msg = xhr.responseJSON?.message || 'Failed to update section.';
                    showAlert('editSectionAlert', msg, 'danger');
                }
            });
        });
    }

    // -------------------------------------------------------
    // Manage Students
    // -------------------------------------------------------

    function setupManageStudents() {
        $('#eligibleBatchFilter, #eligibleCourseFilter').on('change', function () {
            refreshEligibleStudents();
        });
    }

    function refreshCurrentStudents() {
        const tbody = document.getElementById('currentStudentsBody');
        tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted">Loading...</td></tr>';

        $.ajax({
            url: '/api/registrar/sections/' + encodeURIComponent(manageSectionCode) + '/students',
            method: 'GET',
            success: function (students) {
                $('#currentStudentCount').text(students.length);
                if (students.length === 0) {
                    tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted">No students in this section.</td></tr>';
                    return;
                }
                tbody.innerHTML = students.map(function (s) {
                    return '<tr>' +
                        '<td>' + escapeHtml(s.studentId) + '</td>' +
                        '<td>' + escapeHtml(s.lastName) + '</td>' +
                        '<td>' + escapeHtml(s.firstName) + '</td>' +
                        '<td>' + escapeHtml(s.studentStatus) + '</td>' +
                        '<td class="text-end">' +
                            '<button class="btn btn-danger-surface btn-sm remove-student-btn" ' +
                                'data-id="' + escapeHtml(s.studentId) + '">' +
                                'Remove' +
                            '</button>' +
                        '</td>' +
                    '</tr>';
                }).join('');

                $(tbody).find('.remove-student-btn').on('click', function () {
                    const studentId = $(this).data('id');
                    $.ajax({
                        url: '/api/registrar/sections/' + encodeURIComponent(manageSectionCode) +
                             '/students/' + encodeURIComponent(studentId),
                        method: 'DELETE',
                        success: function () {
                            hideAlert('manageStudentAlert');
                            refreshCurrentStudents();
                            refreshEligibleStudents();
                            sectionsTable.ajax.reload(null, false);
                        },
                        error: function (xhr) {
                            const msg = xhr.responseJSON?.message || 'Failed to remove student.';
                            showAlert('manageStudentAlert', msg, 'danger');
                        }
                    });
                });
            },
            error: function () {
                tbody.innerHTML = '<tr><td colspan="5" class="text-center text-danger">Failed to load students.</td></tr>';
            }
        });
    }

    function refreshEligibleStudents() {
        const tbody = document.getElementById('eligibleStudentsBody');
        tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted">Loading...</td></tr>';

        const batchCode  = $('#eligibleBatchFilter').val();
        const courseCode = $('#eligibleCourseFilter').val();
        let url = '/api/registrar/sections/eligible-students';
        const params = [];
        if (batchCode)  params.push('batchCode='  + encodeURIComponent(batchCode));
        if (courseCode) params.push('courseCode=' + encodeURIComponent(courseCode));
        if (params.length) url += '?' + params.join('&');

        $.ajax({
            url: url,
            method: 'GET',
            success: function (students) {
                if (students.length === 0) {
                    tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted">No eligible students found.</td></tr>';
                    return;
                }
                tbody.innerHTML = students.map(function (s) {
                    return '<tr>' +
                        '<td>' + escapeHtml(s.studentId) + '</td>' +
                        '<td>' + escapeHtml(s.lastName) + '</td>' +
                        '<td>' + escapeHtml(s.firstName) + '</td>' +
                        '<td>' + escapeHtml(s.batchCode) + '</td>' +
                        '<td>' + escapeHtml(s.courseCode) + '</td>' +
                        '<td class="text-end">' +
                            '<button class="btn btn-surface btn-sm assign-student-btn" ' +
                                'data-id="' + escapeHtml(s.studentId) + '">' +
                                'Assign' +
                            '</button>' +
                        '</td>' +
                    '</tr>';
                }).join('');

                $(tbody).find('.assign-student-btn').on('click', function () {
                    const btn = $(this);
                    const studentId = btn.data('id');
                    btn.prop('disabled', true).text('Assigning...');

                    $.ajax({
                        url: '/api/registrar/sections/' + encodeURIComponent(manageSectionCode) + '/students',
                        method: 'POST',
                        contentType: 'application/json',
                        data: JSON.stringify({ studentIds: [studentId] }),
                        success: function (result) {
                            hideAlert('manageStudentAlert');
                            if (result.assignedCount > 0) {
                                refreshCurrentStudents();
                                refreshEligibleStudents();
                            } else {
                                const reason = (result.reasons && result.reasons[0]) || 'Student could not be assigned.';
                                showAlert('manageStudentAlert', reason, 'warning');
                                refreshEligibleStudents();
                            }
                        },
                        error: function (xhr) {
                            const msg = xhr.responseJSON?.message || 'Failed to assign student.';
                            showAlert('manageStudentAlert', msg, 'danger');
                            refreshEligibleStudents();
                        }
                    });
                });
            },
            error: function () {
                tbody.innerHTML = '<tr><td colspan="6" class="text-center text-danger">Failed to load eligible students.</td></tr>';
            }
        });
    }

    function loadFilterDropdowns() {
        $.ajax({
            url: '/api/lookup/batches',
            method: 'GET',
            success: function (data) {
                const select = $('#eligibleBatchFilter');
                select.find('option:not(:first)').remove();
                data.forEach(function (b) {
                    select.append('<option value="' + escapeHtml(b.code) + '">' +
                        escapeHtml(b.code) + '</option>');
                });
            }
        });

        $.ajax({
            url: '/api/lookup/courses',
            method: 'GET',
            success: function (data) {
                const select = $('#eligibleCourseFilter');
                select.find('option:not(:first)').remove();
                data.forEach(function (c) {
                    select.append('<option value="' + escapeHtml(c.code) + '">' +
                        escapeHtml(c.name) + '</option>');
                });
            }
        });
    }

    // -------------------------------------------------------
    // Delete Section
    // -------------------------------------------------------

    function setupDeleteSection() {
        $('#confirmDeleteSectionBtn').on('click', function () {
            $.ajax({
                url: '/api/registrar/sections/' + encodeURIComponent(deleteSectionCode),
                method: 'DELETE',
                success: function () {
                    deleteSectionModal.hide();
                    sectionsTable.ajax.reload(null, false);
                },
                error: function (xhr) {
                    const msg = xhr.responseJSON?.message || 'Failed to delete section. It may have linked classes.';
                    showAlert('deleteSectionAlert', msg, 'danger');
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
