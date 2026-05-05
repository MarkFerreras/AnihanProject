'use strict';

// ─── State ──────────────────────────────────────────────────────────────────
let studentId = null;
let currentStep = 1;
const TOTAL_STEPS = 4;
const REDIRECT_DELAY_MS = 5000;

// Required fields per step — used for both step-level and full-submit validation.
const STEP_REQUIRED = {
    1: [
        { id: 'contactNo',        label: 'Contact Number' },
        { id: 'birthdate',        label: 'Birthdate' },
        { id: 'sex',              label: 'Sex' },
        { id: 'civilStatus',      label: 'Civil Status' },
        { id: 'permanentAddress', label: 'Permanent Address' },
    ],
    2: [{ id: 'religion', label: 'Religion' }],
    3: [
        { id: 'fatherFamilyName', label: "Father's Family Name" },
        { id: 'fatherFirstName',  label: "Father's First Name" },
        { id: 'fatherBirthdate',  label: "Father's Birthdate" },
        { id: 'fatherOccupation', label: "Father's Occupation" },
        { id: 'fatherContactNo',  label: "Father's Contact No." },
        { id: 'fatherAddress',    label: "Father's Address" },
        { id: 'motherFamilyName', label: "Mother's Family Name" },
        { id: 'motherFirstName',  label: "Mother's First Name" },
        { id: 'motherBirthdate',  label: "Mother's Birthdate" },
        { id: 'motherOccupation', label: "Mother's Occupation" },
        { id: 'motherContactNo',  label: "Mother's Contact No." },
        { id: 'motherAddress',    label: "Mother's Address" },
    ],
    4: [],
};

// Custom validators for fields whose required status depends on runtime state
// (file uploads, conditional fields). Each returns an array of error objects.
const STEP_CUSTOM_VALIDATORS = {
    2: () => {
        const errors = [];
        // ID Photo is always required
        const idStatus = document.getElementById('idPhotoStatus')?.textContent || '';
        if (!idStatus.startsWith('✓')) {
            errors.push({ id: 'idPhotoFile', label: 'ID Photo', step: 2 });
        }
        // Baptism fields required only when checkbox is checked
        if (document.getElementById('baptized').checked) {
            const bDate = document.getElementById('baptismDate');
            if (!bDate || !bDate.value) {
                errors.push({ id: 'baptismDate', label: 'Baptism Date', step: 2 });
            }
            const bPlace = document.getElementById('baptismPlace');
            if (!bPlace || !bPlace.value.trim()) {
                errors.push({ id: 'baptismPlace', label: 'Baptism Place', step: 2 });
            }
            const certStatus = document.getElementById('baptCertStatus')?.textContent || '';
            if (!certStatus.startsWith('✓')) {
                errors.push({ id: 'baptCertFile', label: 'Baptismal Certificate', step: 2 });
            }
        }
        return errors;
    },
};

// Flat list with step numbers for submit-time full validation
const ALL_REQUIRED = Object.entries(STEP_REQUIRED).flatMap(([step, fields]) =>
    fields.map(f => ({ ...f, step: Number(step) }))
);

// ─── Init ────────────────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', async () => {
    renderTesdaRows();
    renderSyRow();
    setupBaptismToggle();
    setupBirthdateAgeCalc();
    setupPhoneFormatting();
    setupFileUploads();
    document.getElementById('addSyRow').addEventListener('click', () => renderSyRow());

    const params = new URLSearchParams(window.location.search);
    const lastName   = params.get('lastName')   || '';
    const firstName  = params.get('firstName')  || '';
    const middleName = params.get('middleName') || '';

    // Pre-fill name fields from URL params immediately (before API call)
    if (firstName || lastName) {
        prefillNameFields(lastName, firstName, middleName);
    }

    const savedId = sessionStorage.getItem('studentId');

    if (savedId) {
        studentId = savedId;
        try {
            const data = await apiGet(`/api/student/${studentId}`);
            populateForm(data);
            updateNameDisplay(data.lastName, data.firstName, data.middleName);
            if (data.studentStatus === 'Submitted') {
                showSubmittedBanner(studentId);
                return;
            }
        } catch {
            sessionStorage.removeItem('studentId');
        }
    } else if (firstName && lastName) {
        try {
            const data = await apiPost('/api/student/start', { lastName, firstName, middleName });
            studentId = data.studentId;
            sessionStorage.setItem('studentId', studentId);
            populateForm(data);
            updateNameDisplay(lastName, firstName, middleName);
        } catch {
            showAlert('Failed to start enrollment. Please go back and try again.', 'danger');
            return;
        }
    } else {
        showAlert('No student name provided. Please start from the student portal.', 'danger');
        return;
    }

    updateStepUI();
    setupNavButtons();
});

