/**
 * system-logs.js
 * Fetches system logs from GET /api/logs and renders them in a DataTables 2 table.
 * Admin-only — page is protected by auth-guard.js and SecurityConfig.
 */
$(document).ready(function () {
    'use strict';

    const $table = $('#systemLogsTable');
    const $loading = $('#logsLoading');
    const $errorAlert = $('#logsErrorAlert');
    const $errorMessage = $('#logsErrorMessage');
    const $totalLogsStat = $('#totalLogsStat');

    /**
     * Formats a LocalDateTime string (ISO) to a human-readable format.
     * Input:  "2026-04-14T14:30:00"
     * Output: "2026-04-14  14:30:00"
     */
    function formatTimestamp(isoString) {
        if (!isoString) return '-';
        // Handle array format [year, month, day, hour, minute, second] from Spring
        if (Array.isArray(isoString)) {
            const [y, mo, d, h, mi, s] = isoString;
            return String(y).padStart(4, '0') + '-' +
                   String(mo).padStart(2, '0') + '-' +
                   String(d).padStart(2, '0') + '  ' +
                   String(h).padStart(2, '0') + ':' +
                   String(mi).padStart(2, '0') + ':' +
                   String(s || 0).padStart(2, '0');
        }
        // Handle ISO string format
        const dt = new Date(isoString);
        if (isNaN(dt.getTime())) return isoString;
        const yyyy = dt.getFullYear();
        const mm = String(dt.getMonth() + 1).padStart(2, '0');
        const dd = String(dt.getDate()).padStart(2, '0');
        const hh = String(dt.getHours()).padStart(2, '0');
        const mi = String(dt.getMinutes()).padStart(2, '0');
        const ss = String(dt.getSeconds()).padStart(2, '0');
        return yyyy + '-' + mm + '-' + dd + '  ' + hh + ':' + mi + ':' + ss;
    }

    /**
     * Returns a role pill HTML badge matching the dashboard.css styles.
     */
    function renderRolePill(role) {
        const roleClean = (role || '').replace('ROLE_', '').toUpperCase();
        let pillClass = '';
        switch (roleClean) {
            case 'ADMIN':
                pillClass = 'role-pill-admin';
                break;
            case 'REGISTRAR':
                pillClass = 'role-pill-registrar';
                break;
            case 'TRAINER':
                pillClass = 'role-pill-trainer';
                break;
            default:
                pillClass = '';
        }
        return '<span class="role-pill ' + pillClass + '">' + roleClean + '</span>';
    }

    /**
     * Fetches logs and initializes/reloads DataTables.
     */
    function loadLogs() {
        $.ajax({
            url: '/api/logs',
            method: 'GET',
            dataType: 'json',
            success: function (data) {
                $loading.hide();
                $errorAlert.hide();
                $table.show();

                // Update stats
                $totalLogsStat.text('Total: ' + data.length);

                // Initialize DataTable
                if ($.fn.DataTable.isDataTable($table)) {
                    $table.DataTable().clear().destroy();
                }

                $table.DataTable({
                    data: data,
                    columns: [
                        {
                            data: 'timestamp',
                            render: function (data) {
                                return '<span class="log-timestamp">' + formatTimestamp(data) + '</span>';
                            }
                        },
                        {
                            data: 'userId',
                            render: function (data) {
                                return '<span class="log-user-id">' + (data != null ? data : '-') + '</span>';
                            }
                        },
                        {
                            data: 'username',
                            render: function (data) {
                                return '<strong>' + $('<span>').text(data).html() + '</strong>';
                            }
                        },
                        {
                            data: 'role',
                            render: function (data) {
                                return renderRolePill(data);
                            }
                        },
                        {
                            data: 'action',
                            render: function (data) {
                                return '<span class="log-action-text">' + $('<span>').text(data).html() + '</span>';
                            }
                        },
                        {
                            data: 'ipAddress',
                            render: function (data) {
                                return '<span class="log-ip">' + (data || '-') + '</span>';
                            }
                        }
                    ],
                    order: [[0, 'desc']],
                    pageLength: 25,
                    lengthMenu: [10, 25, 50, 100],
                    language: {
                        search: 'Search logs:',
                        lengthMenu: 'Show _MENU_ entries',
                        info: 'Showing _START_ to _END_ of _TOTAL_ log entries',
                        infoEmpty: 'No log entries found',
                        emptyTable: 'No system logs recorded yet.',
                        paginate: {
                            first: 'First',
                            last: 'Last',
                            next: 'Next',
                            previous: 'Previous'
                        }
                    },
                    responsive: true
                });
            },
            error: function (xhr) {
                $loading.hide();
                $table.hide();
                $errorAlert.show();

                if (xhr.status === 401) {
                    $errorMessage.text('Session expired. Redirecting to login...');
                    setTimeout(function () {
                        window.location.href = '/index.html';
                    }, 1500);
                } else if (xhr.status === 403) {
                    $errorMessage.text('Access denied. You do not have permission to view system logs.');
                } else {
                    $errorMessage.text('Failed to load system logs. Please try again later.');
                }
            }
        });
    }

    // Initial load
    loadLogs();
});
