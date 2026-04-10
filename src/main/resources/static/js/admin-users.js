(function () {
    'use strict';

    const ROLE_LABELS = {
        ROLE_ADMIN: 'Admin',
        ROLE_REGISTRAR: 'Registrar',
        ROLE_TRAINER: 'Trainer'
    };

    function escapeHtml(value) {
        return String(value ?? '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function formatName(user) {
        const parts = [user.lastName, user.firstName, user.middleName].filter(Boolean);
        return parts.length ? parts.join(', ') : 'Not set';
    }

    function renderRolePill(role) {
        const roleClass = {
            ROLE_ADMIN: 'role-pill role-pill-admin',
            ROLE_REGISTRAR: 'role-pill role-pill-registrar',
            ROLE_TRAINER: 'role-pill role-pill-trainer'
        }[role] || 'role-pill';

        return '<span class="' + roleClass + '">' + escapeHtml(ROLE_LABELS[role] || role) + '</span>';
    }

    function setText(id, value) {
        const element = document.getElementById(id);
        if (element) {
            element.textContent = value ?? '-';
        }
    }

    function updateStats(users) {
        setText('totalUsersStat', users.length);
        setText('adminUsersStat', users.filter(user => user.role === 'ROLE_ADMIN').length);
        setText('registrarUsersStat', users.filter(user => user.role === 'ROLE_REGISTRAR').length);
        setText('trainerUsersStat', users.filter(user => user.role === 'ROLE_TRAINER').length);
    }

    async function loadUserDetails(userId, modal) {
        const response = await fetch('/api/admin/users/' + encodeURIComponent(userId), {
            credentials: 'same-origin'
        });

        if (!response.ok) {
            throw new Error('Failed to load user details.');
        }

        const user = await response.json();
        setText('detailsUserId', user.userId);
        setText('detailsUsername', user.username);
        setText('detailsEmail', user.email || 'Not set');
        setText('detailsLastName', user.lastName || 'Not set');
        setText('detailsFirstName', user.firstName || 'Not set');
        setText('detailsMiddleName', user.middleName || 'Not set');
        setText('detailsAge', user.age ?? 'Not set');
        setText('detailsBirthdate', user.birthdate || 'Not set');

        const roleElement = document.getElementById('detailsRole');
        if (roleElement) {
            roleElement.innerHTML = renderRolePill(user.role);
        }

        const editLink = document.getElementById('detailsEditLink');
        const logsLink = document.getElementById('detailsLogsLink');
        if (editLink) {
            editLink.href = 'edit-user.html?id=' + encodeURIComponent(user.userId);
        }
        if (logsLink) {
            logsLink.href = 'logs.html?user=' + encodeURIComponent(user.userId);
        }

        modal.show();
    }

    document.addEventListener('DOMContentLoaded', function () {
        const tableElement = document.getElementById('usersTable');
        if (!tableElement || !window.jQuery || !window.bootstrap) {
            return;
        }

        const modal = new bootstrap.Modal(document.getElementById('userDetailsModal'));

        window.jQuery('#usersTable').DataTable({
            ajax: {
                url: '/api/admin/users',
                dataSrc: function (json) {
                    updateStats(json);
                    return json;
                }
            },
            columns: [
                { data: 'userId' },
                {
                    data: 'role',
                    render: function (data) {
                        return renderRolePill(data);
                    }
                },
                {
                    data: null,
                    render: function (data, type, row) {
                        return escapeHtml(formatName(row));
                    }
                },
                {
                    data: 'email',
                    render: function (data) {
                        return escapeHtml(data || 'Not set');
                    }
                },
                {
                    data: null,
                    orderable: false,
                    className: 'text-end',
                    render: function (data, type, row) {
                        return '<button class="btn btn-surface-secondary btn-sm" data-user-id="' +
                            escapeHtml(row.userId) + '">Open Details</button>';
                    }
                }
            ],
            order: [[0, 'asc']],
            language: {
                emptyTable: 'No users found.'
            }
        });

        window.jQuery('#usersTable tbody').on('click', 'button[data-user-id]', async function () {
            try {
                await loadUserDetails(this.getAttribute('data-user-id'), modal);
            } catch (error) {
                window.alert('Unable to load the selected user right now.');
            }
        });
    });
})();
