(function () {
    'use strict';

    let isDirty = false;
    let allowNavigation = false;
    let currentRecordId = null;

    // ----- Helpers -----

    function readRecordId() {
        const id = new URLSearchParams(window.location.search).get('id');
        return id ? Number(id) : null;
    }

    function setAlert(id, message, type) {
        const el = document.getElementById(id);
        if (!el) return;
        el.className = 'alert alert-' + type;
        el.textContent = message;
        el.classList.remove('d-none');
    }

    function hideAlert(id) {
        const el = document.getElementById(id);
        if (!el) return;
        el.classList.add('d-none');
        el.textContent = '';
    }

    function setVal(id, value) {
        const el = document.getElementById(id);
        if (!el) return;
        el.value = value === null || value === undefined ? '' : value;
    }

    function getVal(id) {
        const el = document.getElementById(id);
        return el ? el.value : '';
    }

    function getTrimmed(id) {
        return getVal(id).trim();
    }

    function getNullableInt(id) {
        const v = getTrimmed(id);
        if (v === '') return null;
        const n = Number(v);
        return Number.isFinite(n) ? n : null;
    }

    function getNullableDate(id) {
        const v = getTrimmed(id);
        return v === '' ? null : v;
    }

    function calculateAge(birthdateString) {
        if (!birthdateString) return null;
        const birth = new Date(birthdateString);
        if (isNaN(birth.getTime())) return null;
        const today = new Date();
        let age = today.getFullYear() - birth.getFullYear();
        const monthDiff = today.getMonth() - birth.getMonth();
        if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
            age--;
        }
        return age >= 0 ? age : null;
    }

    function updateAgeDisplay() {
        const display = document.getElementById('editAgeDisplay');
        if (!display) return;
        const age = calculateAge(getTrimmed('editBirthdate'));
        display.textContent = age == null ? '-' : age;
    }

    // ----- Dirty tracking -----

    function markDirty() {
        if (isDirty) return;
        isDirty = true;
        const notice = document.getElementById('unsavedNotice');
        if (notice) {
            notice.classList.remove('d-none');
        }
    }

    function clearDirty() {
        isDirty = false;
        const notice = document.getElementById('unsavedNotice');
        if (notice) {
            notice.classList.add('d-none');
        }
    }

    function setupDirtyTracking() {
        const form = document.getElementById('editRecordForm');
        if (!form) return;

        form.querySelectorAll('input, select, textarea').forEach(function (field) {
            if (field.disabled) return;
            field.addEventListener('input', markDirty);
            field.addEventListener('change', markDirty);
        });

        // Delegation covers dynamically-added school year rows
        form.addEventListener('input', markDirty);
        form.addEventListener('change', markDirty);

        window.addEventListener('beforeunload', function (event) {
            if (!isDirty || allowNavigation) return;
            event.preventDefault();
            event.returnValue = '';
        });

        const guardSelectors = [
            '.admin-nav-link',
            '.navbar-brand',
            '#cancelEditLink',
            '#logoutBtn',
            '#editAccountBtn'
        ];
        document.querySelectorAll(guardSelectors.join(',')).forEach(function (link) {
            link.addEventListener('click', function (event) {
                if (!isDirty || allowNavigation) return;
                if (!window.confirm('You have unsaved changes. Leave this page?')) {
                    event.preventDefault();
                } else {
                    allowNavigation = true;
                }
            });
        });
    }

    // ----- Lookup loaders -----

    async function loadOptions(url, datalistId) {
        try {
            const response = await fetch(url, { credentials: 'same-origin' });
            if (!response.ok) return;
            const items = await response.json();
            const datalist = document.getElementById(datalistId);
            if (!datalist) return;
            datalist.innerHTML = '';
            items.forEach(function (item) {
                const opt = document.createElement('option');
                opt.value = item.code;
                opt.label = item.code + ' — ' + item.name;
                opt.textContent = item.code + ' — ' + item.name;
                datalist.appendChild(opt);
            });
        } catch (error) {
            // Datalist stays empty; user can still type free text.
        }
    }

    async function loadAllLookups() {
        await Promise.all([
            loadOptions('/api/lookup/batches', 'batchList'),
            loadOptions('/api/lookup/courses', 'courseList'),
            loadOptions('/api/lookup/sections', 'sectionList')
        ]);
    }

    // ----- School year row management -----

    function createSchoolYearRow(sy) {
        const tr = document.createElement('tr');
        tr.innerHTML =
            '<td><input type="text" class="form-control form-control-sm sy-start" value="' + esc(sy.syStart) + '" placeholder="e.g. 2024"></td>' +
            '<td><input type="text" class="form-control form-control-sm sem-start" value="' + esc(sy.semStart) + '" placeholder="1st"></td>' +
            '<td><input type="text" class="form-control form-control-sm sy-end" value="' + esc(sy.syEnd) + '" placeholder="e.g. 2025"></td>' +
            '<td><input type="text" class="form-control form-control-sm sem-end" value="' + esc(sy.semEnd) + '" placeholder="2nd"></td>' +
            '<td><input type="text" class="form-control form-control-sm sy-remarks" value="' + esc(sy.remarks) + '"></td>' +
            '<td><button type="button" class="btn btn-sm btn-outline-danger remove-sy-row">Remove</button></td>';
        return tr;
    }

    function esc(v) {
        if (v === null || v === undefined) return '';
        return String(v)
            .replace(/&/g, '&amp;')
            .replace(/"/g, '&quot;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;');
    }

    function setupSchoolYearHandlers() {
        const addBtn = document.getElementById('addSchoolYearRowBtn');
        if (addBtn) {
            addBtn.addEventListener('click', function () {
                const body = document.getElementById('schoolYearsBody');
                if (body) {
                    body.appendChild(createSchoolYearRow({}));
                    markDirty();
                }
            });
        }

        const body = document.getElementById('schoolYearsBody');
        if (body) {
            body.addEventListener('click', function (event) {
                const btn = event.target.closest('.remove-sy-row');
                if (btn) {
                    btn.closest('tr').remove();
                    markDirty();
                }
            });
        }
    }

    // ----- Form population -----

    function populateForm(r) {
        currentRecordId = r.recordId;
        setVal('editRecordId', r.recordId);
        setVal('editStudentId', r.studentId);
        setVal('editStudentStatus', r.studentStatus);
        setVal('editLastName', r.lastName);
        setVal('editFirstName', r.firstName);
        setVal('editMiddleName', r.middleName);
        setVal('editBirthdate', r.birthdate || '');
        setVal('editSex', r.sex);
        setVal('editCivilStatus', r.civilStatus);
        setVal('editReligion', r.religion);
        setVal('editEmail', r.email);
        setVal('editContactNo', r.contactNo);
        setVal('editPermanentAddress', r.permanentAddress);
        setVal('editTemporaryAddress', r.temporaryAddress);
        setVal('editBaptized', r.baptized ? 'true' : 'false');
        setVal('editBaptismDate', r.baptismDate || '');
        setVal('editBaptismPlace', r.baptismPlace);
        setVal('editSiblingCount', r.siblingCount);
        setVal('editBrotherCount', r.brotherCount);
        setVal('editSisterCount', r.sisterCount);
        setVal('editBatchCode', r.batchCode);
        setVal('editCourseCode', r.courseCode);
        setVal('editSectionCode', r.sectionCode);
        setVal('editEnrollmentDate', r.enrollmentDate || 'Not set');
        updateAgeDisplay();

        // OJT
        const ojt = r.ojt || {};
        setVal('editOjtCompanyName', ojt.companyName);
        setVal('editOjtCompanyAddress', ojt.companyAddress);
        setVal('editOjtHoursRendered', ojt.hoursRendered !== null && ojt.hoursRendered !== undefined ? ojt.hoursRendered : '');

        // TESDA — 3 fixed slots, keyed by slot number
        const tesdaBySlot = {};
        (r.tesdaQualifications || []).forEach(function (q) { tesdaBySlot[q.slot] = q; });
        [1, 2, 3].forEach(function (slot) {
            const q = tesdaBySlot[slot] || {};
            setVal('editTesdaTitle' + slot, q.title);
            setVal('editTesdaCenter' + slot, q.centerAddress);
            setVal('editTesdaDate' + slot, q.assessmentDate || '');
            setVal('editTesdaResult' + slot, q.result);
        });

        // School Years — dynamic rows
        const syBody = document.getElementById('schoolYearsBody');
        if (syBody) {
            syBody.innerHTML = '';
            (r.schoolYears || []).forEach(function (sy) {
                syBody.appendChild(createSchoolYearRow(sy));
            });
        }
    }

    // ----- Payload builder -----

    function buildOjt() {
        const companyName = getTrimmed('editOjtCompanyName');
        const companyAddress = getTrimmed('editOjtCompanyAddress');
        const hoursStr = getTrimmed('editOjtHoursRendered');
        if (!companyName && !companyAddress && !hoursStr) return null;
        return {
            companyName: companyName || null,
            companyAddress: companyAddress || null,
            hoursRendered: hoursStr !== '' ? Number(hoursStr) : null
        };
    }

    function buildTesdaSlot(slot) {
        const title = getTrimmed('editTesdaTitle' + slot);
        const center = getTrimmed('editTesdaCenter' + slot);
        const date = getNullableDate('editTesdaDate' + slot);
        const result = getTrimmed('editTesdaResult' + slot);
        if (!title && !center && !date && !result) return null;
        return { slot: slot, title: title || null, centerAddress: center || null, assessmentDate: date, result: result || null };
    }

    function buildSchoolYearRows() {
        const rows = [];
        const body = document.getElementById('schoolYearsBody');
        if (!body) return rows;
        body.querySelectorAll('tr').forEach(function (tr) {
            const syStart = (tr.querySelector('.sy-start') || {}).value || '';
            const semStart = (tr.querySelector('.sem-start') || {}).value || '';
            const syEnd = (tr.querySelector('.sy-end') || {}).value || '';
            const semEnd = (tr.querySelector('.sem-end') || {}).value || '';
            const remarks = (tr.querySelector('.sy-remarks') || {}).value || '';
            if (!syStart.trim() && !semStart.trim() && !syEnd.trim() && !semEnd.trim() && !remarks.trim()) return;
            rows.push({
                rowIndex: rows.length + 1,
                syStart: syStart.trim() || null,
                semStart: semStart.trim() || null,
                syEnd: syEnd.trim() || null,
                semEnd: semEnd.trim() || null,
                remarks: remarks.trim() || null
            });
        });
        return rows;
    }

    function buildPayload() {
        return {
            studentId: getTrimmed('editStudentId'),
            lastName: getTrimmed('editLastName'),
            firstName: getTrimmed('editFirstName'),
            middleName: getTrimmed('editMiddleName'),
            birthdate: getNullableDate('editBirthdate'),
            sex: getTrimmed('editSex') || null,
            civilStatus: getTrimmed('editCivilStatus') || null,
            permanentAddress: getTrimmed('editPermanentAddress') || null,
            temporaryAddress: getTrimmed('editTemporaryAddress') || null,
            email: getTrimmed('editEmail') || null,
            contactNo: getTrimmed('editContactNo') || null,
            religion: getTrimmed('editReligion') || null,
            baptized: getVal('editBaptized') === 'true',
            baptismDate: getNullableDate('editBaptismDate'),
            baptismPlace: getTrimmed('editBaptismPlace') || null,
            siblingCount: getNullableInt('editSiblingCount'),
            brotherCount: getNullableInt('editBrotherCount'),
            sisterCount: getNullableInt('editSisterCount'),
            batchCode: getTrimmed('editBatchCode') || null,
            courseCode: getTrimmed('editCourseCode') || null,
            sectionCode: getTrimmed('editSectionCode') || null,
            studentStatus: getTrimmed('editStudentStatus'),
            ojt: buildOjt(),
            tesdaQualifications: [1, 2, 3].map(buildTesdaSlot).filter(function (s) { return s !== null; }),
            schoolYears: buildSchoolYearRows()
        };
    }

    // ----- Load and save -----

    async function loadRecord(recordId) {
        const response = await fetch('/api/registrar/student-records/' + encodeURIComponent(recordId), {
            credentials: 'same-origin'
        });
        if (!response.ok) {
            throw new Error('Failed to load student record.');
        }
        const data = await response.json();
        populateForm(data);
    }

    async function saveRecord() {
        hideAlert('recordEditAlert');

        const saveBtn = document.getElementById('saveRecordBtn');
        saveBtn.disabled = true;
        saveBtn.textContent = 'Saving...';

        try {
            const response = await fetch('/api/registrar/student-records/' + encodeURIComponent(currentRecordId), {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'same-origin',
                body: JSON.stringify(buildPayload())
            });

            const data = await response.json().catch(function () { return {}; });

            if (!response.ok) {
                const message = data.errors
                    ? Object.values(data.errors).join('. ')
                    : data.message || 'Unable to save the student record.';
                throw new Error(message);
            }

            clearDirty();
            allowNavigation = true;
            window.location.href = 'registrar.html?updated=true';
        } catch (error) {
            setAlert('recordEditAlert', error.message || 'Unable to save the student record.', 'danger');
            saveBtn.disabled = false;
            saveBtn.textContent = 'Save Changes';
        }
    }

    // ----- Boot -----

    document.addEventListener('DOMContentLoaded', async function () {
        const form = document.getElementById('editRecordForm');
        if (!form) return;

        const recordId = readRecordId();
        if (!recordId) {
            setAlert('recordEditAlert',
                'No student record was selected. Return to the registrar home and choose a record to edit.',
                'danger');
            const saveBtn = document.getElementById('saveRecordBtn');
            if (saveBtn) saveBtn.disabled = true;
            return;
        }

        const birthdateEl = document.getElementById('editBirthdate');
        if (birthdateEl) {
            birthdateEl.addEventListener('change', updateAgeDisplay);
            birthdateEl.addEventListener('input', updateAgeDisplay);
        }

        try {
            await loadAllLookups();
            await loadRecord(recordId);
        } catch (error) {
            setAlert('recordEditAlert', error.message || 'Unable to load the student record.', 'danger');
            return;
        }

        setupSchoolYearHandlers();
        setupDirtyTracking();

        const saveBtn = document.getElementById('saveRecordBtn');
        if (saveBtn) {
            saveBtn.addEventListener('click', saveRecord);
        }

        form.addEventListener('submit', function (event) {
            event.preventDefault();
            saveRecord();
        });
    });
})();
