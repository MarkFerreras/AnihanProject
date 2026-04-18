/**
 * system-logs.js
 * Fetches system logs from GET /api/logs and renders them in a DataTables 2 table.
 * Supports server-side date filtering with preset day ranges and custom date ranges.
 * Admin-only — page is protected by auth-guard.js and SecurityConfig.
 */
$(document).ready(function () {
    'use strict';

    const $table = $('#systemLogsTable');
    const $loading = $('#logsLoading');
    const $errorAlert = $('#logsErrorAlert');
    const $errorMessage = $('#logsErrorMessage');
    const $totalLogsStat = $('#totalLogsStat');
    const $filterFeedback = $('#filterFeedback');

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
     * Hides the filter validation feedback message.
     */
    function hideFilterFeedback() {
        $filterFeedback.hide().text('');
    }

    /**
     * Shows a filter validation feedback message.
     */
    function showFilterFeedback(msg) {
        $filterFeedback.text(msg).show();
    }

    /**
     * Fetches logs from the backend with optional filter parameters and initializes/reloads DataTables.
     *
     * @param {Object} [params] - Filter parameters
     * @param {number} [params.rangeDays] - Preset day range (7, 14, or 30)
     * @param {string} [params.startDate] - Custom range start (YYYY-MM-DD)
     * @param {string} [params.endDate]   - Custom range end (YYYY-MM-DD)
     */
    function loadLogs(params) {
        hideFilterFeedback();

        // Build URL with query parameters
        var url = '/api/logs';
        var queryParts = [];

        if (params) {
            if (params.startDate && params.endDate) {
                queryParts.push('startDate=' + encodeURIComponent(params.startDate));
                queryParts.push('endDate=' + encodeURIComponent(params.endDate));
            } else if (params.rangeDays) {
                queryParts.push('rangeDays=' + encodeURIComponent(params.rangeDays));
            }
        }

        if (queryParts.length > 0) {
            url += '?' + queryParts.join('&');
        }

        // Show loading state
        $loading.show();
        $table.hide();
        $errorAlert.hide();

        $.ajax({
            url: url,
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
                        emptyTable: 'No system logs recorded for the selected period.',
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
                } else if (xhr.status === 400) {
                    $errorMessage.text('Invalid date range. Please check your filter inputs.');
                } else {
                    $errorMessage.text('Failed to load system logs. Please try again later.');
                }
            }
        });
    }

    // ========== Filter Event Handlers ==========

    /**
     * Sets the active state on preset buttons and clears custom date inputs.
     */
    function setPresetActive(rangeDays) {
        $('.btn-filter-preset').removeClass('active');
        $('.btn-filter-preset[data-range="' + rangeDays + '"]').addClass('active');
        // Clear custom date inputs when using presets
        $('#filterFrom').val('');
        $('#filterTo').val('');
        hideFilterFeedback();
    }

    // Preset day-range buttons (7, 14, 30)
    $('.btn-filter-preset').on('click', function () {
        var range = parseInt($(this).data('range'), 10);
        setPresetActive(range);
        loadLogs({ rangeDays: range });
    });

    // Apply custom date range
    $('#filterApplyBtn').on('click', function () {
        var from = $('#filterFrom').val();
        var to = $('#filterTo').val();

        if (!from || !to) {
            showFilterFeedback('Please select both From and To dates.');
            return;
        }

        if (from > to) {
            showFilterFeedback('From date must not be after To date.');
            return;
        }

        // Deactivate preset buttons when using custom range
        $('.btn-filter-preset').removeClass('active');
        hideFilterFeedback();

        loadLogs({ startDate: from, endDate: to });
    });

    // Reset to default (7 days)
    $('#filterResetBtn').on('click', function () {
        setPresetActive(7);
        loadLogs({ rangeDays: 7 });
    });

    // ========== Initial Load ==========
    // Default: last 7 days
    loadLogs({ rangeDays: 7 });
});
