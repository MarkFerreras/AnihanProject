$(function () {
    'use strict';

    let subjectsTable;
    let rosterTable;
    let currentSubjectCode = null;

    subjectsTable = $('#subjectsTable').DataTable({
        ajax: {
            url: '/api/trainer/subjects',
            dataSrc: '',
            error: function () {
                console.error('Failed to load subjects.');
            }
        },
        columns: [
            { data: 'subjectCode' },
            { data: 'subjectName' },
            {
                data: 'qualificationName',
                render: val => val || '<em class="text-muted">Not Available</em>'
            },
            { data: 'units' },
            { data: 'enrolledCount' },
            {
                data: 'sectionNames',
                render: val => Array.isArray(val) && val.length ? val.join(', ') : '<em class="text-muted">None</em>'
            }
        ],
        order: [[0, 'asc']],
        pageLength: 25,
        language: { emptyTable: 'No subjects assigned.' }
    });

    rosterTable = $('#rosterTable').DataTable({
        columns: [
            { data: 'studentId' },
            { data: 'lastName' },
            { data: 'firstName' },
            {
                data: 'middleName',
                render: val => val || '<em class="text-muted">—</em>'
            },
            { data: 'sectionCode' },
            { data: 'sectionName' }
        ],
        order: [[5, 'asc'], [1, 'asc'], [2, 'asc']],
        pageLength: 50,
        language: { emptyTable: 'No students enrolled.' }
    });

    $('#subjectsTable tbody').on('click', 'tr', function () {
        const data = subjectsTable.row(this).data();
        if (!data) return;
        openRoster(data.subjectCode, data.subjectName);
    });

    $('#closeRosterBtn').on('click', function () {
        $('#studentRosterPanel').addClass('d-none');
        currentSubjectCode = null;
    });

    function openRoster(subjectCode, subjectName) {
        currentSubjectCode = subjectCode;
        $('#rosterSubjectTitle').text('Students — ' + subjectName);
        $('#rosterSubjectSubtitle').text('Subject Code: ' + subjectCode);
        rosterTable.clear().draw();
        $('#studentRosterPanel').removeClass('d-none');

        $.ajax({
            url: '/api/trainer/subjects/' + encodeURIComponent(subjectCode) + '/students',
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
