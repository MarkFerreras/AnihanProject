/**
 * registrar-documents.js — Documents DataTable + folder explorer + atomic
 * multi-document upload + shared View/Download/Delete + search & filters.
 */
(function () {
    'use strict';

    // -------------------------------------------------------
    // Table state (preserved from the original single-tab page)
    // -------------------------------------------------------
    let documentsTable;
    let uploadModal = null;
    let viewModal = null;
    let deleteDocumentModal = null;
    let currentDeleteDocumentId = null;
    let documentTypeChoices = [];

    // -------------------------------------------------------
    // Explorer state
    // -------------------------------------------------------
    let folderHierarchy = [];
    let flatStudents = [];
    let selection = { kind: null, key: null };
    const expandedKeys = new Set();
    let studentRequestToken = 0;

    // -------------------------------------------------------
    // Upload staging state
    // -------------------------------------------------------
    let stagedFiles = []; // [{ file, documentType }]
    let uploadLockedStudentId = null;
    let uploadLockedLabel = null;
    let selectedUploadStudentId = null;

    const MAX_BATCH_FILES = 20;
    const MAX_FILE_BYTES = 10 * 1024 * 1024;
    const MAX_TOTAL_BYTES = 50 * 1024 * 1024;

    $(document).ready(function () {
        uploadModal = new bootstrap.Modal(document.getElementById('uploadDocumentModal'));
        viewModal = new bootstrap.Modal(document.getElementById('viewDocumentModal'));
        deleteDocumentModal = new bootstrap.Modal(document.getElementById('deleteDocumentConfirmModal'));

        initTable();
        loadDocumentTypes();
        loadBatchFilter();
        loadSectionFilter();
        setupFilters();
        setupUploadModal();
        setupUploadStaging();
        setupSharedActions();
        setupExplorerFilter();
        initExplorer();

        $('#tab-table').on('shown.bs.tab', function () {
            if (documentsTable) documentsTable.columns.adjust();
        });
    });

    // -------------------------------------------------------
    // DOM builder helper — avoids string-concatenated HTML/attrs for the
    // dynamic explorer content (escapeHtml alone is unsafe for attributes).
    // -------------------------------------------------------

    function el(tag, attrs, children) {
        const node = document.createElement(tag);
        if (attrs) {
            Object.keys(attrs).forEach(function (key) {
                if (key === 'class') node.className = attrs[key];
                else if (key === 'text') node.textContent = attrs[key];
                else node.setAttribute(key, attrs[key]);
            });
        }
        (children || []).forEach(function (child) {
            if (child) node.appendChild(child);
        });
        return node;
    }

    // -------------------------------------------------------
    // All Documents Table (unchanged behavior)
    // -------------------------------------------------------

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

    /** Refreshes both surfaces after an upload/delete, preserving explorer selection/expansion/filter. */
    function refreshAfterMutation() {
        reloadTable();
        fetchFolderTree(function () {
            renderFolderTree();
            renderBreadcrumb();
            renderContentPanel();
        });
    }

    // -------------------------------------------------------
    // Filters
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
                types.forEach(function (t) {
                    filterSel.append('<option value="' + escapeHtml(t) + '">' + escapeHtml(t) + '</option>');
                });
                // The ID picture is uploaded from the student record screens
                // (student-records.html), not from this page's bulk upload.
                documentTypeChoices = types.filter(function (t) { return t !== 'ID Picture (1x1 / 2x2)'; });
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

    // -------------------------------------------------------
    // Folder Explorer — data fetch + key helpers
    // -------------------------------------------------------

    function initExplorer() {
        const content = document.getElementById('folderContent');
        content.appendChild(el('p', { class: 'text-muted', text: 'Loading folders…' }));
        fetchFolderTree(function () {
            renderFolderTree();
            renderBreadcrumb();
            renderContentPanel();
        });
    }

    function fetchFolderTree(onDone) {
        $.ajax({
            url: '/api/registrar/documents/folders/tree',
            method: 'GET',
            success: function (data) {
                folderHierarchy = data;
                rebuildFlatStudentsAndDatalist();
                if (onDone) onDone();
            },
            error: function (xhr) {
                const content = document.getElementById('folderContent');
                content.innerHTML = '';
                if (xhr.status === 401) {
                    content.appendChild(buildSessionExpiredNotice());
                } else {
                    content.appendChild(buildRetryableError(
                        'Failed to load the document folders.',
                        function () { fetchFolderTree(onDone); }));
                }
            }
        });
    }

    function batchNodeKey(batch) {
        return batch.batchCode === null ? 'no-batch' : batch.batchCode;
    }

    function isSelected(kind, key) {
        return selection.kind === kind && selection.key === key;
    }

    function findBatchByKey(key) {
        return folderHierarchy.find(function (b) { return batchNodeKey(b) === key; }) || null;
    }

    function findStudentAnywhere(studentId) {
        for (let i = 0; i < folderHierarchy.length; i++) {
            const batch = folderHierarchy[i];
            for (let j = 0; j < batch.sections.length; j++) {
                const section = batch.sections[j];
                const found = section.students.find(function (s) { return s.studentId === studentId; });
                if (found) return { batch: batch, section: section, student: found };
            }
            const found = batch.unassignedStudents.find(function (s) { return s.studentId === studentId; });
            if (found) return { batch: batch, section: null, student: found };
        }
        return null;
    }

    function resolveSelectionContext() {
        if (selection.kind === 'batch') {
            const batch = findBatchByKey(selection.key);
            return batch ? { batch: batch } : null;
        }
        if (selection.kind === 'section') {
            for (let i = 0; i < folderHierarchy.length; i++) {
                const batch = folderHierarchy[i];
                const section = batch.sections.find(function (s) { return s.sectionCode === selection.key; });
                if (section) return { batch: batch, section: section };
            }
            return null;
        }
        if (selection.kind === 'unassigned') {
            const batch = findBatchByKey(selection.key);
            return batch ? { batch: batch, isUnassigned: true } : null;
        }
        if (selection.kind === 'student') {
            const found = findStudentAnywhere(selection.key);
            return found
                ? { batch: found.batch, section: found.section, isUnassigned: !found.section, student: found.student }
                : null;
        }
        return null;
    }

    // -------------------------------------------------------
    // Folder Explorer — tree rendering + filtering
    // -------------------------------------------------------

    function setupExplorerFilter() {
        let debounce = null;
        $('#explorerFilterInput').on('input', function () {
            window.clearTimeout(debounce);
            debounce = window.setTimeout(renderFolderTree, 200);
        });
    }

    function computeFilteredHierarchy(query) {
        if (!query) return folderHierarchy;
        const q = query.toLowerCase();
        const matches = function (text) { return !!text && text.toLowerCase().indexOf(q) !== -1; };
        const studentMatches = function (s) {
            return matches(s.firstName) || matches(s.lastName) || matches(s.studentId) || matches(s.studentNumber);
        };

        return folderHierarchy.map(function (batch) {
            const batchSelfMatch = matches(batch.batchCode) || (batch.batchCode === null && matches('no batch'));

            const filteredSections = batch.sections.map(function (section) {
                const sectionSelfMatch = matches(section.sectionCode) || matches(section.sectionName);
                const students = section.students.filter(studentMatches);
                if (batchSelfMatch || sectionSelfMatch || students.length) {
                    return Object.assign({}, section, {
                        students: (batchSelfMatch || sectionSelfMatch) ? section.students : students
                    });
                }
                return null;
            }).filter(Boolean);

            const filteredUnassigned = (batchSelfMatch ? batch.unassignedStudents
                : batch.unassignedStudents.filter(studentMatches));

            if (batchSelfMatch || filteredSections.length || filteredUnassigned.length) {
                return Object.assign({}, batch, { sections: filteredSections, unassignedStudents: filteredUnassigned });
            }
            return null;
        }).filter(Boolean);
    }

    function renderFolderTree() {
        const query = ($('#explorerFilterInput').val() || '').trim();
        const data = computeFilteredHierarchy(query);
        const forceExpand = !!query;
        const root = document.getElementById('folderTree');
        root.innerHTML = '';
        if (!data.length) {
            root.appendChild(el('li', { class: 'text-muted small px-2 py-1', text: query ? 'No matches.' : 'No batches yet.' }));
            return;
        }
        data.forEach(function (batch) { root.appendChild(buildBatchNode(batch, forceExpand)); });
    }

    function buildNodeRow(kind, key, label, countText, expandable, expanded, onToggle) {
        const row = el('div', {
            class: 'folder-node' + (isSelected(kind, key) ? ' selected' : ''),
            role: 'button',
            tabindex: '0'
        });
        if (expandable) {
            const toggle = el('button', {
                type: 'button',
                class: 'folder-toggle',
                'aria-expanded': expanded ? 'true' : 'false',
                'aria-label': (expanded ? 'Collapse ' : 'Expand ') + label,
                text: expanded ? '▾' : '▸'
            });
            toggle.addEventListener('click', function (evt) {
                evt.stopPropagation();
                onToggle();
            });
            row.appendChild(toggle);
        } else {
            row.appendChild(el('span', { class: 'folder-toggle', 'aria-hidden': 'true', text: ' ' }));
        }
        row.appendChild(el('span', { class: 'folder-label', text: label }));
        row.appendChild(el('span', { class: 'folder-count', text: countText }));
        row.addEventListener('click', function () { selectFolder(kind, key); });
        row.addEventListener('keydown', function (evt) {
            if (evt.key === 'Enter' || evt.key === ' ') {
                evt.preventDefault();
                selectFolder(kind, key);
            }
        });
        return row;
    }

    function buildBatchNode(batch, forceExpand) {
        const key = batchNodeKey(batch);
        const trackKey = 'batch:' + key;
        const expanded = forceExpand || expandedKeys.has(trackKey);
        const label = batch.batchCode ? (batch.batchCode + (batch.batchYear ? ' (' + batch.batchYear + ')' : '')) : 'No Batch';
        const li = el('li', { 'data-node-kind': 'batch', 'data-node-key': key });
        li.appendChild(buildNodeRow('batch', key, label,
            batch.studentCount + ' students, ' + batch.documentCount + ' docs',
            true, expanded, function () { toggleExpand(trackKey); }));
        if (expanded) {
            const ul = el('ul');
            batch.sections.forEach(function (section) { ul.appendChild(buildSectionNode(section, forceExpand)); });
            ul.appendChild(buildUnassignedNode(batch, forceExpand));
            li.appendChild(ul);
        }
        return li;
    }

    function buildSectionNode(section, forceExpand) {
        const key = section.sectionCode;
        const trackKey = 'section:' + key;
        const expanded = forceExpand || expandedKeys.has(trackKey);
        const label = section.sectionCode + ' — ' + (section.sectionName || '');
        const li = el('li', { 'data-node-kind': 'section', 'data-node-key': key });
        li.appendChild(buildNodeRow('section', key, label,
            section.studentCount + ' students, ' + section.documentCount + ' docs',
            true, expanded, function () { toggleExpand(trackKey); }));
        if (expanded) {
            const ul = el('ul');
            section.students.forEach(function (student) { ul.appendChild(buildStudentNode(student)); });
            li.appendChild(ul);
        }
        return li;
    }

    function buildUnassignedNode(batch, forceExpand) {
        const key = batchNodeKey(batch);
        const trackKey = 'unassigned:' + key;
        const expanded = forceExpand || expandedKeys.has(trackKey);
        const docCount = batch.unassignedStudents.reduce(function (sum, s) { return sum + s.documentCount; }, 0);
        const li = el('li', { 'data-node-kind': 'unassigned', 'data-node-key': key });
        li.appendChild(buildNodeRow('unassigned', key, 'Unassigned Students',
            batch.unassignedStudents.length + ' students, ' + docCount + ' docs',
            true, expanded, function () { toggleExpand(trackKey); }));
        if (expanded) {
            const ul = el('ul');
            batch.unassignedStudents.forEach(function (student) { ul.appendChild(buildStudentNode(student)); });
            li.appendChild(ul);
        }
        return li;
    }

    function buildStudentNode(student) {
        const key = student.studentId;
        const label = student.lastName + ', ' + student.firstName;
        const li = el('li', { 'data-node-kind': 'student', 'data-node-key': key });
        li.appendChild(buildNodeRow('student', key, label, student.documentCount + ' docs', false, false, function () {}));
        return li;
    }

    function toggleExpand(trackKey) {
        if (expandedKeys.has(trackKey)) expandedKeys.delete(trackKey);
        else expandedKeys.add(trackKey);
        renderFolderTree();
    }

    function selectFolder(kind, key) {
        selection = { kind: kind, key: key };
        renderFolderTree();
        renderBreadcrumb();
        renderContentPanel();
    }

    // -------------------------------------------------------
    // Folder Explorer — breadcrumb
    // -------------------------------------------------------

    function makeCrumbLink(label, onClick) {
        const link = el('a', { href: '#', text: label });
        link.addEventListener('click', function (evt) { evt.preventDefault(); onClick(); });
        return link;
    }

    function renderBreadcrumb() {
        const bc = document.getElementById('folderBreadcrumb');
        bc.innerHTML = '';

        const rootCrumb = el('li', { class: 'breadcrumb-item' + (!selection.kind ? ' active' : '') });
        if (!selection.kind) {
            rootCrumb.textContent = 'All Batches';
        } else {
            rootCrumb.appendChild(makeCrumbLink('All Batches', function () {
                selection = { kind: null, key: null };
                renderFolderTree();
                renderBreadcrumb();
                renderContentPanel();
            }));
        }
        bc.appendChild(rootCrumb);

        if (!selection.kind) return;
        const ctx = resolveSelectionContext();
        if (!ctx) return;

        const batchLabel = ctx.batch.batchCode ? ctx.batch.batchCode : 'No Batch';
        const batchCrumb = el('li', { class: 'breadcrumb-item' + (selection.kind === 'batch' ? ' active' : '') });
        if (selection.kind === 'batch') {
            batchCrumb.textContent = batchLabel;
        } else {
            batchCrumb.appendChild(makeCrumbLink(batchLabel, function () { selectFolder('batch', batchNodeKey(ctx.batch)); }));
        }
        bc.appendChild(batchCrumb);

        if (ctx.section) {
            const crumb = el('li', { class: 'breadcrumb-item' + (selection.kind === 'section' ? ' active' : '') });
            if (selection.kind === 'section') {
                crumb.textContent = ctx.section.sectionCode;
            } else {
                crumb.appendChild(makeCrumbLink(ctx.section.sectionCode,
                    function () { selectFolder('section', ctx.section.sectionCode); }));
            }
            bc.appendChild(crumb);
        } else if (ctx.isUnassigned) {
            const crumb = el('li', { class: 'breadcrumb-item' + (selection.kind === 'unassigned' ? ' active' : '') });
            if (selection.kind === 'unassigned') {
                crumb.textContent = 'Unassigned Students';
            } else {
                crumb.appendChild(makeCrumbLink('Unassigned Students',
                    function () { selectFolder('unassigned', batchNodeKey(ctx.batch)); }));
            }
            bc.appendChild(crumb);
        }

        if (ctx.student) {
            bc.appendChild(el('li', {
                class: 'breadcrumb-item active',
                text: ctx.student.lastName + ', ' + ctx.student.firstName
            }));
        }
    }

    // -------------------------------------------------------
    // Folder Explorer — content panel per selection kind
    // -------------------------------------------------------

    function exportUrl(scope, key) {
        if (scope === 'student') return '/api/registrar/documents/export/student/' + encodeURIComponent(key);
        if (scope === 'section') return '/api/registrar/documents/export/section/' + encodeURIComponent(key);
        if (scope === 'unassignedBatch') return '/api/registrar/documents/export/batch/' + encodeURIComponent(key) + '/unassigned';
        return '/api/registrar/documents/export/batch/' + encodeURIComponent(key);
    }

    function buildExportButton(label, url, disabled) {
        if (disabled) {
            return el('button', { type: 'button', class: 'btn btn-surface-secondary disabled', 'aria-disabled': 'true', disabled: 'disabled', text: label });
        }
        return el('a', { href: url, class: 'btn btn-primary', text: label });
    }

    function exportNote() {
        return el('div', {
            class: 'export-note',
            text: 'ZIP exports download the original stored files (including generated HTML and ID photos), '
                + 'unlike the single-document Download button, which converts a generated document to an editable Word file.'
        });
    }

    function renderContentPanel() {
        const content = document.getElementById('folderContent');
        content.innerHTML = '';
        if (!selection.kind) {
            content.appendChild(el('p', { class: 'text-muted', text: 'Select a batch, section, or student on the left to begin.' }));
            return;
        }
        const ctx = resolveSelectionContext();
        if (!ctx) {
            content.appendChild(el('p', { class: 'text-muted', text: 'This item is no longer available. It may have been reassigned — refresh to continue.' }));
            return;
        }
        if (selection.kind === 'batch') renderBatchOverview(ctx.batch);
        else if (selection.kind === 'section') renderStudentListView(ctx.section.students,
            'Section ' + ctx.section.sectionCode, { url: exportUrl('section', ctx.section.sectionCode), label: 'Export Section' });
        else if (selection.kind === 'unassigned') renderStudentListView(ctx.batch.unassignedStudents,
            'Unassigned Students',
            ctx.batch.batchCode ? { url: exportUrl('unassignedBatch', batchNodeKey(ctx.batch)), label: 'Export Unassigned' } : null);
        else if (selection.kind === 'student') renderStudentDetail(ctx.student, ctx.batch, ctx.section);
    }

    function buildFolderCard(kind, key, title, subtitle) {
        const col = el('div', { class: 'col-md-6' });
        const card = el('div', { class: 'folder-card', role: 'button', tabindex: '0' }, [
            el('h4', { class: 'h6 mb-1', text: title }),
            el('p', { class: 'text-muted small mb-0', text: subtitle })
        ]);
        card.addEventListener('click', function () { selectFolder(kind, key); });
        card.addEventListener('keydown', function (evt) {
            if (evt.key === 'Enter' || evt.key === ' ') { evt.preventDefault(); selectFolder(kind, key); }
        });
        col.appendChild(card);
        return col;
    }

    function renderBatchOverview(batch) {
        const content = document.getElementById('folderContent');
        const header = el('div', { class: 'd-flex justify-content-between align-items-start mb-3 flex-wrap gap-2' });
        header.appendChild(el('h3', {
            class: 'h5 mb-0',
            text: batch.batchCode ? (batch.batchCode + (batch.batchYear ? ' (' + batch.batchYear + ')' : '')) : 'No Batch'
        }));
        if (batch.batchCode) {
            const wrap = el('div');
            wrap.appendChild(buildExportButton('Export Entire Batch', exportUrl('batch', batch.batchCode), batch.documentCount === 0));
            wrap.appendChild(exportNote());
            header.appendChild(wrap);
        }
        content.appendChild(header);

        const row = el('div', { class: 'row g-3' });
        batch.sections.forEach(function (section) {
            row.appendChild(buildFolderCard('section', section.sectionCode,
                section.sectionCode + ' — ' + (section.sectionName || ''),
                section.studentCount + ' students, ' + section.documentCount + ' documents'));
        });
        const unassignedDocs = batch.unassignedStudents.reduce(function (s, x) { return s + x.documentCount; }, 0);
        row.appendChild(buildFolderCard('unassigned', batchNodeKey(batch), 'Unassigned Students',
            batch.unassignedStudents.length + ' students, ' + unassignedDocs + ' documents'));
        content.appendChild(row);
    }

    function renderStudentListView(students, title, exportConfig) {
        const content = document.getElementById('folderContent');
        const header = el('div', { class: 'd-flex justify-content-between align-items-start mb-3 flex-wrap gap-2' });
        header.appendChild(el('h3', { class: 'h5 mb-0', text: title }));
        if (exportConfig) {
            const totalDocs = students.reduce(function (s, x) { return s + x.documentCount; }, 0);
            const wrap = el('div');
            wrap.appendChild(buildExportButton(exportConfig.label, exportConfig.url, totalDocs === 0));
            wrap.appendChild(exportNote());
            header.appendChild(wrap);
        }
        content.appendChild(header);

        if (!students.length) {
            content.appendChild(el('p', { class: 'text-muted', text: 'No students here yet.' }));
            return;
        }
        const list = el('div');
        students.forEach(function (student) {
            const row = el('div', { class: 'folder-card mb-2', role: 'button', tabindex: '0' }, [
                el('div', { class: 'd-flex justify-content-between align-items-center' }, [
                    el('span', { text: student.lastName + ', ' + student.firstName }),
                    el('span', { class: 'text-muted small', text: student.documentCount + ' documents' })
                ])
            ]);
            row.addEventListener('click', function () { selectFolder('student', student.studentId); });
            row.addEventListener('keydown', function (evt) {
                if (evt.key === 'Enter' || evt.key === ' ') { evt.preventDefault(); selectFolder('student', student.studentId); }
            });
            list.appendChild(row);
        });
        content.appendChild(list);
    }

    function renderStudentDetail(student, batch, section) {
        const content = document.getElementById('folderContent');

        const header = el('div', { class: 'd-flex justify-content-between align-items-start mb-3 flex-wrap gap-2' });
        const left = el('div');
        left.appendChild(el('h3', { class: 'h5 mb-1', text: student.lastName + ', ' + student.firstName }));
        const refLine = el('p', { class: 'mb-2 small text-muted' });
        refLine.appendChild(document.createTextNode('Reference No.: ' + student.studentId));
        if (student.studentNumber) {
            refLine.appendChild(document.createTextNode(' · Student Number: ' + student.studentNumber));
        }
        left.appendChild(refLine);
        const badges = el('div', { class: 'd-flex gap-2 flex-wrap' });
        badges.appendChild(el('span', { class: 'status-badge status-badge-active', text: student.studentStatus || 'Unknown' }));
        if (!section) badges.appendChild(el('span', { class: 'no-assignment-badge', text: 'No Section Assigned' }));
        if (!batch.batchCode) badges.appendChild(el('span', { class: 'no-assignment-badge', text: 'No Batch Assigned' }));
        left.appendChild(badges);
        header.appendChild(left);

        const right = el('div', { class: 'text-end' });
        right.appendChild(buildExportButton('Export Student', exportUrl('student', student.studentId), student.documentCount === 0));
        const uploadBtn = el('button', { type: 'button', class: 'btn btn-surface mt-2', text: 'Upload for this Student' });
        uploadBtn.addEventListener('click', function () {
            openUploadForStudent(student.studentId, student.lastName + ', ' + student.firstName);
        });
        right.appendChild(el('div', { class: 'mt-2' }, [uploadBtn]));
        header.appendChild(right);
        content.appendChild(header);
        content.appendChild(exportNote());

        const listContainer = el('div', { id: 'studentDocList', class: 'mt-3' });
        listContainer.appendChild(el('p', { class: 'text-muted', text: 'Loading documents…' }));
        content.appendChild(listContainer);

        loadStudentDocuments(student.studentId, listContainer);
    }

    function loadStudentDocuments(studentId, container) {
        const token = ++studentRequestToken;
        $.ajax({
            url: '/api/registrar/documents/student/' + encodeURIComponent(studentId),
            method: 'GET',
            success: function (docs) {
                if (token !== studentRequestToken) return; // a newer selection has since been made
                renderStudentDocList(container, docs);
            },
            error: function (xhr) {
                if (token !== studentRequestToken) return;
                container.innerHTML = '';
                if (xhr.status === 401) {
                    container.appendChild(buildSessionExpiredNotice());
                } else {
                    container.appendChild(buildRetryableError(
                        "Failed to load this student's documents.",
                        function () { loadStudentDocuments(studentId, container); }));
                }
            }
        });
    }

    function buildActionButton(cls, label, dataAttrs) {
        const btn = el('button', { type: 'button', class: 'btn btn-surface btn-sm ' + cls, text: label });
        Object.keys(dataAttrs).forEach(function (key) { btn.setAttribute(key, dataAttrs[key]); });
        return btn;
    }

    function renderStudentDocList(container, docs) {
        container.innerHTML = '';
        if (!docs.length) {
            container.appendChild(el('p', { class: 'text-muted', text: 'No documents uploaded yet.' }));
            return;
        }
        docs.forEach(function (doc) {
            const row = el('div', { class: 'student-doc-row document-actions' });
            row.appendChild(el('span', {
                class: 'flex-grow-1',
                text: doc.fileName + ' (' + doc.documentType + ', ' + formatSize(doc.fileSize) + ')'
            }));
            const actions = el('div', { class: 'd-flex gap-1' });
            actions.appendChild(buildActionButton('view-document-btn', 'View', {
                'data-id': doc.documentId, 'data-name': doc.fileName, 'data-mime': doc.fileType,
                'data-student': doc.studentId, 'data-type': doc.documentType
            }));
            actions.appendChild(buildActionButton('download-document-btn', 'Download', { 'data-id': doc.documentId }));
            const deleteBtn = buildActionButton('delete-document-btn', 'Delete', {
                'data-id': doc.documentId, 'data-name': doc.fileName, 'data-student': doc.studentId
            });
            deleteBtn.classList.add('btn-danger-surface');
            actions.appendChild(deleteBtn);
            row.appendChild(actions);
            container.appendChild(row);
        });
    }

    // -------------------------------------------------------
    // Loading / error / session-expired helpers
    // -------------------------------------------------------

    function buildSessionExpiredNotice() {
        const box = el('div', { class: 'alert alert-warning' });
        box.appendChild(document.createTextNode('Your session has expired. '));
        box.appendChild(el('a', { href: 'index.html', text: 'Log in again' }));
        return box;
    }

    function buildRetryableError(message, retryFn) {
        const box = el('div', { class: 'alert alert-danger d-flex justify-content-between align-items-center gap-2' });
        box.appendChild(el('span', { text: message }));
        const btn = el('button', { type: 'button', class: 'btn btn-sm btn-surface-secondary', text: 'Retry' });
        btn.addEventListener('click', retryFn);
        box.appendChild(btn);
        return box;
    }

    // -------------------------------------------------------
    // Upload — student combobox (explicit selection) + datalist from tree
    // -------------------------------------------------------

    function studentOptionLabel(s) {
        return s.lastName + ', ' + s.firstName + ' — ' + (s.studentNumber ? s.studentNumber : s.studentId) + ' (Ref: ' + s.studentId + ')';
    }

    function rebuildFlatStudentsAndDatalist() {
        flatStudents = [];
        folderHierarchy.forEach(function (batch) {
            batch.sections.forEach(function (section) { flatStudents = flatStudents.concat(section.students); });
            flatStudents = flatStudents.concat(batch.unassignedStudents);
        });
        const list = document.getElementById('studentsDatalist');
        list.innerHTML = '';
        flatStudents.forEach(function (s) {
            const option = document.createElement('option');
            option.value = studentOptionLabel(s);
            list.appendChild(option);
        });
    }

    /** Opens the shared upload modal locked to one student (explorer entry point). */
    function openUploadForStudent(studentId, label) {
        uploadLockedStudentId = studentId;
        uploadLockedLabel = label;
        uploadModal.show();
    }

    function setupUploadModal() {
        $('#uploadDocumentModal').on('show.bs.modal', function () {
            hideAlert('uploadDocumentAlert');
            stagedFiles = [];
            renderStagedFiles();
            $('#uploadDocumentFile').val('');

            if (uploadLockedStudentId) {
                $('#uploadStudentId').val(uploadLockedLabel).prop('readonly', true);
                $('#uploadStudentHelp').text('Uploading for ' + uploadLockedLabel + ' (' + uploadLockedStudentId + ').');
                selectedUploadStudentId = uploadLockedStudentId;
            } else {
                $('#uploadStudentId').val('').prop('readonly', false);
                $('#uploadStudentHelp').text('Pick the student the documents belong to.');
                selectedUploadStudentId = null;
            }
            updateUploadSubmitState();
        });

        $('#uploadDocumentModal').on('hidden.bs.modal', function () {
            uploadLockedStudentId = null;
            uploadLockedLabel = null;
        });

        $('#uploadStudentId').on('input', function () {
            if (uploadLockedStudentId) return;
            const typed = $(this).val();
            const match = flatStudents.filter(function (s) { return studentOptionLabel(s) === typed; });
            selectedUploadStudentId = match.length === 1 ? match[0].studentId : null;
            updateUploadSubmitState();
        });
    }

    // -------------------------------------------------------
    // Upload — file staging (chooser + drag/drop), limits, submission
    // -------------------------------------------------------

    function setupUploadStaging() {
        const fileInput = document.getElementById('uploadDocumentFile');
        const dropZone = document.getElementById('uploadDropZone');

        fileInput.addEventListener('change', function () {
            addStagedFiles(Array.prototype.slice.call(fileInput.files));
            fileInput.value = '';
        });

        dropZone.addEventListener('dragover', function (evt) {
            evt.preventDefault();
            dropZone.classList.add('drag-over');
        });
        dropZone.addEventListener('dragleave', function () {
            dropZone.classList.remove('drag-over');
        });
        dropZone.addEventListener('drop', function (evt) {
            evt.preventDefault();
            dropZone.classList.remove('drag-over');
            if (evt.dataTransfer && evt.dataTransfer.files) {
                addStagedFiles(Array.prototype.slice.call(evt.dataTransfer.files));
            }
        });

        $('#saveUploadDocumentBtn').on('click', submitBatchUpload);
    }

    function addStagedFiles(files) {
        files.forEach(function (file) {
            stagedFiles.push({ file: file, documentType: 'Others' });
        });
        renderStagedFiles();
    }

    function removeStagedFile(index) {
        stagedFiles.splice(index, 1);
        renderStagedFiles();
    }

    function renderStagedFiles() {
        const container = document.getElementById('stagedFilesList');
        container.innerHTML = '';
        let totalBytes = 0;

        stagedFiles.forEach(function (entry, index) {
            totalBytes += entry.file.size;
            const row = el('div', { class: 'staged-file-row' });
            row.appendChild(el('span', {
                class: 'staged-file-name',
                text: entry.file.name + ' (' + formatSize(entry.file.size) + ')'
            }));

            const select = document.createElement('select');
            select.className = 'form-select form-select-sm';
            documentTypeChoices.forEach(function (type) {
                const opt = document.createElement('option');
                opt.value = type;
                opt.textContent = type;
                if (type === entry.documentType) opt.selected = true;
                select.appendChild(opt);
            });
            select.addEventListener('change', function () { entry.documentType = select.value; });
            row.appendChild(select);

            const removeBtn = el('button', { type: 'button', class: 'btn btn-sm btn-surface-secondary', text: 'Remove' });
            removeBtn.addEventListener('click', function () { removeStagedFile(index); });
            row.appendChild(removeBtn);

            container.appendChild(row);
        });

        $('#uploadStagedSummary').text(stagedFiles.length + ' file' + (stagedFiles.length === 1 ? '' : 's') + ', ' + formatSize(totalBytes));
        updateUploadSubmitState();
    }

    function updateUploadSubmitState() {
        const totalBytes = stagedFiles.reduce(function (sum, e) { return sum + e.file.size; }, 0);
        const tooMany = stagedFiles.length > MAX_BATCH_FILES;
        const anyTooBig = stagedFiles.some(function (e) { return e.file.size > MAX_FILE_BYTES; });
        const tooMuch = totalBytes > MAX_TOTAL_BYTES;
        const valid = !!selectedUploadStudentId && stagedFiles.length > 0 && !tooMany && !anyTooBig && !tooMuch;

        $('#saveUploadDocumentBtn').prop('disabled', !valid);

        hideAlert('uploadDocumentAlert');
        if (tooMany) {
            showAlert('uploadDocumentAlert', 'A maximum of ' + MAX_BATCH_FILES + ' files can be uploaded at once.', 'warning');
        } else if (anyTooBig) {
            showAlert('uploadDocumentAlert', 'Each file must be 10MB or smaller.', 'warning');
        } else if (tooMuch) {
            showAlert('uploadDocumentAlert', 'The combined upload size must be 50MB or smaller.', 'warning');
        }
    }

    function submitBatchUpload() {
        if (!selectedUploadStudentId || !stagedFiles.length) return;

        const formData = new FormData();
        formData.append('studentId', selectedUploadStudentId);
        stagedFiles.forEach(function (entry) {
            formData.append('files', entry.file);
            formData.append('documentTypes', entry.documentType);
        });

        const btn = $('#saveUploadDocumentBtn');
        btn.prop('disabled', true).text('Uploading...');

        $.ajax({
            url: '/api/registrar/documents/batch',
            method: 'POST',
            data: formData,
            processData: false,
            contentType: false,
            success: function () {
                btn.text('Upload');
                uploadModal.hide();
                stagedFiles = []; // completed upload can never be resubmitted
                refreshAfterMutation();
            },
            error: function (xhr) {
                btn.prop('disabled', false).text('Upload');
                let msg = 'Failed to upload the document(s).';
                if (xhr.responseJSON && xhr.responseJSON.message) {
                    msg = xhr.responseJSON.message;
                } else if (xhr.status === 0 || xhr.status === 413) {
                    msg = 'Upload failed — the request may exceed the server\'s size limit. Try fewer or smaller files.';
                }
                showAlert('uploadDocumentAlert', msg, 'danger');
                // Failed upload preserves staging so the registrar can retry.
            }
        });
    }

    // -------------------------------------------------------
    // Shared View / Download / Delete — delegated from the page root so
    // both the table and the explorer's rendered rows use one handler.
    // -------------------------------------------------------

    function setupSharedActions() {
        const frame = document.getElementById('documentViewFrame');
        const notice = document.getElementById('viewDocumentNotice');

        $('#documentsPageRoot').on('click', '.view-document-btn', function () {
            const id = $(this).data('id');
            const name = $(this).data('name');
            const mime = String($(this).data('mime') || '');
            const studentId = String($(this).data('student') || '');
            const documentType = String($(this).data('type') || '');

            $('#viewDocumentTitle').text(name);
            $('#viewDocumentDownloadBtn').attr('href', '/api/registrar/documents/' + id + '/download');

            const editable = mime.indexOf('text/html') === 0;
            $('#viewDocumentEditBtn').toggleClass('d-none', !editable);
            if (editable) {
                $('#viewDocumentEditBtn').attr('href', 'generate-document.html'
                    + '?documentId=' + encodeURIComponent(id)
                    + '&studentId=' + encodeURIComponent(studentId)
                    + '&documentType=' + encodeURIComponent(documentType)
                    + '&fileName=' + encodeURIComponent(name));
            }

            const previewable = mime === 'application/pdf'
                || mime.indexOf('text/html') === 0
                || mime.indexOf('image/') === 0;
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

        $('#documentsPageRoot').on('click', '.download-document-btn', function () {
            const id = $(this).data('id');
            window.location.href = '/api/registrar/documents/' + id + '/download';
        });

        setupDelete();
    }

    function setupDelete() {
        const confirmInput = document.getElementById('deleteDocumentConfirmInput');
        const confirmBtn = document.getElementById('confirmDeleteDocumentBtn');

        $('#documentsPageRoot').on('click', '.delete-document-btn', function () {
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
                    refreshAfterMutation();
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
        const node = document.getElementById(id);
        node.className = 'alert alert-' + type;
        node.textContent = message;
        node.classList.remove('d-none');
    }

    function hideAlert(id) {
        const node = document.getElementById(id);
        node.className = 'alert d-none';
        node.textContent = '';
    }
})();
