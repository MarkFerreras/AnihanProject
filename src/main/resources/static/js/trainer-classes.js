$(function () {
    'use strict';

    let classesTable;
    let currentClassId = null;
    let currentGrades = [];
    let gradesLocked = false;
    const classRosterModal = new bootstrap.Modal(document.getElementById('classRosterModal'));

    const STATUS_OPTIONS = [
        { code: '', label: '—' },
        { code: 'C', label: 'Complete' },
        { code: 'FA', label: 'Failure Due to Absences' },
        { code: 'INC', label: 'Incomplete' },
        { code: 'D', label: 'Dropped' }
    ];

    // Mirror of GradeEquivalent.toEquivalent — bands read as ">= lower bound".
    function toEquivalent(pct) {
        if (pct === '' || pct === null || pct === undefined || isNaN(pct)) return null;
        const p = parseFloat(pct);
        if (p < 0 || p > 100) return null;
        if (p >= 99) return '1.00';
        if (p >= 96) return '1.25';
        if (p >= 93) return '1.50';
        if (p >= 90) return '1.75';
        if (p >= 87) return '2.00';
        if (p >= 84) return '2.25';
        if (p >= 81) return '2.50';
        if (p >= 78) return '2.75';
        if (p >= 75) return '3.00';
        if (p >= 70) return '4.00';
        return '5.00';
    }

    function isFailingEquiv(equiv) {
        return equiv !== null && parseFloat(equiv) > 3.0;
    }

    function remarkLabel(finalEquiv, reExamEquiv, statusCode) {
        if (statusCode) {
            if (statusCode === 'C') return 'Competent';
            if (statusCode === 'FA') return 'Not Competent';
            return '';
        }
        if (finalEquiv === null) return '';
        const effective = (reExamEquiv !== null && isFailingEquiv(finalEquiv)) ? reExamEquiv : finalEquiv;
        return parseFloat(effective) <= 3.0 ? 'Competent' : 'Not Competent';
    }

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
            { data: 'courseName', render: val => val || '<em class="text-muted">Not Available</em>' },
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
        $('#gradeInputTable tbody').empty();
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
                showAlert(gradeErrorMessage(xhr, 'Failed to load grades.'), 'danger');
            }
        });
    }

    function buildGradeInputTable() {
        const tbody = $('#gradeInputTable tbody');
        tbody.empty();

        currentGrades.forEach(function (student) {
            const row = $('<tr>').data('studentId', student.studentId);
            row.append($('<td>').text(student.studentId));
            row.append($('<td>').text(student.lastName || '—'));
            row.append($('<td>').text(student.firstName || '—'));

            const finalPctInput = $('<input>')
                .attr({ type: 'number', step: '0.01', min: '0', max: '100', placeholder: '0–100' })
                .addClass('form-control form-control-sm grade-final-pct')
                .val(student.finalPercentage != null ? student.finalPercentage : '')
                .prop('disabled', gradesLocked);
            row.append($('<td>').append(finalPctInput));

            const statusSelect = $('<select>')
                .addClass('form-select form-select-sm grade-status')
                .prop('disabled', gradesLocked);
            STATUS_OPTIONS.forEach(function (o) {
                statusSelect.append($('<option>').val(o.code).text(o.label));
            });
            statusSelect.val(student.gradeStatus || '');
            row.append($('<td>').append(statusSelect));

            row.append($('<td>').append($('<span>').addClass('grade-equiv text-muted')));

            const reExamPctInput = $('<input>')
                .attr({ type: 'number', step: '0.01', min: '0', max: '100', placeholder: '0–100' })
                .addClass('form-control form-control-sm grade-reexam-pct')
                .val(student.reExamPercentage != null ? student.reExamPercentage : '')
                .prop('disabled', gradesLocked);
            row.append($('<td>').append(reExamPctInput));

            row.append($('<td>').append($('<span>').addClass('grade-reexam-equiv text-muted')));
            row.append($('<td>').append($('<span>').addClass('grade-remark')));

            const hoursInput = $('<input>')
                .attr({ type: 'number', step: '0.01', min: '0', max: '100', placeholder: '0–100' })
                .addClass('form-control form-control-sm grade-hours')
                .val(student.hoursRendered != null ? student.hoursRendered : '')
                .prop('disabled', gradesLocked);
            row.append($('<td>').append(hoursInput));

            tbody.append(row);
            refreshRow(row);
        });

        tbody.off('input.grade change.grade')
            .on('input.grade change.grade', 'input, select', function () {
                refreshRow($(this).closest('tr'));
            });
    }

    // Recompute the derived cells for one row and enforce percentage <-> status exclusivity.
    function refreshRow(row) {
        const finalPct = row.find('.grade-final-pct').val();
        const statusSel = row.find('.grade-status');
        const status = statusSel.val();
        const reExamPct = row.find('.grade-reexam-pct').val();

        const usingStatus = status !== '';
        const usingPct = finalPct !== '' && !usingStatus;

        // Exclusivity: a chosen status disables the percentage inputs, and vice versa.
        row.find('.grade-final-pct').prop('disabled', gradesLocked || usingStatus);
        statusSel.prop('disabled', gradesLocked || (finalPct !== '' && !usingStatus));

        const finalEquiv = usingPct ? toEquivalent(finalPct) : null;
        row.find('.grade-equiv').text(finalEquiv !== null ? finalEquiv : (usingStatus ? status : '—'))
            .toggleClass('text-danger', finalEquiv !== null && isFailingEquiv(finalEquiv));

        // Re-exam column only when the final is a failing mark.
        const reExamCell = row.find('.grade-reexam-pct').closest('td');
        const reExamEquivCell = row.find('.grade-reexam-equiv').closest('td');
        const showReExam = finalEquiv !== null && isFailingEquiv(finalEquiv);
        reExamCell.css('visibility', showReExam ? 'visible' : 'hidden');
        reExamEquivCell.css('visibility', showReExam ? 'visible' : 'hidden');
        if (!showReExam) {
            row.find('.grade-reexam-pct').val('');
        }
        const reExamEquiv = showReExam ? toEquivalent(reExamPct) : null;
        row.find('.grade-reexam-equiv').text(reExamEquiv !== null ? reExamEquiv : '');

        row.find('.grade-remark').text(remarkLabel(finalEquiv, reExamEquiv, usingStatus ? status : ''));
    }

    function updateGradeActionButtons() {
        $('#lockGradesBtn').prop('disabled', gradesLocked).toggleClass('disabled', gradesLocked);
        $('#saveGradesBtn').prop('disabled', gradesLocked).toggleClass('disabled', gradesLocked);
        $('#unlockGradesBtn').prop('disabled', !gradesLocked).toggleClass('disabled', !gradesLocked);
    }

    function collectGradeUpdates() {
        const updates = [];
        $('#gradeInputTable tbody tr').each(function () {
            const row = $(this);
            const studentId = row.data('studentId');
            const finalPct = row.find('.grade-final-pct').val();
            const status = row.find('.grade-status').val();
            const reExamPct = row.find('.grade-reexam-pct').val();
            const hours = row.find('.grade-hours').val();

            // A row is only submitted when it has been graded (percentage or status).
            if (finalPct === '' && status === '') {
                return;
            }
            updates.push({
                studentId: studentId,
                finalPercentage: status === '' && finalPct !== '' ? parseFloat(finalPct) : null,
                gradeStatus: status !== '' ? status : null,
                reExamPercentage: status === '' && reExamPct !== '' ? parseFloat(reExamPct) : null,
                hoursRendered: hours !== '' ? parseFloat(hours) : null
            });
        });
        return updates;
    }

    function saveGrades() {
        const updates = collectGradeUpdates();
        if (updates.length === 0) {
            showAlert('No grades entered. Fill in a Final % or a Status for at least one student.', 'warning');
            return;
        }
        const missingHours = updates.filter(u => u.hoursRendered === null || isNaN(u.hoursRendered));
        if (missingHours.length > 0) {
            showAlert('Hours Rendered is required for every graded student (' +
                missingHours.map(u => u.studentId).join(', ') + ').', 'warning');
            return;
        }
        $.ajax({
            url: '/api/trainer/classes/' + encodeURIComponent(currentClassId) + '/grades',
            method: 'PUT',
            contentType: 'application/json',
            data: JSON.stringify(updates),
            success: function () {
                showAlert('Grades saved successfully.', 'success');
                $.ajax({
                    url: '/api/trainer/classes/' + encodeURIComponent(currentClassId) + '/grades',
                    method: 'GET',
                    success: function (response) {
                        currentGrades = response.students || [];
                        gradesLocked = response.locked || false;
                        buildGradeInputTable();
                        updateGradeActionButtons();
                    }
                });
            },
            error: function (xhr) {
                showAlert(gradeErrorMessage(xhr, 'Failed to save grades.'), 'danger');
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
                $('#gradeInputTable tbody :input').prop('disabled', true);
                showAlert('Grades locked successfully.', 'success');
            },
            error: function (xhr) {
                showAlert(gradeErrorMessage(xhr, 'Failed to lock grades.'), 'danger');
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
                buildGradeInputTable();
                showAlert('Grades unlocked successfully.', 'success');
            },
            error: function (xhr) {
                showAlert(gradeErrorMessage(xhr, 'Failed to unlock grades.'), 'danger');
            }
        });
    }

    function gradeErrorMessage(xhr, fallback) {
        return xhr && xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : fallback;
    }

    function showAlert(message, type) {
        const el = $('#gradeInputAlert');
        el.removeClass('d-none alert-danger alert-success alert-warning alert-info');
        el.addClass('alert alert-' + type);
        el.text(message);
    }

    $('#saveGradesBtn').on('click', saveGrades);
    $('#lockGradesBtn').on('click', lockGrades);
    $('#unlockGradesBtn').on('click', unlockGrades);
});