// ─── Step UI ──────────────────────────────────────────────────────────────────
function updateStepUI() {
    for (let i = 1; i <= TOTAL_STEPS; i++) {
        const step   = document.getElementById(`step${i}`);
        const circle = document.getElementById(`circle${i}`);
        const label  = document.getElementById(`label${i}`);

        step.classList.toggle('active', i === currentStep);
        circle.classList.remove('active', 'done');
        label.classList.remove('active');
        if (i < currentStep)       { circle.classList.add('done'); }
        else if (i === currentStep){ circle.classList.add('active'); label.classList.add('active'); }
    }

    const prev   = document.getElementById('btnPrev');
    const next   = document.getElementById('btnNext');
    const submit = document.getElementById('btnSubmit');

    prev.style.display = currentStep === 1 ? 'none' : '';
    next.classList.toggle('d-none', currentStep === TOTAL_STEPS);
    submit.classList.toggle('d-none', currentStep !== TOTAL_STEPS);
}

function setupNavButtons() {
    document.getElementById('btnNext').addEventListener('click', async () => {
        clearAlerts();
        const stepErrors = validateStep(currentStep);
        if (stepErrors.length > 0) {
            highlightErrors(stepErrors);
            return;
        }
        clearValidation();
        await saveDraft();
        if (currentStep < TOTAL_STEPS) {
            currentStep++;
            updateStepUI();
            window.scrollTo(0, 0);
        }
    });

    document.getElementById('btnPrev').addEventListener('click', () => {
        clearAlerts();
        clearValidation();
        if (currentStep > 1) {
            currentStep--;
            updateStepUI();
            window.scrollTo(0, 0);
        }
    });

    document.getElementById('btnSubmit').addEventListener('click', async () => {
        clearAlerts();
        clearValidation();

        const errors = validateAll();
        if (errors.length > 0) {
            highlightErrors(errors);
            return;
        }

        // Final save MUST succeed before we can submit
        const saved = await saveDraft();
        if (!saved) {
            showAlert('Your data could not be saved. Please check your connection and try again.', 'danger');
            return;
        }

        await submitForm();
    });
}

// ─── API helpers ──────────────────────────────────────────────────────────────
async function apiGet(url) {
    const res = await fetch(url);
    if (!res.ok) throw new Error(`GET ${url} → ${res.status}`);
    return res.json();
}

async function apiPost(url, body) {
    const res = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
    });
    if (!res.ok) throw new Error(`POST ${url} → ${res.status}`);
    return res.json();
}

async function apiPut(url, body) {
    const res = await fetch(url, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
    });
    if (!res.ok) throw new Error(`PUT ${url} → ${res.status}`);
    return res.json();
}

// ─── Save draft ───────────────────────────────────────────────────────────────
// Returns true if the save succeeded, false otherwise.
async function saveDraft() {
    if (!studentId) return false;
    try {
        await apiPut(`/api/student/${studentId}`, buildPayload());
        return true;
    } catch (e) {
        return false;
    }
}

// ─── Submit ───────────────────────────────────────────────────────────────────
async function submitForm() {
    try {
        const res = await fetch(`/api/student/${studentId}/submit`, { method: 'POST' });
        if (res.status === 409) {
            showAlert('This enrollment has already been submitted.', 'danger');
            return;
        }
        if (!res.ok) {
            showAlert('Submission failed. Please try again.', 'danger');
            return;
        }
        showSubmittedBanner(studentId);
        sessionStorage.removeItem('studentId');
    } catch {
        showAlert('Submission failed. Check your connection.', 'danger');
    }
}

