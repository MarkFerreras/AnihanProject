(function () {
    'use strict';

    const ROLE_LABELS = {
        ROLE_ADMIN: 'Admin',
        ROLE_REGISTRAR: 'Registrar',
        ROLE_TRAINER: 'Trainer'
    };

    let currentDeleteUserId = null;
    let detailsModal = null;
    let deleteConfirmModal = null;

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

    function renderStatusBadge(enabled) {
        if (enabled === false) {
            return '<span class="status-badge status-badge-disabled">Disabled</span>';
        }
        return '<span class="status-badge status-badge-active">Active</span>';
    }

    function setText(id, value) {
        const element = document.getElementById(id);
        if (element) {
            element.textContent = value ?? '-';
        }
    }

    function setAlert(id, message, type) {
        const element = document.getElementById(id);
        if (!element) {
            return;
        }

        element.className = 'alert alert-' + type + ' mt-3';
        element.textContent = message;
        element.classList.remove('d-none');
    }

    function hideAlert(id) {
        const element = document.getElementById(id);
        if (!element) {
            return;
        }

        element.classList.add('d-none');
        element.textContent = '';
    }

    function updateStats(users) {
        setText('totalUsersStat', users.length);
        setText('adminUsersStat', users.filter(user => user.role === 'ROLE_ADMIN').length);
        setText('registrarUsersStat', users.filter(user => user.role === 'ROLE_REGISTRAR').length);
        setText('trainerUsersStat', users.filter(user => user.role === 'ROLE_TRAINER').length);
    }

    async function loadUserDetails(userId) {
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

        // Display password last changed timestamp
        setText('detailsPasswordChanged', user.passwordChangedAt
            ? new Date(user.passwordChangedAt).toLocaleString()
            : 'Never');

        const roleElement = document.getElementById('detailsRole');
        if (roleElement) {
            roleElement.innerHTML = renderRolePill(user.role);
        }

        const editLink = document.getElementById('detailsEditLink');
        const logsLink = document.getElementById('detailsLogsLink');
        const deleteBtn = document.getElementById('detailsDeleteBtn');
        const reEnableBtn = document.getElementById('detailsReEnableBtn');

        if (editLink) {
            editLink.href = 'edit-user.html?id=' + encodeURIComponent(user.userId);
        }
        if (logsLink) {
            logsLink.href = 'logs.html?user=' + encodeURIComponent(user.userId);
        }

        // Store the user ID for delete and set the display name
        currentDeleteUserId = user.userId;
        const deleteUserNameEl = document.getElementById('deleteUserName');
        if (deleteUserNameEl) {
            deleteUserNameEl.textContent = user.username;
        }

        // Toggle delete vs re-enable based on user status and self-check
        const meResponse = await fetch('/api/auth/me', { credentials: 'same-origin' });
        var isSelf = false;
        if (meResponse.ok) {
            var me = await meResponse.json();
            isSelf = (me.username === user.username);
        }

        if (deleteBtn) {
            // Show delete only for active users who are not self
            deleteBtn.style.display = (!isSelf && user.enabled !== false) ? '' : 'none';
        }
        if (reEnableBtn) {
            // Show re-enable only for disabled users
            reEnableBtn.style.display = (user.enabled === false) ? '' : 'none';
        }

        detailsModal.show();
    }

    async function deleteUser(userId, permanent) {
        const url = permanent
            ? '/api/admin/users/' + encodeURIComponent(userId) + '/permanent'
            : '/api/admin/users/' + encodeURIComponent(userId);

        const response = await fetch(url, {
            method: 'DELETE',
            credentials: 'same-origin'
        });

        if (!response.ok) {
            const data = await response.json().catch(function () { return {}; });
            throw new Error(data.message || 'Failed to delete user.');
        }

        return response.json();
    }

    function setupDeleteHandlers(dataTable) {
        const detailsDeleteBtn = document.getElementById('detailsDeleteBtn');
        const confirmSoftDeleteBtn = document.getElementById('confirmSoftDeleteBtn');
        const confirmHardDeleteBtn = document.getElementById('confirmHardDeleteBtn');

        if (detailsDeleteBtn) {
            detailsDeleteBtn.addEventListener('click', function () {
                // Close the details modal and open the delete confirmation modal
                detailsModal.hide();
                hideAlert('deleteResultAlert');
                deleteConfirmModal.show();
            });
        }

        if (confirmSoftDeleteBtn) {
            confirmSoftDeleteBtn.addEventListener('click', async function () {
                confirmSoftDeleteBtn.disabled = true;
                confirmSoftDeleteBtn.textContent = 'Deactivating...';
                hideAlert('deleteResultAlert');

                try {
                    await deleteUser(currentDeleteUserId, false);
                    setAlert('deleteResultAlert', 'Account deactivated successfully.', 'success');
                    confirmSoftDeleteBtn.style.display = 'none';
                    confirmHardDeleteBtn.style.display = 'none';

                    window.setTimeout(function () {
                        deleteConfirmModal.hide();
                        dataTable.ajax.reload(null, false);
                    }, 1200);
                } catch (error) {
                    setAlert('deleteResultAlert', error.message, 'danger');
                } finally {
                    confirmSoftDeleteBtn.disabled = false;
                    confirmSoftDeleteBtn.textContent = 'Deactivate Account';
                }
            });
        }

        if (confirmHardDeleteBtn) {
            confirmHardDeleteBtn.addEventListener('click', async function () {
                // Extra confirmation for permanent deletion
                if (!window.confirm('This action is PERMANENT and cannot be undone. Are you absolutely sure?')) {
                    return;
                }

                confirmHardDeleteBtn.disabled = true;
                confirmHardDeleteBtn.textContent = 'Deleting...';
                hideAlert('deleteResultAlert');

                try {
                    await deleteUser(currentDeleteUserId, true);
                    setAlert('deleteResultAlert', 'Account permanently deleted.', 'success');
                    confirmSoftDeleteBtn.style.display = 'none';
                    confirmHardDeleteBtn.style.display = 'none';

                    window.setTimeout(function () {
                        deleteConfirmModal.hide();
                        dataTable.ajax.reload(null, false);
                    }, 1200);
                } catch (error) {
                    setAlert('deleteResultAlert', error.message, 'danger');
                } finally {
                    confirmHardDeleteBtn.disabled = false;
                    confirmHardDeleteBtn.textContent = 'Permanently Delete';
                }
            });
        }

        // Reset the confirmation modal state when it is hidden
        var deleteModalEl = document.getElementById('deleteConfirmModal');
        if (deleteModalEl) {
            deleteModalEl.addEventListener('hidden.bs.modal', function () {
                hideAlert('deleteResultAlert');
                if (confirmSoftDeleteBtn) {
                    confirmSoftDeleteBtn.style.display = '';
                    confirmSoftDeleteBtn.disabled = false;
                    confirmSoftDeleteBtn.textContent = 'Deactivate Account';
                }
                if (confirmHardDeleteBtn) {
                    confirmHardDeleteBtn.style.display = '';
                    confirmHardDeleteBtn.disabled = false;
                    confirmHardDeleteBtn.textContent = 'Permanently Delete';
                }
            });
        }
    }

    document.addEventListener('DOMContentLoaded', function () {
        // Show success alert if redirected from add-user page
        var params = new URLSearchParams(window.location.search);
        if (params.get('created') === 'true') {
            var alertEl = document.getElementById('createdSuccessAlert');
            if (alertEl) {
                alertEl.classList.remove('d-none');
                alertEl.classList.add('show');
            }
            // Clean the URL without reloading
            window.history.replaceState({}, document.title, window.location.pathname);
        }

        const tableElement = document.getElementById('usersTable');
        if (!tableElement || !window.jQuery || !window.bootstrap) {
            return;
        }

        detailsModal = new bootstrap.Modal(document.getElementById('userDetailsModal'));
        deleteConfirmModal = new bootstrap.Modal(document.getElementById('deleteConfirmModal'));

        var dataTable = window.jQuery('#usersTable').DataTable({
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
                    data: 'enabled',
                    render: function (data) {
                        return renderStatusBadge(data);
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

        setupDeleteHandlers(dataTable);
        setupReEnableHandler(dataTable);

        window.jQuery('#usersTable tbody').on('click', 'button[data-user-id]', async function () {
            try {
                await loadUserDetails(this.getAttribute('data-user-id'));
            } catch (error) {
                window.alert('Unable to load the selected user right now.');
            }
        });
    });

    function setupReEnableHandler(dataTable) {
        var reEnableBtn = document.getElementById('detailsReEnableBtn');
        if (!reEnableBtn) {
            return;
        }

        reEnableBtn.addEventListener('click', async function () {
            reEnableBtn.disabled = true;
            reEnableBtn.textContent = 'Enabling...';

            try {
                var response = await fetch('/api/admin/users/' + encodeURIComponent(currentDeleteUserId) + '/enable', {
                    method: 'PUT',
                    credentials: 'same-origin'
                });

                if (!response.ok) {
                    var data = await response.json().catch(function () { return {}; });
                    throw new Error(data.message || 'Failed to re-enable user.');
                }

                detailsModal.hide();
                dataTable.ajax.reload(null, false);
            } catch (error) {
                window.alert(error.message);
            } finally {
                reEnableBtn.disabled = false;
                reEnableBtn.textContent = 'Re-enable Account';
            }
        });
    }
})();
