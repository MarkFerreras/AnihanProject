$(function () {
    'use strict';

    let classesTable;
    let rosterTable;
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
                    return '<button class="btn btn-sm btn-surface-secondary view-students-btn">View Students</button>';
                }
            }
        ],
        order: [[0, 'desc'], [2, 'asc'], [3, 'asc']],
        pageLength: 25,
        language: { emptyTable: 'No classes assigned.' }
    });

    rosterTable = $('#classRosterTable').DataTable({
        autoWidth: false,
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

    $('#classesTable tbody').on('click', '.view-students-btn', function () {
        const row = classesTable.row($(this).closest('tr')).data();
        if (!row) return;
        openRoster(row.classId, row.subjectName, row.sectionName, row.semester);
    });

    $('#classRosterModal').on('shown.bs.modal', function () {
        rosterTable.columns.adjust().draw();
    });

    function openRoster(classId, subjectName, sectionName, semester) {
        $('#classRosterModalLabel').text('Enrolled Students — ' + subjectName);
        $('#classRosterSubtitle').text(sectionName + ' | Semester: ' + (semester || 'N/A'));
        rosterTable.clear().draw();
        classRosterModal.show();

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