// ─── Build payload ────────────────────────────────────────────────────────────
function buildPayload() {
    const val = id => {
        const el = document.getElementById(id);
        return el ? el.value.trim() || null : null;
    };
    const num = id => {
        const v = val(id);
        return v !== null && v !== '' ? parseInt(v, 10) : null;
    };
    const dec = id => {
        const v = val(id);
        return v !== null && v !== '' ? parseFloat(v) : null;
    };

    const educationHistory = ['Elementary', 'Secondary', 'Tertiary', 'Vocational'].map(level => ({
        level,
        schoolName:    document.querySelector(`.edu-school[data-level="${level}"]`)?.value.trim()   || null,
        schoolAddress: document.querySelector(`.edu-address[data-level="${level}"]`)?.value.trim()  || null,
        gradeYear:     document.querySelector(`.edu-grade[data-level="${level}"]`)?.value.trim()    || null,
        semester:      document.querySelector(`.edu-sem[data-level="${level}"]`)?.value.trim()      || null,
        endedYear:     document.querySelector(`.edu-ended[data-level="${level}"]`)?.value.trim()    || null,
    }));

    const schoolYears = [];
    document.querySelectorAll('#syTableBody tr').forEach((tr, i) => {
        const inputs = tr.querySelectorAll('input');
        schoolYears.push({
            rowIndex:  i + 1,
            syStart:   inputs[0]?.value.trim() || null,
            semStart:  inputs[1]?.value.trim() || null,
            syEnd:     inputs[2]?.value.trim() || null,
            semEnd:    inputs[3]?.value.trim() || null,
            remarks:   inputs[4]?.value.trim() || null,
        });
    });

    const tesdaQualifications = [1, 2, 3].map(slot => ({
        slot,
        title:          document.getElementById(`tesdaTitle${slot}`)?.value.trim()  || null,
        centerAddress:  document.getElementById(`tesdaCenter${slot}`)?.value.trim() || null,
        assessmentDate: document.getElementById(`tesdaDate${slot}`)?.value          || null,
        result:         document.getElementById(`tesdaResult${slot}`)?.value.trim() || null,
    }));

    const baptized = document.getElementById('baptized').checked;

    const fatherHasData   = ['fatherFamilyName', 'fatherFirstName', 'fatherMiddleName'].some(id => val(id));
    const motherHasData   = ['motherFamilyName', 'motherFirstName', 'motherMiddleName'].some(id => val(id));
    const guardianHasData = ['guardianLastName', 'guardianFirstName'].some(id => val(id));

    return {
        contactNo:        val('contactNo'),
        birthdate:        val('birthdate') || null,
        sex:              val('sex')       || null,
        civilStatus:      val('civilStatus') || null,
        permanentAddress: val('permanentAddress'),
        temporaryAddress: val('temporaryAddress'),
        siblingCount:     num('siblingCount'),
        brotherCount:     num('brotherCount'),
        sisterCount:      num('sisterCount'),
        religion:         val('religion'),
        baptized,
        baptismDate:  baptized ? (val('baptismDate') || null) : null,
        baptismPlace: baptized ? val('baptismPlace')          : null,
        father: fatherHasData ? {
            familyName: val('fatherFamilyName'), firstName: val('fatherFirstName'),
            middleName: val('fatherMiddleName'), birthdate:  val('fatherBirthdate') || null,
            occupation: val('fatherOccupation'), estIncome: dec('fatherEstIncome'),
            contactNo:  val('fatherContactNo'),  email:     val('fatherEmail'),
            address:    val('fatherAddress'),
        } : null,
        mother: motherHasData ? {
            familyName: val('motherFamilyName'), firstName: val('motherFirstName'),
            middleName: val('motherMiddleName'), birthdate:  val('motherBirthdate') || null,
            occupation: val('motherOccupation'), estIncome: dec('motherEstIncome'),
            contactNo:  val('motherContactNo'),  email:     val('motherEmail'),
            address:    val('motherAddress'),
        } : null,
        guardian: guardianHasData ? {
            relation:   val('guardianRelation'),   lastName:   val('guardianLastName'),
            firstName:  val('guardianFirstName'),  middleName: val('guardianMiddleName'),
            birthdate:  val('guardianBirthdate') || null, address: val('guardianAddress'),
        } : null,
        educationHistory,
        schoolYears,
        ojt: {
            companyName:    val('ojtCompany'),
            companyAddress: val('ojtAddress'),
            hoursRendered:  dec('ojtHours'),
        },
        tesdaQualifications,
    };
}

