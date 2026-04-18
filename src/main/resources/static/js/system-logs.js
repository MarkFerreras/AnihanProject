/**
 * system-logs.js
 * Fetches system logs from GET /api/logs, supports preset and exact-date filtering,
 * and exports the currently selected range through GET /api/logs/export.
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
    const $exportFeedback = $('#exportFeedback');
    const $filterFrom = $('#filterFrom');
    const $filterTo = $('#filterTo');
    const $exportFormat = $('#exportFormat');
    const $exportButton = $('#exportLogsBtn');

    let currentFilter = {
        mode: 'preset',
        rangeDays: 7
    };

    function formatTimestamp(isoString) {
        if (!isoString) {
            return '-';
        }

        if (Array.isArray(isoString)) {
            const year = String(isoString[0]).padStart(4, '0');
            const month = String(isoString[1]).padStart(2, '0');
            const day = String(isoString[2]).padStart(2, '0');
            const hour = String(isoString[3]).padStart(2, '0');
            const minute = String(isoString[4]).padStart(2, '0');
            const second = String(isoString[5] || 0).padStart(2, '0');
            return year + '-' + month + '-' + day + '  ' + hour + ':' + minute + ':' + second;
        }

        const parsed = new Date(isoString);
        if (isNaN(parsed.getTime())) {
            return isoString;
        }

        return parsed.getFullYear() + '-' +
            String(parsed.getMonth() + 1).padStart(2, '0') + '-' +
            String(parsed.getDate()).padStart(2, '0') + '  ' +
            String(parsed.getHours()).padStart(2, '0') + ':' +
            String(parsed.getMinutes()).padStart(2, '0') + ':' +
            String(parsed.getSeconds()).padStart(2, '0');
    }

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

    function hideFilterFeedback() {
        $filterFeedback.hide().text('');
    }

    function showFilterFeedback(message) {
        $filterFeedback.text(message).show();
    }

    function hideExportFeedback() {
        $exportFeedback.hide().text('');
    }

    function showExportFeedback(message) {
        $exportFeedback.text(message).show();
    }

    function buildQueryParts(filter, format) {
        const queryParts = [];

        if (format) {
            queryParts.push('format=' + encodeURIComponent(format));
        }

        if (filter.startDate && filter.endDate) {
            queryParts.push('startDate=' + encodeURIComponent(filter.startDate));
            queryParts.push('endDate=' + encodeURIComponent(filter.endDate));
        } else if (filter.rangeDays) {
            queryParts.push('rangeDays=' + encodeURIComponent(filter.rangeDays));
        }

        return queryParts;
    }

    function buildApiUrl(basePath, filter, format) {
        const queryParts = buildQueryParts(filter, format);
        return queryParts.length > 0 ? basePath + '?' + queryParts.join('&') : basePath;
    }

    function clearDateInputs() {
        $filterFrom.val('');
        $filterTo.val('');
    }

    function setPresetActive(rangeDays) {
        $('.btn-filter-preset').removeClass('active');
        $('.btn-filter-preset[data-range="' + rangeDays + '"]').addClass('active');
    }

    function clearPresetActive() {
        $('.btn-filter-preset').removeClass('active');
    }

    function applyPreset(rangeDays) {
        currentFilter = {
            mode: 'preset',
            rangeDays: rangeDays
        };
        setPresetActive(rangeDays);
        clearDateInputs();
        hideFilterFeedback();
        hideExportFeedback();
        loadLogs();
    }

    function applyDateRange(from, to) {
        currentFilter = {
            mode: 'date',
            startDate: from,
            endDate: to
        };
        clearPresetActive();
        hideFilterFeedback();
        hideExportFeedback();
        loadLogs();
    }

    function loadLogs() {
        hideFilterFeedback();
        hideExportFeedback();

        const url = buildApiUrl('/api/logs', currentFilter);

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
                $totalLogsStat.text('Total: ' + data.length);

                if ($.fn.DataTable.isDataTable($table)) {
                    $table.DataTable().clear().destroy();
                }

                $table.DataTable({
                    data: data,
                    columns: [
                        {
                            data: 'timestamp',
                            render: function (value) {
                                return '<span class="log-timestamp">' + formatTimestamp(value) + '</span>';
                            }
                        },
                        {
                            data: 'userId',
                            render: function (value) {
                                return '<span class="log-user-id">' + (value != null ? value : '-') + '</span>';
                            }
                        },
                        {
                            data: 'username',
                            render: function (value) {
                                return '<strong>' + $('<span>').text(value || '-').html() + '</strong>';
                            }
                        },
                        {
                            data: 'role',
                            render: function (value) {
                                return renderRolePill(value);
                            }
                        },
                        {
                            data: 'action',
                            render: function (value) {
                                return '<span class="log-action-text">' + $('<span>').text(value || '-').html() + '</span>';
                            }
                        },
                        {
                            data: 'ipAddress',
                            render: function (value) {
                                return '<span class="log-ip">' + (value || '-') + '</span>';
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

    async function exportLogs() {
        hideExportFeedback();
        $exportButton.prop('disabled', true).text('Exporting...');

        try {
            const format = $exportFormat.val();
            const url = buildApiUrl('/api/logs/export', currentFilter, format);
            const response = await fetch(url, { method: 'GET' });

            if (response.status === 401) {
                showExportFeedback('Session expired. Redirecting to login...');
                setTimeout(function () {
                    window.location.href = '/index.html';
                }, 1500);
                return;
            }

            if (response.status === 403) {
                showExportFeedback('Access denied. You do not have permission to export logs.');
                return;
            }

            if (response.status === 400) {
                showExportFeedback('The selected filter or export format is invalid.');
                return;
            }

            if (!response.ok) {
                showExportFeedback('Export failed. Please try again later.');
                return;
            }

            const blob = await response.blob();
            const objectUrl = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            const disposition = response.headers.get('Content-Disposition') || '';
            const fileNameMatch = disposition.match(/filename="?([^"]+)"?/i);

            link.href = objectUrl;
            link.download = fileNameMatch ? fileNameMatch[1] : 'system-logs.' + format;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            window.URL.revokeObjectURL(objectUrl);

            showExportFeedback('Export ready.');
        } catch (error) {
            showExportFeedback('Export failed. Please try again later.');
        } finally {
            $exportButton.prop('disabled', false).text('Export');
        }
    }

    $('.btn-filter-preset').on('click', function () {
        applyPreset(parseInt($(this).data('range'), 10));
    });

    $('#filterApplyBtn').on('click', function () {
        const from = $filterFrom.val();
        const to = $filterTo.val();

        if (!from || !to) {
            showFilterFeedback('Please select both From and To dates.');
            return;
        }

        if (from > to) {
            showFilterFeedback('From date must not be after To date.');
            return;
        }

        applyDateRange(from, to);
    });

    $('#filterResetBtn').on('click', function () {
        applyPreset(7);
    });

    $('#exportLogsBtn').on('click', function () {
        exportLogs();
    });

    loadLogs();
});
