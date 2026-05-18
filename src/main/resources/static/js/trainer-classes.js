$(function () {
    'use strict';

    let classesTable;
    let rosterTable;
    let currentClassId = null;

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
            { data: 'enrolledCount' }
        ],
        order: [[0, 'desc'], [2, 'asc'], [3, 'asc']],
        pageLength: 25,
        language: { emptyTable: 'No classes assigned.' }
    });

    rosterTable = $('#rosterTable').DataTable({
        columns: [
            { data: 'studentId' },
            { data: 'lastName' },
            { data: 'firstName' },
            {
                data: 'middleName',
                render: val => val || '<em class="text-muted">—</em>'
            }
        ],
        order: [[1, 'asc'], [2, 'asc']],
        pageLength: 50,
        language: { emptyTable: 'No students enrolled.' }
    });

    $('#classesTable tbody').on('click', 'tr', function () {
        const data = classesTable.row(this).data();
        if (!data) return;
        openRoster(data.classId, data.subjectName, data.sectionName, data.semester);
    });

    $('#closeRosterBtn').on('click', function () {
        $('#studentRosterPanel').addClass('d-none');
        currentClassId = null;
    });

    function openRoster(classId, subjectName, sectionName, semester) {
        currentClassId = classId;
        $('#rosterClassTitle').text('Students — ' + subjectName);
        $('#rosterClassSubtitle').text(sectionName + ' | Semester: ' + (semester || 'N/A'));
        rosterTable.clear().draw();
        $('#studentRosterPanel').removeClass('d-none');

        $.ajax({
            url: '/api/trainer/classes/' + encodeURIComponent(classId) + '/students',
            method: 'GET',
            success: function (rows) {
                rosterTable.rows.add(rows).draw();
            },
            error: function (xhr) {
                const msg = xhr.responseJSON && xhr.responseJSON.message
                    ? xhr.responseJSON.message
                    : 'Failed to load student roster.';
                console.error(msg);
            }
        });
    }
});