// ─── Populate form from response ──────────────────────────────────────────────
function populateForm(data) {
    const set = (id, value) => {
        const el = document.getElementById(id);
        if (el && value != null) el.value = value;
    };

    // Student name (read-only display fields)
    prefillNameFields(data.lastName, data.firstName, data.middleName);

    set('contactNo', data.contactNo);
    set('birthdate', data.birthdate);
    set('sex', data.sex);
    set('civilStatus', data.civilStatus);
    set('permanentAddress', data.permanentAddress);
    set('temporaryAddress', data.temporaryAddress);
    set('siblingCount', data.siblingCount);
    set('brotherCount', data.brotherCount);
    set('sisterCount', data.sisterCount);
    if (data.age != null) document.getElementById('ageDisplay').value = data.age;

    set('religion', data.religion);
    if (data.baptized) {
        document.getElementById('baptized').checked = true;
        document.getElementById('baptismFields').style.setProperty('display', 'flex', 'important');
        set('baptismDate', data.baptismDate);
        set('baptismPlace', data.baptismPlace);
    }

    if (data.idPhotoRef) {
        document.getElementById('idPhotoStatus').textContent = `✓ ${data.idPhotoRef.originalName}`;
        const img = document.getElementById('idPhotoPreview');
        img.src = `/api/student/files/${data.idPhotoRef.uploadId}`;
        img.classList.add('show');
    }
    if (data.baptismalCertRef) {
        document.getElementById('baptCertStatus').textContent = `✓ ${data.baptismalCertRef.originalName}`;
        if (data.baptismalCertRef.mimeType !== 'application/pdf') {
            const img = document.getElementById('baptCertPreview');
            img.src = `/api/student/files/${data.baptismalCertRef.uploadId}`;
            img.classList.add('show');
        }
    }

    const f = data.father;
    if (f) {
        set('fatherFamilyName', f.familyName); set('fatherFirstName', f.firstName);
        set('fatherMiddleName', f.middleName); set('fatherBirthdate', f.birthdate);
        set('fatherOccupation', f.occupation); set('fatherEstIncome', f.estIncome);
        set('fatherContactNo', f.contactNo);   set('fatherEmail', f.email);
        set('fatherAddress', f.address);
    }
    const m = data.mother;
    if (m) {
        set('motherFamilyName', m.familyName); set('motherFirstName', m.firstName);
        set('motherMiddleName', m.middleName); set('motherBirthdate', m.birthdate);
        set('motherOccupation', m.occupation); set('motherEstIncome', m.estIncome);
        set('motherContactNo', m.contactNo);   set('motherEmail', m.email);
        set('motherAddress', m.address);
    }
    const g = data.guardian;
    if (g) {
        set('guardianRelation', g.relation);   set('guardianLastName', g.lastName);
        set('guardianFirstName', g.firstName); set('guardianMiddleName', g.middleName);
        set('guardianBirthdate', g.birthdate); set('guardianAddress', g.address);
    }

    if (data.educationHistory) {
        data.educationHistory.forEach(e => {
            const lvl = e.level;
            const q = sel => document.querySelector(`${sel}[data-level="${lvl}"]`);
            if (q('.edu-school'))   q('.edu-school').value   = e.schoolName    || '';
            if (q('.edu-address'))  q('.edu-address').value  = e.schoolAddress || '';
            if (q('.edu-grade'))    q('.edu-grade').value    = e.gradeYear     || '';
            if (q('.edu-sem'))      q('.edu-sem').value      = e.semester      || '';
            if (q('.edu-ended'))    q('.edu-ended').value    = e.endedYear     || '';
        });
    }

    if (data.schoolYears && data.schoolYears.length > 0) {
        document.getElementById('syTableBody').innerHTML = '';
        data.schoolYears.forEach(sy =>
            addSyRowData(sy.syStart, sy.semStart, sy.syEnd, sy.semEnd, sy.remarks));
    }

    if (data.ojt) {
        set('ojtCompany', data.ojt.companyName);
        set('ojtAddress', data.ojt.companyAddress);
        set('ojtHours',   data.ojt.hoursRendered);
    }

    if (data.tesdaQualifications) {
        data.tesdaQualifications.forEach(q => {
            if (q.slot >= 1 && q.slot <= 3) {
                set(`tesdaTitle${q.slot}`,  q.title);
                set(`tesdaCenter${q.slot}`, q.centerAddress);
                set(`tesdaDate${q.slot}`,   q.assessmentDate);
                set(`tesdaResult${q.slot}`, q.result);
            }
        });
    }
}

