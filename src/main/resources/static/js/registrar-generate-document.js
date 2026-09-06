/**
 * registrar-generate-document.js — Auto-filled, editable, print-ready generation of the
 * official document templates (TOR / Form IX). The rendered document itself is the
 * fillable form: every <span class="fill"> is contenteditable, pre-populated from
 * GET /api/registrar/documents/generate-data/{studentId}, and the registrar completes
 * the rest before printing or saving to the Documents module.
 */
(function () {
    'use strict';

    const TEMPLATES = window.AnihanCurriculum.TEMPLATES;
    const GRADING = window.AnihanCurriculum.GRADING_SYSTEM;

    let currentData = null;
    let currentTemplateKey = null;
    let currentVariant = 'candidate';
    let cachedCss = null;
    let allStudents = [];
    // Edit mode: re-open a saved generated document (documents.html -> View -> Edit).
    let editDocumentId = null;
    let editFileName = null;

    // Degrade to a logo-less header if anihan-logo.js ever fails to load —
    // a hard dereference here would kill the whole page at parse time.
    const LOGO_URI = (window.AnihanLogo && window.AnihanLogo.DATA_URI) || '';

    const SCHOOL_HEADER =
        '<div class="doc-school-header">' +
        (LOGO_URI
            ? '<img class="doc-school-logo" src="' + LOGO_URI + '" alt="Anihan Technical School logo">'
            : '') +
        '<div>' +
        '<p class="doc-school-name">Anihan Technical School</p>' +
        '<p class="doc-school-sub">(For Women-in-Development)</p>' +
        '<p class="doc-school-sub">A project of the Foundation for Professional Training, Inc. (FPTI)</p>' +
        '</div>' +
        '</div>';

    const CONTACT_FOOTER =
        '<div class="doc-contact-footer">' +
        'Email: info@anihan.edu.ph / anihanschool@gmail.com / registrar@anihan.edu.ph * Web: anihan.edu.ph<br>' +
        '294 Purok 6 Brgy. Milagrosa, Calamba City, Laguna 4027 Philippines * (049) 545-1598 * 0939-2666108' +
        '</div>';

    $(document).ready(function () {
        document.body.classList.add('document-toolbar-active');
        setupStudentPicker();
        populateTemplateSelect();
        $('#generateTemplate').on('change', toggleVariantSelect);
        $('#loadDocumentBtn').on('click', loadDocument);
        $('#printDocumentBtn').on('click', printDocument);
        $('#saveDocumentBtn').on('click', saveDocument);
        $('#cancelDocumentBtn').on('click', function () {
            window.location.href = 'documents.html';
        });
        toggleVariantSelect();

        const params = new URLSearchParams(window.location.search);
        if (params.get('documentId')) {
            enterEditMode(params);
        } else if (params.get('studentId')) {
            $('#generateStudentId').val(params.get('studentId'));
        }
    });

    function populateTemplateSelect() {
        const sel = $('#generateTemplate');
        Object.keys(TEMPLATES).forEach(function (key) {
            sel.append('<option value="' + esc(key) + '">' + esc(key) + '</option>');
        });
    }

    function toggleVariantSelect() {
        const key = $('#generateTemplate').val();
        const isFormIx = key && TEMPLATES[key].kind === 'FORM_IX';
        $('#formIxVariantWrap').toggleClass('d-none', !isFormIx);
    }

    // -------------------------------------------------------
    // Student picker — searchable dropdown (combobox)
    // -------------------------------------------------------

    function setupStudentPicker() {
        const input = document.getElementById('generateStudentId');
        const menu = document.getElementById('studentPickerMenu');
        let activeIndex = -1;
        let studentsLoaded = false;

        $.ajax({
            url: '/api/registrar/student-records',
            method: 'GET',
            success: function (students) {
                allStudents = students.map(function (s) {
                    return {
                        studentId: s.studentId,
                        name: joinNonBlank([s.lastName, s.firstName], ', '),
                        search: (s.studentId + ' ' + s.lastName + ' ' + s.firstName).toLowerCase()
                    };
                });
                studentsLoaded = true;
                if (menu.classList.contains('show') || document.activeElement === input) {
                    renderMenu();
                }
            }
        });

        function filteredStudents() {
            const q = input.value.trim().toLowerCase();
            if (!q) return allStudents;
            return allStudents.filter(function (s) { return s.search.indexOf(q) >= 0; });
        }

        function renderMenu() {
            const matches = filteredStudents();
            activeIndex = -1;
            if (!studentsLoaded) {
                menu.innerHTML = '<span class="dropdown-item-text text-muted">Loading students&hellip;</span>';
            } else if (!matches.length) {
                menu.innerHTML = '<span class="dropdown-item-text text-muted">No matching students</span>';
            } else {
                menu.innerHTML = matches.map(function (s) {
                    return '<button type="button" class="dropdown-item" role="option" data-id="' + esc(s.studentId) + '">' +
                        '<strong>' + esc(s.studentId) + '</strong>' +
                        '<span class="text-muted"> — ' + esc(s.name) + '</span>' +
                        '</button>';
                }).join('');
            }
            openMenu();
        }

        function openMenu() {
            menu.classList.add('show');
            input.setAttribute('aria-expanded', 'true');
        }

        function closeMenu() {
            menu.classList.remove('show');
            input.setAttribute('aria-expanded', 'false');
            activeIndex = -1;
        }

        function items() {
            return menu.querySelectorAll('.dropdown-item');
        }

        function setActive(index) {
            const els = items();
            if (!els.length) return;
            activeIndex = (index + els.length) % els.length;
            els.forEach(function (el, i) { el.classList.toggle('active', i === activeIndex); });
            els[activeIndex].scrollIntoView({ block: 'nearest' });
        }

        function select(el) {
            input.value = el.getAttribute('data-id');
            closeMenu();
            input.focus();
        }

        input.addEventListener('focus', renderMenu);
        input.addEventListener('input', renderMenu);
        input.addEventListener('keydown', function (e) {
            if (e.key === 'ArrowDown') {
                e.preventDefault();
                if (!menu.classList.contains('show')) renderMenu();
                setActive(activeIndex + 1);
            } else if (e.key === 'ArrowUp') {
                e.preventDefault();
                setActive(activeIndex - 1);
            } else if (e.key === 'Enter') {
                const els = items();
                if (menu.classList.contains('show') && activeIndex >= 0 && els[activeIndex]) {
                    e.preventDefault();
                    select(els[activeIndex]);
                } else {
                    closeMenu();
                }
            } else if (e.key === 'Escape' || e.key === 'Tab') {
                closeMenu();
            }
        });
        menu.addEventListener('mousedown', function (e) {
            const item = e.target.closest('.dropdown-item');
            if (item) {
                e.preventDefault();
                select(item);
            }
        });
        document.addEventListener('click', function (e) {
            if (!document.getElementById('studentPicker').contains(e.target)) closeMenu();
        });
    }

    /**
     * Resolve whatever is typed in the picker to a student ID: an exact ID match wins,
     * otherwise a search string matching exactly one student resolves to that student.
     */
    function resolveStudentId(raw) {
        const q = raw.trim().toLowerCase();
        if (!q) return null;
        const exact = allStudents.find(function (s) { return s.studentId.toLowerCase() === q; });
        if (exact) return exact.studentId;
        const matches = allStudents.filter(function (s) { return s.search.indexOf(q) >= 0; });
        if (matches.length === 1) return matches[0].studentId;
        return allStudents.length ? null : raw.trim();
    }

    // -------------------------------------------------------
    // Edit mode — re-open a saved generated document
    // -------------------------------------------------------

    function enterEditMode(params) {
        editDocumentId = Number(params.get('documentId'));
        editFileName = params.get('fileName') || '';
        currentTemplateKey = params.get('documentType') || '';
        const studentId = params.get('studentId') || '';
        currentData = { student: { studentId: studentId } };

        // The document identity is fixed while editing — lock the setup controls.
        $('#generateStudentId').val(studentId).prop('disabled', true);
        $('#generateTemplate').val(currentTemplateKey).prop('disabled', true);
        $('#generateVariant').prop('disabled', true);
        $('#loadDocumentBtn').prop('disabled', true);
        toggleVariantSelect();

        $.ajax({
            url: '/api/registrar/documents/' + encodeURIComponent(editDocumentId) + '/view',
            method: 'GET',
            dataType: 'html',
            success: function (html) {
                const parsed = new DOMParser().parseFromString(html, 'text/html');
                const sheet = parsed.querySelector('.document-sheet');
                if (!sheet) {
                    showAlert('This document has no editable content. Only documents generated '
                        + 'by this page can be edited.', 'danger');
                    return;
                }
                const area = document.getElementById('documentArea');
                area.innerHTML = '';
                area.appendChild(document.adoptNode(sheet));
                // Saved documents have contenteditable stripped — re-arm every blank.
                area.querySelectorAll('.fill').forEach(function (el) {
                    el.setAttribute('contenteditable', 'true');
                });
                $('#printDocumentBtn').prop('disabled', false);
                $('#saveDocumentBtn').prop('disabled', false);
                $('#cancelDocumentBtn').removeClass('d-none');
                showAlert('Editing "' + editFileName + '". Change any value below, then '
                    + 'Save to Documents to overwrite the saved copy.', 'info');
            },
            error: function (xhr) {
                showAlert(ajaxErrorMessage(xhr, 'Failed to load the saved document'), 'danger');
            }
        });

        // Names are only needed for the print filename; missing data degrades gracefully.
        if (studentId) {
            $.ajax({
                url: '/api/registrar/documents/generate-data/' + encodeURIComponent(studentId),
                method: 'GET',
                success: function (data) {
                    currentData = data;
                }
            });
        }
    }

    // -------------------------------------------------------
    // Load + render
    // -------------------------------------------------------

    function loadDocument() {
        const rawStudent = $('#generateStudentId').val().trim();
        const templateKey = $('#generateTemplate').val();
        if (!rawStudent || !templateKey) {
            showAlert('Please pick a student and a template.', 'danger');
            return;
        }

        const studentId = resolveStudentId(rawStudent);
        if (!studentId) {
            showAlert('No student matches "' + rawStudent + '". Pick one from the dropdown list.', 'danger');
            return;
        }
        $('#generateStudentId').val(studentId);

        hideAlert();
        $.ajax({
            url: '/api/registrar/documents/generate-data/' + encodeURIComponent(studentId),
            method: 'GET',
            success: function (data) {
                currentData = data;
                currentTemplateKey = templateKey;
                currentVariant = $('#generateVariant').val() || 'candidate';
                render();
                $('#printDocumentBtn').prop('disabled', false);
                $('#saveDocumentBtn').prop('disabled', false);
                $('#cancelDocumentBtn').removeClass('d-none');
                document.getElementById('documentArea').scrollIntoView({ behavior: 'smooth' });
            },
            error: function (xhr) {
                showAlert(ajaxErrorMessage(xhr, 'Failed to load student data'), 'danger');
            }
        });
    }

    /**
     * Build a diagnosable error message: prefer the server's JSON message, otherwise
     * include the HTTP status so failures (404 stale build, session timeout, network)
     * are distinguishable instead of one generic string.
     */
    function ajaxErrorMessage(xhr, prefix) {
        if (xhr.responseJSON && xhr.responseJSON.message) return xhr.responseJSON.message;
        if (xhr.status === 0) return prefix + ': cannot reach the server. Check that the application is running.';
        if (xhr.status === 401) return prefix + ': your session has expired. Please log in again.';
        if (xhr.status === 404) return prefix + ': the server does not have this endpoint (HTTP 404). It may be running an outdated build — restart it after updating.';
        return prefix + ' (HTTP ' + xhr.status + '). Please contact the administrator.';
    }

    function render() {
        const template = TEMPLATES[currentTemplateKey];
        const html = template.kind === 'TOR'
            ? renderTor(currentData, template)
            : renderFormIx(currentData, template, currentVariant);
        document.getElementById('documentArea').innerHTML =
            '<div class="document-sheet">' + html + '</div>';
    }

    function renderTor(data, template) {
        const s = data.student;
        const sy = schoolYear(s.batchYear);
        const tesda = {
            bpp: findTesda(data, ['bread', 'pastry']),
            fbs: findTesda(data, ['food', 'beverage']),
            cookery: findTesda(data, ['cookery'])
        };

        let html = SCHOOL_HEADER;
        html += '<div class="doc-title">O F F I C E&nbsp;&nbsp;O F&nbsp;&nbsp;T H E&nbsp;&nbsp;R E G I S T R A R' +
            '<span class="doc-subtitle">O F F I C I A L&nbsp;&nbsp;T R A N S C R I P T&nbsp;&nbsp;O F&nbsp;&nbsp;R E C O R D S</span></div>';

        html += '<div class="doc-fields">';
        html += fieldRow([
            field('Name', fullName(s), 'wide'),
            field('Address', s.permanentAddress, 'wide')
        ]);
        html += fieldRow([
            field('Date of Admission', fmtDate(s.enrollmentDate)),
            field('Sex', s.sex, 'narrow'),
            field('Birthday', fmtDate(s.birthdate)),
            field('Student No.', s.studentId)
        ]);
        html += fieldRow([
            field('Entrance Date', ''),
            field('Hon. Dismissal', ''),
            field('Graduation', '')
        ]);
        html += fieldRow([
            field('Entrance Data', '', 'wide'),
            field('SY Ended', '')
        ]);
        html += fieldRow([
            field('Course Title', s.courseName || 'Culinary Arts and Restaurant Services', 'wide'),
            field('Certificate No.', '', 'wide')
        ]);
        html += assessmentBlock('Bread and Pastry Production-NC II', tesda.bpp);
        html += assessmentBlock('Food and Beverage Services-NC II', tesda.fbs);
        html += assessmentBlock('Cookery -NC II', tesda.cookery);
        html += fieldRow([field('Remarks', '', 'wide')]);
        html += '</div>';

        html += '<p class="doc-semester"><span class="fill" contenteditable="true">' +
            esc(sy + ', First Semester') + '</span></p>';
        html += subjectsTable(template.firstSemesterSections, data, false);

        html += '<p class="doc-semester"><span class="fill" contenteditable="true">' +
            esc(sy + ', Second Semester') + '</span></p>';
        html += subjectsTable(template.secondSemesterSections, data, false);

        html += '<p class="doc-end-line">X X X X X X X X X X X X X X X X X END OF TRANSCRIPT X X X X X X X X X X X X X X X X X</p>';

        html += '<div class="doc-notes">' +
            '<p><strong>CREDITS:</strong> One unit credit is one hour lecture, laboratory or practicum / OJT per week for a period of a complete semester.</p>' +
            '<p><strong>NOTE:</strong> Any erasure or alteration on this record invalidates the whole transcript.</p>' +
            '</div>';

        html += gradingLegend();

        html += '<div class="doc-signatories">' +
            signatory('Prepared by:', 'MELESINE B. VELASCO', 'Assistant to the Registrar') +
            signatory('Approved by:', 'HERMINIA F. GABUTINA', 'Registrar') +
            '</div>';

        html += CONTACT_FOOTER;
        return html;
    }

    function renderFormIx(data, template, variant) {
        const s = data.student;
        const sy = schoolYear(s.batchYear);
        const title = variant === 'permanent'
            ? 'STUDENT&rsquo;S PERMANENT RECORD'
            : 'RECORDS OF CANDIDATE FOR GRADUATION';
        const parent = pickParent(data);
        const assessment = findTesda(data, template.qualification.toLowerCase().split(' ').slice(0, 1));
        const edu = {
            elementary: findEducation(data, 'elem'),
            jhs: findEducation(data, 'junior'),
            shs: findEducation(data, 'senior'),
            last: findEducation(data, 'last')
        };

        let html = SCHOOL_HEADER;
        html += '<p class="doc-form-label">FORM IX</p>';
        html += '<div class="doc-title"><span class="doc-subtitle">' + title + '</span></div>';

        html += '<div class="doc-fields">';
        html += fieldRow([
            field('Name:', fullName(s), 'wide'),
            field('Address:', s.permanentAddress, 'wide')
        ]);
        html += fieldRow([
            field('Date of Birth:', fmtDate(s.birthdate)),
            field('Age:', s.age == null ? '' : String(s.age), 'narrow'),
            field('Sex', s.sex, 'narrow'),
            field('Student No.', s.studentId)
        ]);
        html += fieldRow([
            field('Parent:', parent ? parent.fullName : '', 'wide'),
            field('Address:', parent ? parent.address : '', 'wide')
        ]);
        html += fieldRow([
            field('Date of Graduation:', ''),
            field('Course Title:', s.courseName || 'Culinary Arts and Restaurant Services'),
            field('ULI:', '')
        ]);
        html += fieldRow([
            fieldStatic('Qualification:', template.qualification),
            field('Start of Training:', fmtDate(s.enrollmentDate)),
            field('End of Training:', '')
        ]);
        html += fieldRow([
            field('Assessment:', assessment ? assessment.title : template.qualification),
            field('Date Taken:', assessment ? fmtDate(assessment.assessmentDate) : ''),
            field('Result:', assessment ? assessment.result : '')
        ]);
        html += educationRow('Elementary School Completed at:', edu.elementary);
        html += educationRow('Junior High School Completed at:', edu.jhs);
        html += educationRow('Senior High School Completed at:', edu.shs);
        html += educationRow('Last School Attended at:', edu.last);
        html += '</div>';

        html += '<p class="doc-semester"><span class="fill" contenteditable="true">' +
            esc(sy + ', First Semester') + '</span></p>';
        html += subjectsTable(template.firstSemesterSections, data, true);

        html += '<p class="doc-end-line">X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X</p>';

        if (variant === 'candidate') {
            html += '<div class="doc-certification">' +
                '<p class="cert-heading">C E R T I F I C A T I O N :</p>' +
                '<p>This is to certify that the foregoing record of Ms. ' +
                '<span class="fill" contenteditable="true">' + esc(fullName(s)) + '</span>, ' +
                'a candidate for graduation in this institution has been verified by me and true copies of the ' +
                'official records substantiating same are kept in the file of our school. I also certify that ' +
                'this student was enrolled in our institution in <u><strong>' +
                esc(template.qualificationUnderline) + '</strong></u>.</p>' +
                '</div>';
            html += '<p class="doc-date-line">Date: <span class="fill" contenteditable="true">' +
                esc(fmtDate(new Date().toISOString().slice(0, 10))) + '</span></p>';
        }

        html += '<div class="doc-signatories">' +
            signatory('Certified by:', 'HERMINIA F. GABUTINA', 'Registrar') +
            '</div>';

        html += CONTACT_FOOTER;
        return html;
    }

    // -------------------------------------------------------
    // Document building blocks
    // -------------------------------------------------------

    function fieldRow(fields) {
        return '<div class="doc-field-row">' + fields.join('') + '</div>';
    }

    function field(label, value, cls) {
        return '<div class="doc-field ' + (cls || '') + '">' +
            '<span class="doc-field-label">' + label + '</span>' +
            '<span class="fill" contenteditable="true">' + esc(value || '') + '</span>' +
            '</div>';
    }

    function fieldStatic(label, value) {
        return '<div class="doc-field">' +
            '<span class="doc-field-label">' + label + '</span>' +
            '<span class="fill static-value"><u><strong>' + esc(value) + '</strong></u></span>' +
            '</div>';
    }

    function assessmentBlock(qualificationLabel, tesda) {
        return fieldRow([
            fieldStatic('TESDA Assessment', qualificationLabel),
            field('Date Taken', tesda ? fmtDate(tesda.assessmentDate) : ''),
            field('Result', tesda ? tesda.result : '')
        ]) + fieldRow([
            field('Special Order No.', '', 'wide'),
            field('Issued', '', 'wide')
        ]);
    }

    function educationRow(label, edu) {
        return fieldRow([
            field(label, edu ? joinNonBlank([edu.schoolName, edu.schoolAddress], ', ') : '', 'wide'),
            field('Year:', edu ? (edu.endedYear || '') : '', 'narrow')
        ]);
    }

    function subjectsTable(sections, data, withRemarks) {
        const gradesByCode = {};
        (data.grades || []).forEach(function (g) {
            gradesByCode[g.subjectCode] = g;
        });

        let totalHours = 0;
        let totalUnits = 0;

        let html = '<table class="doc-subjects-table"><thead><tr>' +
            '<th rowspan="2" class="col-code">SUBJECT CODE</th>' +
            '<th rowspan="2">SUBJECT TITLE</th>' +
            '<th colspan="2">GRADES</th>' +
            '<th rowspan="2" class="col-hours">HOURS</th>' +
            '<th rowspan="2" class="col-units">UNITS</th>' +
            (withRemarks ? '<th rowspan="2" class="col-remarks">Remarks</th>' : '') +
            '</tr><tr>' +
            '<th class="col-grade">FINAL</th><th class="col-grade">RE-EXAM</th>' +
            '</tr></thead><tbody>';

        const cols = withRemarks ? 7 : 6;

        sections.forEach(function (section) {
            html += '<tr class="section-row"><td></td><td colspan="' + (cols - 1) + '">' +
                esc(section.label) + '</td></tr>';
            section.subjects.forEach(function (subj) {
                // GradePart fields are pre-formatted strings from the backend:
                // finalGrade = the 1.00–5.00 equivalent or a status code (C/FA/INC/D);
                // reExamGrade = the equivalent or null; remarks = "Competent" / "Not Competent" / null.
                const grade = gradesByCode[subj.code];
                const finalGrade = grade && grade.finalGrade != null ? esc(String(grade.finalGrade)) : '';
                const reExam = grade && grade.reExamGrade != null ? esc(String(grade.reExamGrade)) : '';
                const remarks = grade && grade.remarks != null ? esc(grade.remarks) : '';
                totalHours += parseFloat(subj.hours);
                totalUnits += parseFloat(subj.units);

                html += '<tr>' +
                    '<td class="col-code">' + esc(subj.code) + '</td>' +
                    '<td>' + esc(subj.title) + '</td>' +
                    '<td class="col-grade"><span class="fill" contenteditable="true">' + finalGrade + '</span></td>' +
                    '<td class="col-grade"><span class="fill" contenteditable="true">' + reExam + '</span></td>' +
                    '<td class="col-hours">' + esc(subj.hours) + '</td>' +
                    '<td class="col-units">' + esc(subj.units) + '</td>' +
                    (withRemarks
                        ? '<td class="col-remarks"><span class="fill" contenteditable="true">' + esc(remarks) + '</span></td>'
                        : '') +
                    '</tr>';
            });
        });

        if (withRemarks) {
            html += '<tr class="total-row">' +
                '<td colspan="4">Total</td>' +
                '<td class="col-hours">' + formatTotal(totalHours) + '</td>' +
                '<td class="col-units">' + totalUnits.toFixed(2) + '</td>' +
                '<td></td></tr>';
        }

        html += '</tbody></table>';
        return html;
    }

    function gradingLegend() {
        let rows = '';
        GRADING.forEach(function (r, i) {
            rows += '<tr>' +
                '<td>' + esc(r[0]) + '</td><td>' + esc(r[1]) + '</td><td>' + esc(r[2]) + '</td>' +
                (i === 0 ? '<td class="competent-cell" rowspan="8">Competent</td>' : '') +
                '<td>' + esc(r[3]) + '</td><td>' + esc(r[4]) + '</td><td>' + esc(r[5]) + '</td>' +
                (i === 0 ? '<td class="competent-cell" rowspan="2">Competent</td>' : '') +
                (i === 2 ? '<td class="competent-cell" rowspan="5">Not Competent</td>' : '') +
                (i === 7 ? '<td></td>' : '') +
                '</tr>';
        });

        return '<div class="doc-grading-wrap">' +
            '<div class="doc-seal-note">Not Valid<br>Without School<br>Dry Seal<br>' +
            '<span class="fill" contenteditable="true">' +
            esc(fmtDate(new Date().toISOString().slice(0, 10))) + '</span></div>' +
            '<table class="doc-grading-table">' +
            '<caption>G R A D I N G&nbsp;&nbsp;S Y S T E M</caption>' +
            '<tbody>' + rows + '</tbody></table>' +
            '</div>';
    }

    function signatory(caption, name, role) {
        return '<div class="doc-signatory">' +
            '<p class="sig-caption">' + esc(caption) + '</p>' +
            '<span class="sig-name fill" contenteditable="true">' + esc(name) + '</span>' +
            '<p class="sig-role">' + esc(role) + '</p>' +
            '</div>';
    }

    // -------------------------------------------------------
    // Auto-fill helpers
    // -------------------------------------------------------

    function fullName(s) {
        return joinNonBlank([
            s.lastName ? s.lastName + ',' : '',
            s.firstName,
            s.middleName
        ], ' ');
    }

    function pickParent(data) {
        const parents = data.parents || [];
        const father = parents.find(function (p) { return /father/i.test(p.relation || ''); });
        const mother = parents.find(function (p) { return /mother/i.test(p.relation || ''); });
        return father || mother || parents[0] || null;
    }

    function findTesda(data, keywords) {
        return (data.tesdaQualifications || []).find(function (t) {
            const title = (t.title || '').toLowerCase();
            return keywords.every(function (k) { return title.indexOf(k) >= 0; });
        }) || null;
    }

    function findEducation(data, needle) {
        return (data.education || []).find(function (e) {
            return (e.level || '').toLowerCase().indexOf(needle) >= 0;
        }) || null;
    }

    function schoolYear(batchYear) {
        if (!batchYear) return 'SY ______';
        return 'SY ' + batchYear + '-' + (Number(batchYear) + 1);
    }

    // -------------------------------------------------------
    // Print
    // -------------------------------------------------------

    /**
     * Browsers use document.title as the suggested save-as-PDF filename, so swap in
     * "{DocumentType}-{LastName} {FirstName}" for the duration of the print dialog.
     */
    function printDocument() {
        if (!currentData || !currentTemplateKey) return;
        const originalTitle = document.title;
        document.title = printFileName();
        const restore = function () {
            document.title = originalTitle;
            window.removeEventListener('afterprint', restore);
        };
        window.addEventListener('afterprint', restore);
        window.print();
        // Fallback for browsers where afterprint does not fire reliably.
        window.setTimeout(restore, 2000);
    }

    function printFileName() {
        const s = currentData.student;
        const template = TEMPLATES[currentTemplateKey];
        const shortName = template ? template.shortName : currentTemplateKey;
        const namePart = joinNonBlank([s.lastName, s.firstName], ' ') || s.studentId;
        return sanitizeFileName(shortName + '-' + namePart);
    }

    /** Strip characters that are invalid in Windows/macOS filenames. */
    function sanitizeFileName(name) {
        return name.replace(/[\\/:*?"<>|]/g, '').replace(/\s+/g, ' ').trim();
    }

    // -------------------------------------------------------
    // Save to Documents
    // -------------------------------------------------------

    function saveDocument() {
        if (!currentData || !currentTemplateKey) return;
        const template = TEMPLATES[currentTemplateKey];
        const studentId = currentData.student.studentId;
        const variantSuffix = template && template.kind === 'FORM_IX' ? '-' + currentVariant : '';
        const fileName = editDocumentId
            ? editFileName
            : studentId + '-' + template.shortName + variantSuffix + '.html';

        const btn = $('#saveDocumentBtn');
        btn.prop('disabled', true).text('Saving...');

        loadCss().then(function (css) {
            const area = document.getElementById('documentArea').cloneNode(true);
            area.querySelectorAll('[contenteditable]').forEach(function (el) {
                el.removeAttribute('contenteditable');
            });

            const standalone = '<!DOCTYPE html>\n<html lang="en">\n<head>\n<meta charset="UTF-8">\n' +
                '<title>' + esc(currentTemplateKey) + ' — ' + esc(studentId) + '</title>\n' +
                '<style>\n' + css + '\n</style>\n</head>\n<body>\n' +
                area.innerHTML + '\n</body>\n</html>\n';

            $.ajax({
                url: '/api/registrar/documents/generate',
                method: 'POST',
                contentType: 'application/json',
                data: JSON.stringify({
                    studentId: studentId,
                    documentType: currentTemplateKey,
                    fileName: fileName,
                    html: standalone,
                    documentId: editDocumentId
                }),
                success: function () {
                    btn.text('Saved');
                    showAlert('Document saved. Returning to the Documents page…', 'success');
                    window.setTimeout(function () {
                        window.location.href = 'documents.html';
                    }, 900);
                },
                error: function (xhr) {
                    btn.prop('disabled', false).text('Save to Documents');
                    showAlert(ajaxErrorMessage(xhr, 'Failed to save the document'), 'danger');
                }
            });
        }).catch(function () {
            btn.prop('disabled', false).text('Save to Documents');
            showAlert('Could not load the document stylesheet for saving.', 'danger');
        });
    }

    function loadCss() {
        if (cachedCss !== null) return Promise.resolve(cachedCss);
        return fetch('css/document-print.css', { credentials: 'same-origin' })
            .then(function (res) {
                if (!res.ok) throw new Error('css fetch failed');
                return res.text();
            })
            .then(function (css) {
                cachedCss = css;
                return css;
            });
    }

    // -------------------------------------------------------
    // Formatting helpers
    // -------------------------------------------------------

    function fmtDate(value) {
        if (!value) return '';
        const d = new Date(value);
        if (isNaN(d.getTime())) return String(value);
        return d.toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' });
    }

    function fmtGrade(value) {
        const n = Number(value);
        return isNaN(n) ? esc(String(value)) : n.toFixed(2);
    }

    function formatTotal(value) {
        return Number.isInteger(value) ? String(value) : value.toFixed(1);
    }

    function joinNonBlank(parts, sep) {
        return parts.filter(function (p) { return p && String(p).trim(); }).join(sep);
    }

    function esc(str) {
        if (str === null || str === undefined) return '';
        const div = document.createElement('div');
        div.appendChild(document.createTextNode(String(str)));
        return div.innerHTML;
    }

    function showAlert(message, type) {
        const el = document.getElementById('generateAlert');
        el.className = 'alert alert-' + type;
        el.textContent = message;
        el.classList.remove('d-none');
    }

    function hideAlert() {
        const el = document.getElementById('generateAlert');
        el.className = 'alert d-none';
        el.textContent = '';
    }
})();