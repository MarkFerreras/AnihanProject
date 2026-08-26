/**
 * registrar-trainers.js — Trainer cards + classes-by-subject modal
 */
(function () {
    'use strict';

    let trainerClassesModal = null;

    $(document).ready(function () {
        trainerClassesModal = new bootstrap.Modal(document.getElementById('trainerClassesModal'));
        loadTrainers();

        $('#trainerGrid').on('click', '.trainer-card', function () {
            const id = $(this).data('trainer-id');
            const name = $(this).data('trainer-name');
            openTrainerClasses(id, name);
        });
    });

    function loadTrainers() {
        $.ajax({
            url: '/api/registrar/trainers/summary',
            method: 'GET',
            success: function (trainers) {
                renderTrainers(trainers);
            },
            error: function () {
                $('#trainerCountSubtitle').text('Failed to load trainers.');
            }
        });
    }

    function renderTrainers(trainers) {
        const $grid = $('#trainerGrid');
        $grid.empty();

        if (!trainers.length) {
            $('#trainerCountSubtitle').text('No active trainers found.');
            return;
        }

        $('#trainerCountSubtitle').text(
            trainers.length + ' trainer' + (trainers.length === 1 ? '' : 's') +
            ' — class counts are for the current semester.'
        );

        trainers.forEach(function (t) {
            const fullName = t.lastName + ', ' + t.firstName;
            const $card = $(
                '<div class="trainer-card surface-card" tabindex="0" role="button" ' +
                'data-trainer-id="' + t.userId + '" ' +
                'data-trainer-name="' + escapeHtml(fullName) + '">' +
                '<table class="trainer-card-table">' +
                '<tr><th>Last Name</th><td>' + escapeHtml(t.lastName) + '</td></tr>' +
                '<tr><th>First Name</th><td>' + escapeHtml(t.firstName) + '</td></tr>' +
                '<tr><th>Email</th><td>' + escapeHtml(t.email || 'Not Available') + '</td></tr>' +
                '<tr><th>Classes</th><td><span class="status-badge status-badge-graduated">' +
                t.classCount + '</span></td></tr>' +
                '</table>' +
                '</div>'
            );
            $grid.append($card);
        });
    }

    function openTrainerClasses(trainerId, trainerName) {
        $('#trainerClassesModalLabel').text('Classes — ' + trainerName);
        $('#trainerClassesSubtitle').text('Loading…');
        $('#trainerClassesContent').empty();
        trainerClassesModal.show();

        $.ajax({
            url: '/api/registrar/trainers/' + encodeURIComponent(trainerId) + '/classes',
            method: 'GET',
            success: function (classes) {
                renderTrainerClasses(classes);
            },
            error: function () {
                $('#trainerClassesSubtitle').text('Failed to load classes.');
            }
        });
    }

    function renderTrainerClasses(classes) {
        if (!classes.length) {
            $('#trainerClassesSubtitle').text('No classes assigned this semester.');
            $('#trainerClassesContent').empty();
            return;
        }

        $('#trainerClassesSubtitle').text(
            classes.length + ' class' + (classes.length === 1 ? '' : 'es') + ' this semester, by subject:'
        );

        // Group by subject, preserving first-seen order.
        const bySubject = new Map();
        classes.forEach(function (c) {
            const key = c.subjectCode;
            if (!bySubject.has(key)) {
                bySubject.set(key, { name: c.subjectName, code: c.subjectCode, rows: [] });
            }
            bySubject.get(key).rows.push(c);
        });

        const $content = $('#trainerClassesContent');
        $content.empty();

        bySubject.forEach(function (group) {
            const $section = $('<div class="mb-3"></div>');
            $section.append(
                '<h6 class="mb-2">' + escapeHtml(group.name) +
                ' <span class="text-muted">(' + escapeHtml(group.code) + ')</span></h6>'
            );
            const $table = $(
                '<table class="table table-sm table-hover">' +
                '<thead><tr><th>Section</th><th>Semester</th><th>Enrolled</th></tr></thead>' +
                '<tbody></tbody></table>'
            );
            const $tbody = $table.find('tbody');
            group.rows.forEach(function (c) {
                $tbody.append(
                    '<tr><td>' + escapeHtml(c.sectionName) +
                    ' <span class="text-muted">(' + escapeHtml(c.sectionCode) + ')</span></td>' +
                    '<td>' + escapeHtml(c.semester) + '</td>' +
                    '<td>' + c.enrolledCount + '</td></tr>'
                );
            });
            $section.append($table);
            $content.append($section);
        });
    }

    function escapeHtml(str) {
        if (!str) return '';
        const div = document.createElement('div');
        div.appendChild(document.createTextNode(str));
        return div.innerHTML;
    }
})();