// ─── Name pre-fill (from URL params or loaded data) ───────────────────────────
function prefillNameFields(lastName, firstName, middleName) {
    const set = (id, v) => { const el = document.getElementById(id); if (el && v) el.value = v; };
    set('studentLastName',   lastName);
    set('studentFirstName',  firstName);
    set('studentMiddleName', middleName);
}

// ─── Dynamic rows ─────────────────────────────────────────────────────────────
function renderSyRow() {
    addSyRowData('', '', '', '', '');
}

function addSyRowData(syStart, semStart, syEnd, semEnd, remarks) {
    const tbody    = document.getElementById('syTableBody');
    const rowIndex = tbody.rows.length + 1;
    const tr       = document.createElement('tr');
    tr.innerHTML = `
        <td class="text-center align-middle small text-muted">${rowIndex}</td>
        <td><input type="text" class="form-control form-control-sm" value="${syStart  || ''}" placeholder="2025-2026"></td>
        <td><input type="text" class="form-control form-control-sm" value="${semStart || ''}" placeholder="1st"></td>
        <td><input type="text" class="form-control form-control-sm" value="${syEnd    || ''}" placeholder="2025-2026"></td>
        <td><input type="text" class="form-control form-control-sm" value="${semEnd   || ''}" placeholder="2nd"></td>
        <td><input type="text" class="form-control form-control-sm" value="${remarks  || ''}"></td>
    `;
    tbody.appendChild(tr);
}

function renderTesdaRows() {
    const container = document.getElementById('tesdaRows');
    [1, 2, 3].forEach(slot => {
        const div = document.createElement('div');
        div.className = 'row g-2 mb-3 align-items-end';
        div.innerHTML = `
            <div class="col-auto align-self-center">
                <span class="badge bg-success rounded-pill">${slot}</span>
            </div>
            <div class="col-md-4">
                <label class="form-label">Qualification Title</label>
                <input type="text" class="form-control" id="tesdaTitle${slot}">
            </div>
            <div class="col-md-4">
                <label class="form-label">Assessment Center</label>
                <input type="text" class="form-control" id="tesdaCenter${slot}">
            </div>
            <div class="col-md-2">
                <label class="form-label">Date</label>
                <input type="date" class="form-control" id="tesdaDate${slot}">
            </div>
            <div class="col-md-1">
                <label class="form-label">Result</label>
                <select class="form-select" id="tesdaResult${slot}">
                    <option value="">—</option>
                    <option value="Passed">Passed</option>
                    <option value="Failed">Failed</option>
                    <option value="NC">NC</option>
                </select>
            </div>
        `;
        container.appendChild(div);
    });
}

// ─── Baptism toggle ───────────────────────────────────────────────────────────
function setupBaptismToggle() {
    document.getElementById('baptized').addEventListener('change', function () {
        const fields = document.getElementById('baptismFields');
        fields.style.setProperty('display', this.checked ? 'flex' : 'none', 'important');
    });
}

