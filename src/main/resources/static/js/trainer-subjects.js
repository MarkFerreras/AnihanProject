$(function () {
    'use strict';

    let subjectsTable;
    let rosterTable;
    const subjectRosterModal = new bootstrap.Modal(document.getElementById('subjectRosterModal'));

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
            },
            {
                data: null,
                orderable: false,
                render: function () {
                    return '<button class="btn btn-sm btn-surface-secondary view-students-btn">View Students</button>';
                }
            }
        ],
        order: [[0, 'asc']],
        pageLength: 25,
        language: { emptyTable: 'No subjects assigned.' }
    });

    rosterTable = $('#subjectRosterTable').DataTable({
        autoWidth: false,
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

    $('#subjectsTable tbody').on('click', '.view-students-btn', function () {
        const row = subjectsTable.row($(this).closest('tr')).data();
        if (!row) return;
        openRoster(row.subjectCode, row.subjectName);
    });

    $('#subjectRosterModal').on('shown.bs.modal', function () {
        rosterTable.columns.adjust().draw();
    });

    function openRoster(subjectCode, subjectName) {
        $('#subjectRosterModalLabel').text('Enrolled Students — ' + subjectName);
        $('#subjectRosterSubtitle').text('Subject Code: ' + subjectCode);
        rosterTable.clear().draw();
        subjectRosterModal.show();

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
