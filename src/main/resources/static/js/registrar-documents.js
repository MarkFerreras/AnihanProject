/**
 * registrar-documents.js — Documents DataTable + Upload / View / Download + search & filters
 * (R3.1–R3.7: AGILE-75 … AGILE-81)
 */
(function () {
    'use strict';

    let documentsTable;
    let uploadModal = null;
    let viewModal = null;
    let deleteDocumentModal = null;
    let currentDeleteDocumentId = null;

    $(document).ready(function () {
        initTable();
        loadDocumentTypes();
        loadBatchFilter();
        loadSectionFilter();
        loadStudentsDatalist();
        setupFilters();
        setupUpload();
        setupViewAndDownload();
        setupDelete();
    });

    function buildAjaxUrl() {
        const params = new URLSearchParams();
        const q = $('#documentSearchInput').val().trim();
        const type = $('#documentTypeFilter').val();
        const batch = $('#documentBatchFilter').val();
        const section = $('#documentSectionFilter').val();
        if (q) params.set('q', q);
        if (type) params.set('type', type);
        if (batch) params.set('batchCode', batch);
        if (section) params.set('sectionCode', section);
        const query = params.toString();
        return '/api/registrar/documents' + (query ? '?' + query : '');
    }

    function initTable() {
        documentsTable = $('#documentsTable').DataTable({
            ajax: {
                url: buildAjaxUrl(),
                dataSrc: ''
            },
            searching: false,
            columns: [
                { data: 'studentId' },
                {
                    data: null,
                    render: function (data) {
                        return escapeHtml(data.lastName + ', ' + data.firstName);
                    }
                },
                { data: 'documentType', render: escapeHtml },
                { data: 'fileName', render: escapeHtml },
                {
                    data: 'fileSize',
                    className: 'text-end',
                    render: function (bytes) {
                        return formatSize(bytes);
                    }
                },
                {
                    data: 'uploadDate',
                    render: function (value) {
                        return formatDateTime(value);
                    }
                },
                {
                    data: null,
                    orderable: false,
                    className: 'text-end',
                    render: function (data) {
                        return (
                            '<div class="d-flex flex-nowrap justify-content-end gap-1 document-actions">' +
                            '<button class="btn btn-surface btn-sm view-document-btn" ' +
                            'data-id="' + data.documentId + '" ' +
                            'data-name="' + escapeHtml(data.fileName) + '" ' +
                            'data-mime="' + escapeHtml(data.fileType) + '" ' +
                            'data-student="' + escapeHtml(data.studentId) + '" ' +
                            'data-type="' + escapeHtml(data.documentType) + '">View</button>' +
                            '<button class="btn btn-surface btn-sm download-document-btn" ' +
                            'data-id="' + data.documentId + '">Download</button>' +
                            '<button class="btn btn-danger-surface btn-sm delete-document-btn" ' +
                            'data-id="' + data.documentId + '" ' +
                            'data-name="' + escapeHtml(data.fileName) + '" ' +
                            'data-student="' + escapeHtml(data.studentId) + '">Delete</button>' +
                            '</div>'
                        );
                    }
                }
            ],
            order: [[5, 'desc']],
            language: {
                emptyTable: 'No documents found.',
                zeroRecords: 'No matching documents.'
            }
        });
    }

    function reloadTable() {
        documentsTable.ajax.url(buildAjaxUrl()).load();
    }

    // -------------------------------------------------------
    // Filters (R3.5 search, R3.6 filter)
    // -------------------------------------------------------

    function setupFilters() {
        let searchDebounce = null;
        $('#documentSearchInput').on('input', function () {
            window.clearTimeout(searchDebounce);
            searchDebounce = window.setTimeout(reloadTable, 350);
        });
        $('#documentTypeFilter, #documentBatchFilter, #documentSectionFilter').on('change', reloadTable);
        $('#resetDocumentFiltersBtn').on('click', function () {
            $('#documentSearchInput').val('');
            $('#documentTypeFilter').val('');
            $('#documentBatchFilter').val('');
            $('#documentSectionFilter').val('');
            reloadTable();
        });
    }

    function loadDocumentTypes() {
        $.ajax({
            url: '/api/registrar/documents/types',
            method: 'GET',
            success: function (types) {
                const filterSel = $('#documentTypeFilter');
                const uploadSel = $('#uploadDocumentType');
                uploadSel.empty().append('<option value="">-- Select Type --</option>');
                types.forEach(function (t) {
                    filterSel.append('<option value="' + escapeHtml(t) + '">' + escapeHtml(t) + '</option>');
                    uploadSel.append('<option value="' + escapeHtml(t) + '">' + escapeHtml(t) + '</option>');
                });
            }
        });
    }

    function loadBatchFilter() {
        $.ajax({
            url: '/api/lookup/batches',
            method: 'GET',
            success: function (batches) {
                const sel = $('#documentBatchFilter');
                batches.forEach(function (b) {
                    sel.append('<option value="' + escapeHtml(b.code) + '">' +
                        escapeHtml(b.code + ' (' + b.name + ')') + '</option>');
                });
            }
        });
    }

    function loadSectionFilter() {
        $.ajax({
            url: '/api/registrar/sections',
            method: 'GET',
            success: function (sections) {
                const sel = $('#documentSectionFilter');
                sections.forEach(function (s) {
                    sel.append('<option value="' + escapeHtml(s.sectionCode) + '">' +
                        escapeHtml(s.sectionCode + ' — ' + (s.sectionName || '')) + '</option>');
                });
            }
        });
    }

    function loadStudentsDatalist() {
        $.ajax({
            url: '/api/registrar/student-records',
            method: 'GET',
            success: function (students) {
                const list = document.getElementById('studentsDatalist');
                students.forEach(function (s) {
                    const option = document.createElement('option');
                    option.value = s.studentId;
                    option.label = s.lastName + ', ' + s.firstName;
                    list.appendChild(option);
                });
            }
        });
    }

    // -------------------------------------------------------
    // Upload (R3.1, R3.2)
    // -------------------------------------------------------

    function setupUpload() {
        uploadModal = new bootstrap.Modal(document.getElementById('uploadDocumentModal'));

        $('#uploadDocumentModal').on('show.bs.modal', function () {
            hideAlert('uploadDocumentAlert');
            $('#uploadStudentId').val('');
            $('#uploadDocumentType').val('');
            $('#uploadDocumentFile').val('');
        });

        $('#saveUploadDocumentBtn').on('click', function () {
            const studentId = $('#uploadStudentId').val().trim();
            const documentType = $('#uploadDocumentType').val();
            const fileInput = document.getElementById('uploadDocumentFile');
            const file = fileInput.files[0];

            if (!studentId || !documentType || !file) {
                showAlert('uploadDocumentAlert', 'Please fill in all required fields.', 'danger');
                return;
            }
            if (file.size > 10 * 1024 * 1024) {
                showAlert('uploadDocumentAlert', 'File exceeds the 10MB size limit.', 'danger');
                return;
            }

            const formData = new FormData();
            formData.append('studentId', studentId);
            formData.append('documentType', documentType);
            formData.append('file', file);

            const btn = $(this);
            btn.prop('disabled', true).text('Uploading...');

            $.ajax({
                url: '/api/registrar/documents',
                method: 'POST',
                data: formData,
                processData: false,
                contentType: false,
                success: function () {
                    btn.prop('disabled', false).text('Upload');
                    uploadModal.hide();
                    reloadTable();
                },
                error: function (xhr) {
                    btn.prop('disabled', false).text('Upload');
                    const msg = xhr.responseJSON?.message || 'Failed to upload the document.';
                    showAlert('uploadDocumentAlert', msg, 'danger');
                }
            });
        });
    }

    // -------------------------------------------------------
    // View + Download (R3.4, R3.7)
    // -------------------------------------------------------

    function setupViewAndDownload() {
        viewModal = new bootstrap.Modal(document.getElementById('viewDocumentModal'));
        const frame = document.getElementById('documentViewFrame');
        const notice = document.getElementById('viewDocumentNotice');

        $('#documentsTable').on('click', '.view-document-btn', function () {
            const id = $(this).data('id');
            const name = $(this).data('name');
            const mime = String($(this).data('mime') || '');
            const studentId = String($(this).data('student') || '');
            const documentType = String($(this).data('type') || '');

            $('#viewDocumentTitle').text(name);
            $('#viewDocumentDownloadBtn').attr('href', '/api/registrar/documents/' + id + '/download');

            // Only generated HTML documents can be re-opened in the generate page for editing.
            const editable = mime.indexOf('text/html') === 0;
            $('#viewDocumentEditBtn').toggleClass('d-none', !editable);
            if (editable) {
                $('#viewDocumentEditBtn').attr('href', 'generate-document.html'
                    + '?documentId=' + encodeURIComponent(id)
                    + '&studentId=' + encodeURIComponent(studentId)
                    + '&documentType=' + encodeURIComponent(documentType)
                    + '&fileName=' + encodeURIComponent(name));
            }

            const previewable = mime === 'application/pdf' || mime.indexOf('text/html') === 0;
            if (previewable) {
                notice.classList.add('d-none');
                frame.style.display = '';
                frame.src = '/api/registrar/documents/' + id + '/view';
            } else {
                frame.src = 'about:blank';
                frame.style.display = 'none';
                notice.textContent = 'This file type cannot be previewed in the browser. Use Download to open it.';
                notice.classList.remove('d-none');
            }
            viewModal.show();
        });

        document.getElementById('viewDocumentModal').addEventListener('hidden.bs.modal', function () {
            frame.src = 'about:blank';
        });

        $('#documentsTable').on('click', '.download-document-btn', function () {
            const id = $(this).data('id');
            window.location.href = '/api/registrar/documents/' + id + '/download';
        });
    }

    // -------------------------------------------------------
    // Delete (type-"delete"-to-confirm, same pattern as registrar.html)
    // -------------------------------------------------------

    function setupDelete() {
        deleteDocumentModal = new bootstrap.Modal(document.getElementById('deleteDocumentConfirmModal'));
        const confirmInput = document.getElementById('deleteDocumentConfirmInput');
        const confirmBtn = document.getElementById('confirmDeleteDocumentBtn');

        $('#documentsTable').on('click', '.delete-document-btn', function () {
            currentDeleteDocumentId = $(this).data('id');
            $('#deleteDocumentIdentifier').text(
                '"' + $(this).data('name') + '" of student ' + $(this).data('student'));
            confirmInput.value = '';
            confirmBtn.disabled = true;
            hideAlert('deleteDocumentResultAlert');
            deleteDocumentModal.show();
        });

        confirmInput.addEventListener('input', function () {
            confirmBtn.disabled = confirmInput.value.trim().toLowerCase() !== 'delete';
        });

        confirmBtn.addEventListener('click', function () {
            if (currentDeleteDocumentId == null) return;
            confirmBtn.disabled = true;

            $.ajax({
                url: '/api/registrar/documents/' + currentDeleteDocumentId,
                method: 'DELETE',
                success: function () {
                    deleteDocumentModal.hide();
                    reloadTable();
                },
                error: function (xhr) {
                    const msg = xhr.responseJSON?.message || 'Failed to delete the document.';
                    showAlert('deleteDocumentResultAlert', msg, 'danger');
                    confirmBtn.disabled = confirmInput.value.trim().toLowerCase() !== 'delete';
                }
            });
        });

        document.getElementById('deleteDocumentConfirmModal')
            .addEventListener('hidden.bs.modal', function () {
                currentDeleteDocumentId = null;
                confirmInput.value = '';
                confirmBtn.disabled = true;
                hideAlert('deleteDocumentResultAlert');
            });
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------

    function formatSize(bytes) {
        if (bytes == null) return '';
        if (bytes < 1024) return bytes + ' B';
        if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
        return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
    }

    function formatDateTime(value) {
        if (!value) return '<span class="text-muted fst-italic">Not Available</span>';
        const d = new Date(value);
        if (isNaN(d.getTime())) return escapeHtml(String(value));
        return d.toLocaleDateString() + ' ' + d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }

    function escapeHtml(str) {
        if (str === null || str === undefined) return '';
        const div = document.createElement('div');
        div.appendChild(document.createTextNode(String(str)));
        return div.innerHTML;
    }

    function showAlert(id, message, type) {
        const el = document.getElementById(id);
        el.className = 'alert alert-' + type;
        el.textContent = message;
        el.classList.remove('d-none');
    }

    function hideAlert(id) {
        const el = document.getElementById(id);
        el.className = 'alert d-none';
        el.textContent = '';
    }
})();