// ─── Age auto-calculation ─────────────────────────────────────────────────────
function setupBirthdateAgeCalc() {
    document.getElementById('birthdate').addEventListener('change', function () {
        if (!this.value) { document.getElementById('ageDisplay').value = ''; return; }
        const today = new Date();
        const birth = new Date(this.value);
        let age = today.getFullYear() - birth.getFullYear();
        const m = today.getMonth() - birth.getMonth();
        if (m < 0 || (m === 0 && today.getDate() < birth.getDate())) age--;
        document.getElementById('ageDisplay').value = age >= 0 ? age : 0;
    });
}

// ─── Philippine phone formatting (+63) ───────────────────────────────────────
function formatPH(raw) {
    let d = raw.replace(/\D/g, '');         // strip non-digits
    if (d.startsWith('63'))  d = d.slice(2);
    else if (d.startsWith('0')) d = d.slice(1);
    d = d.slice(0, 10);                     // cap at 10 digits after country code
    if (!d) return '';
    const p1 = d.slice(0, 3);
    const p2 = d.slice(3, 6);
    const p3 = d.slice(6, 10);
    return '+63 ' + [p1, p2, p3].filter(Boolean).join(' ');
}

function attachPhoneFormatter(id) {
    const el = document.getElementById(id);
    if (!el) return;
    el.addEventListener('input', function () {
        const formatted = formatPH(this.value);
        this.value = formatted;
        this.setSelectionRange(formatted.length, formatted.length);
    });
    el.addEventListener('blur', function () {
        this.value = formatPH(this.value);
    });
}

function setupPhoneFormatting() {
    ['contactNo', 'fatherContactNo', 'motherContactNo'].forEach(attachPhoneFormatter);
}

// ─── File uploads ─────────────────────────────────────────────────────────────
function setupFileUploads() {
    setupFileInput('idPhotoFile',  'idPhotoPreview',  'idPhotoStatus',  'ID_PHOTO');
    setupFileInput('baptCertFile', 'baptCertPreview', 'baptCertStatus', 'BAPTISMAL_CERT');
}

function setupFileInput(inputId, previewId, statusId, kind) {
    document.getElementById(inputId).addEventListener('change', async function () {
        if (!this.files.length || !studentId) return;
        const file     = this.files[0];
        const statusEl = document.getElementById(statusId);
        const previewEl = document.getElementById(previewId);

        statusEl.textContent = 'Uploading…';

        const fd = new FormData();
        fd.append('file', file);

        try {
            const res = await fetch(`/api/student/${studentId}/upload?kind=${kind}`, {
                method: 'POST',
                body: fd
            });
            if (!res.ok) {
                const msg = await res.text().catch(() => 'Upload failed.');
                statusEl.textContent = `Error: ${msg}`;
                return;
            }
            const ref = await res.json();
            statusEl.textContent = `✓ ${ref.originalName}`;

            if (file.type.startsWith('image/')) {
                const reader = new FileReader();
                reader.onload = e => {
                    previewEl.src = e.target.result;
                    previewEl.classList.add('show');
                };
                reader.readAsDataURL(file);
            } else {
                previewEl.classList.remove('show');
                statusEl.textContent += ' (PDF)';
            }
        } catch {
            statusEl.textContent = 'Upload error. Try again.';
        }
    });
}

// ─── Client-side validation ───────────────────────────────────────────────────
function validateStep(step) {
    const fieldErrors = (STEP_REQUIRED[step] || []).filter(f => {
        const el = document.getElementById(f.id);
        if (!el) return false;
        return el.tagName === 'SELECT' ? !el.value : !el.value.trim();
    }).map(f => ({ ...f, step }));

    const customErrors = STEP_CUSTOM_VALIDATORS[step] ? STEP_CUSTOM_VALIDATORS[step]() : [];
    return [...fieldErrors, ...customErrors];
}

