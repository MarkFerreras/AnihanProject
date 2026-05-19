$(function () {
    'use strict';

    let classesTable;
    let currentClassId = null;
    let currentGrades = [];
    let gradesLocked = false;
    const classRosterModal = new bootstrap.Modal(document.getElementById('classRosterModal'));

    classesTable = $('#classesTable').DataTable({
        ajax: {
            url: '/api/trainer/classes',
            dataSrc: '',
            error: function () {
                console.error('Failed to load classes.');
            }
        },
        columns: [
            { data: 'semester' },
            { data: 'sectionCode' },
            { data: 'sectionName' },
            { data: 'subjectCode' },
            { data: 'subjectName' },
            {
                data: 'courseName',
                render: val => val || '<em class="text-muted">Not Available</em>'
            },
            { data: 'enrolledCount' },
            {
                data: null,
                orderable: false,
                render: function () {
                    return '<button class="btn btn-sm btn-surface-secondary view-students-btn">Input Grades</button>';
                }
            }
        ],
        order: [[0, 'desc'], [2, 'asc'], [3, 'asc']],
        pageLength: 25,
        language: { emptyTable: 'No classes assigned.' }
    });

    $('#classesTable tbody').on('click', '.view-students-btn', function () {
        const row = classesTable.row($(this).closest('tr')).data();
        if (!row) return;
        openRoster(row.classId, row.subjectName, row.sectionName, row.semester);
    });

    function openRoster(classId, subjectName, sectionName, semester) {
        currentClassId = classId;
        $('#classRosterModalLabel').text('Grade Input — ' + subjectName);
        $('#classRosterSubtitle').text(sectionName + ' | Semester: ' + (semester || 'N/A'));
        $('#gradeInputAlert').addClass('d-none').attr('class', 'd-none');
        clearGradeInputTable();
        classRosterModal.show();

        $.ajax({
            url: '/api/trainer/classes/' + encodeURIComponent(classId) + '/grades',
            method: 'GET',
            success: function (response) {
                currentGrades = response.students || [];
                gradesLocked = response.locked || false;
                buildGradeInputTable();
                updateGradeActionButtons();
            },
            error: function (xhr) {
                const msg = xhr.responseJSON && xhr.responseJSON.message
                    ? xhr.responseJSON.message
                    : 'Failed to load grades.';
                showAlert(msg, 'danger');
            }
        });
    }

    function buildGradeInputTable() {
        const tbody = $('#gradeInputTable tbody');
        tbody.empty();
        currentGrades.forEach(function (student) {
            const row = $('<tr>');
            row.append($('<td>').text(student.studentId));
            row.append($('<td>').text(student.lastName || '—'));
            row.append($('<td>').text(student.firstName || '—'));
            row.append($('<td>').append(
                $('<input>').attr('type', 'number').attr('step', '0.01').attr('min', '1').attr('max', '5')
                    .attr('placeholder', '1.0–5.0').attr('data-field', 'midtermGrade')
                    .addClass('form-control form-control-sm grade-input').val(student.midtermGrade || '')
                    .prop('disabled', gradesLocked)
            ));
            row.append($('<td>').append(
                $('<input>').attr('type', 'number').attr('step', '0.01').attr('min', '1').attr('max', '5')
                    .attr('placeholder', '1.0–5.0').attr('data-field', 'finalsGrade')
                    .addClass('form-control form-control-sm grade-input').val(student.finalsGrade || '')
                    .prop('disabled', gradesLocked)
            ));
            row.append($('<td>').append(
                $('<input>').attr('type', 'number').attr('step', '0.01').attr('min', '1').attr('max', '5')
                    .attr('placeholder', '1.0–5.0 (opt)').attr('data-field', 'reExamGrade')
                    .addClass('form-control form-control-sm grade-input').val(student.reExamGrade || '')
                    .prop('disabled', gradesLocked)
            ));
            row.append($('<td>').append(
                $('<input>').attr('type', 'number').attr('step', '0.01').attr('min', '0').attr('max', '9.99')
                    .attr('placeholder', '0–9.99 (opt)').attr('data-field', 'hoursStudied')
                    .addClass('form-control form-control-sm grade-input').val(student.hoursStudied || '')
                    .prop('disabled', gradesLocked)
            ));
            row.append($('<td>').append(
                $('<input>').attr('type', 'text').attr('placeholder', 'Max 255 chars (opt)')
                    .attr('data-field', 'remarks').attr('maxlength', '255')
                    .addClass('form-control form-control-sm grade-input').val(student.remarks || '')
                    .prop('disabled', gradesLocked)
            ));
            row.data('studentId', student.studentId);
            tbody.append(row);
        });
    }

    function clearGradeInputTable() {
        $('#gradeInputTable tbody').empty();
    }

    function updateGradeActionButtons() {
        if (gradesLocked) {
            $('#lockGradesBtn').prop('disabled', true).addClass('disabled');
            $('#unlockGradesBtn').prop('disabled', false).removeClass('disabled');
            $('#saveGradesBtn').prop('disabled', true).addClass('disabled');
        } else {
            $('#lockGradesBtn').prop('disabled', false).removeClass('disabled');
            $('#unlockGradesBtn').prop('disabled', true).addClass('disabled');
            $('#saveGradesBtn').prop('disabled', false).removeClass('disabled');
        }
    }

    function collectGradeUpdates() {
        const updates = [];
        $('#gradeInputTable tbody tr').each(function () {
            const row = $(this);
            const studentId = row.data('studentId');
            const midtermGrade = row.find('[data-field="midtermGrade"]').val();
            const finalsGrade = row.find('[data-field="finalsGrade"]').val();
            const reExamGrade = row.find('[data-field="reExamGrade"]').val();
            const hoursStudied = row.find('[data-field="hoursStudied"]').val();
            const remarks = row.find('[data-field="remarks"]').val();

            updates.push({
                studentId: studentId,
                midtermGrade: midtermGrade ? parseFloat(midtermGrade) : null,
                finalsGrade: finalsGrade ? parseFloat(finalsGrade) : null,
                reExamGrade: reExamGrade ? parseFloat(reExamGrade) : null,
                hoursStudied: hoursStudied ? parseFloat(hoursStudied) : null,
                remarks: remarks || null
            });
        });
        return updates;
    }

    function saveGrades() {
        const updates = collectGradeUpdates();
        $.ajax({
            url: '/api/trainer/classes/' + encodeURIComponent(currentClassId) + '/grades',
            method: 'PUT',
            contentType: 'application/json',
            data: JSON.stringify(updates),
            success: function () {
                showAlert('Grades saved successfully.', 'success');
                setTimeout(function () {
                    classRosterModal.hide();
                    classesTable.ajax.reload();
                }, 1500);
            },
            error: function (xhr) {
                const msg = xhr.responseJSON && xhr.responseJSON.message
                    ? xhr.responseJSON.message
                    : 'Failed to save grades.';
                showAlert(msg, 'danger');
            }
        });
    }

    function lockGrades() {
        $.ajax({
            url: '/api/trainer/classes/' + encodeURIComponent(currentClassId) + '/grades/lock',
            method: 'POST',
            success: function () {
                gradesLocked = true;
                updateGradeActionButtons();
                $('#gradeInputTable tbody tr').find('.grade-input').prop('disabled', true);
                showAlert('Grades locked successfully.', 'success');
            },
            error: function (xhr) {
                const msg = xhr.responseJSON && xhr.responseJSON.message
                    ? xhr.responseJSON.message
                    : 'Failed to lock grades.';
                showAlert(msg, 'danger');
            }
        });
    }

    function unlockGrades() {
        $.ajax({
            url: '/api/trainer/classes/' + encodeURIComponent(currentClassId) + '/grades/unlock',
            method: 'POST',
            success: function () {
                gradesLocked = false;
                updateGradeActionButtons();
                $('#gradeInputTable tbody tr').find('.grade-input').prop('disabled', false);
                showAlert('Grades unlocked successfully.', 'success');
            },
            error: function (xhr) {
                const msg = xhr.responseJSON && xhr.responseJSON.message
                    ? xhr.responseJSON.message
                    : 'Failed to unlock grades.';
                showAlert(msg, 'danger');
            }
        });
    }

    function showAlert(message, type) {
        const alertElement = $('#gradeInputAlert');
        alertElement.removeClass('d-none alert-danger alert-success alert-warning alert-info');
        alertElement.addClass('alert-' + type);
        alertElement.text(message);
    }

    $('#saveGradesBtn').on('click', saveGrades);
    $('#lockGradesBtn').on('click', lockGrades);
    $('#unlockGradesBtn').on('click', unlockGrades);

    $('#classRosterModal').on('shown.bs.modal', function () {
        // Modal shown
    });
});
