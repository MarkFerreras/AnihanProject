/**
 * registrar-sections.js — Sections DataTable + Create / Delete
 */
(function () {
    'use strict';

    let sectionsTable;
    let currentSemester = '';
    let deleteSectionCode = null;

    const createSectionModal = new bootstrap.Modal(document.getElementById('createSectionModal'));
    const deleteSectionModal = new bootstrap.Modal(document.getElementById('deleteSectionConfirmModal'));

    $(document).ready(function () {
        loadCurrentSemester();
        setupCreateSection();
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
            if (this.checked) {
                reloadTable(null);
            } else {
                reloadTable(currentSemester);
            }
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
                    render: function (data) {
                        return data || '—';
                    }
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
                        return '<button class="btn btn-danger-surface btn-sm delete-section-btn" ' +
                               'data-code="' + escapeHtml(data.sectionCode) + '" ' +
                               'data-name="' + escapeHtml(data.sectionName) + '">Delete</button>';
                    }
                }
            ],
            order: [[2, 'desc'], [0, 'asc']],
            language: {
                emptyTable: 'No sections found.',
                zeroRecords: 'No matching sections.'
            }
        });

        // Delegate delete
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