function validateAll() {
    const fieldErrors = ALL_REQUIRED.filter(f => {
        const el = document.getElementById(f.id);
        if (!el) return false;
        return el.tagName === 'SELECT' ? !el.value : !el.value.trim();
    });

    const customErrors = Object.values(STEP_CUSTOM_VALIDATORS).flatMap(fn => fn());
    return [...fieldErrors, ...customErrors];
}

function highlightErrors(errors) {
    errors.forEach(f => {
        const el = document.getElementById(f.id);
        if (!el) return;
        el.classList.add('is-invalid');
        el.addEventListener('input',  () => el.classList.remove('is-invalid'), { once: true });
        el.addEventListener('change', () => el.classList.remove('is-invalid'), { once: true });
    });

    // Navigate to the first step with an error
    const firstStep = Math.min(...errors.map(f => f.step));
    if (currentStep !== firstStep) {
        currentStep = firstStep;
        updateStepUI();
    }

    document.getElementById(errors[0].id)?.scrollIntoView({ behavior: 'smooth', block: 'center' });

    const labels    = errors.map(f => `<strong>${f.label}</strong>`).join(', ');
    const stepNames = [...new Set(errors.map(f => `Step ${f.step}`))].join(' and ');
    showAlertHtml(
        `Please fill in the highlighted fields (${stepNames}): ${labels}`,
        'danger'
    );
}

// IDs that may receive is-invalid from custom validators (uploads + conditional fields)
const CUSTOM_VALIDATED_IDS = ['idPhotoFile', 'baptCertFile', 'baptismDate', 'baptismPlace'];

function clearValidation() {
    ALL_REQUIRED.forEach(f => document.getElementById(f.id)?.classList.remove('is-invalid'));
    CUSTOM_VALIDATED_IDS.forEach(id => document.getElementById(id)?.classList.remove('is-invalid'));
}

// ─── UI helpers ───────────────────────────────────────────────────────────────
function updateNameDisplay(lastName, firstName, middleName) {
    document.getElementById('studentName').textContent =
        `${firstName}${middleName ? ' ' + middleName : ''} ${lastName}`;
    prefillNameFields(lastName, firstName, middleName);
}

function showAlert(msg, type) {
    const el    = type === 'danger' ? document.getElementById('alertBox')   : document.getElementById('successBox');
    const other = type === 'danger' ? document.getElementById('successBox') : document.getElementById('alertBox');
    el.textContent = msg;
    el.classList.remove('d-none');
    other.classList.add('d-none');
    el.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

function showAlertHtml(html, type) {
    const el    = type === 'danger' ? document.getElementById('alertBox')   : document.getElementById('successBox');
    const other = type === 'danger' ? document.getElementById('successBox') : document.getElementById('alertBox');
    el.innerHTML = html;
    el.classList.remove('d-none');
    other.classList.add('d-none');
    el.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

function clearAlerts() {
    document.getElementById('alertBox').classList.add('d-none');
    document.getElementById('successBox').classList.add('d-none');
}

function showSubmittedBanner(sid) {
    // Hide wizard UI
    document.getElementById('stepIndicator').style.display = 'none';
    document.getElementById('wizardNav').classList.add('d-none');
    document.getElementById('alertBox').classList.add('d-none');
    document.getElementById('successBox').classList.add('d-none');
    for (let i = 1; i <= TOTAL_STEPS; i++) {
        document.getElementById(`step${i}`).style.display = 'none';
    }

    document.getElementById('submittedId').textContent = sid;
    document.getElementById('submittedBanner').classList.add('show');

    // Animate progress bar (CSS transition driven)
    const bar = document.getElementById('redirectProgress');
    const msg = document.getElementById('redirectMsg');
    setTimeout(() => { bar.style.width = '100%'; }, 50);

    // Countdown label
    let remaining = Math.ceil(REDIRECT_DELAY_MS / 1000);
    const tick = () => {
        msg.textContent = `Returning to portal in ${remaining} second${remaining !== 1 ? 's' : ''}…`;
        if (remaining > 0) { remaining--; setTimeout(tick, 1000); }
    };
    tick();

    setTimeout(() => { window.location.href = 'student-portal.html'; }, REDIRECT_DELAY_MS);
}
