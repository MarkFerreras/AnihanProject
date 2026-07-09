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

    const SCHOOL_HEADER =
        '<div class="doc-school-header">' +
        '<p class="doc-school-name">Anihan Technical School</p>' +
        '<p class="doc-school-sub">(For Women-in-Development)</p>' +
        '<p class="doc-school-sub">A project of the Foundation for Professional Training, Inc. (FPTI)</p>' +
        '</div>';

    const CONTACT_FOOTER =
        '<div class="doc-contact-footer">' +
        'Email: info@anihan.edu.ph / anihanschool@gmail.com / registrar@anihan.edu.ph * Web: anihan.edu.ph<br>' +
        '294 Purok 6 Brgy. Milagrosa, Calamba City, Laguna 4027 Philippines * (049) 545-1598 * 0939-2666108' +
        '</div>';

    $(document).ready(function () {
        document.body.classList.add('document-toolbar-active');
        loadStudentsDatalist();
        populateTemplateSelect();
        $('#generateTemplate').on('change', toggleVariantSelect);
        $('#loadDocumentBtn').on('click', loadDocument);
        $('#printDocumentBtn').on('click', function () { window.print(); });
        $('#saveDocumentBtn').on('click', saveDocument);
        toggleVariantSelect();

        const params = new URLSearchParams(window.location.search);
        if (params.get('studentId')) {
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
    // Load + render
    // -------------------------------------------------------

    function loadDocument() {
        const studentId = $('#generateStudentId').val().trim();
        const templateKey = $('#generateTemplate').val();
        if (!studentId || !templateKey) {
            showAlert('Please pick a student and a template.', 'danger');
            return;
        }

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
                document.getElementById('documentArea').scrollIntoView({ behavior: 'smooth' });
            },
            error: function (xhr) {
                const msg = xhr.responseJSON?.message || 'Failed to load student data.';
                showAlert(msg, 'danger');
            }
        });
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
                const grade = gradesByCode[subj.code];
                const finalGrade = grade && grade.finalGrade != null ? fmtGrade(grade.finalGrade) : '';
                const reExam = grade && grade.reExamGrade != null ? fmtGrade(grade.reExamGrade) : '';
                const remarks = grade
                    ? (grade.remarks || (grade.finalGrade != null ? 'Competent' : ''))
                    : '';
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
    // Save to Documents
    // -------------------------------------------------------

    function saveDocument() {
        if (!currentData || !currentTemplateKey) return;
        const template = TEMPLATES[currentTemplateKey];
        const studentId = currentData.student.studentId;
        const variantSuffix = template.kind === 'FORM_IX' ? '-' + currentVariant : '';
        const fileName = studentId + '-' + template.shortName + variantSuffix + '.html';

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
                    html: standalone
                }),
                success: function () {
                    btn.prop('disabled', false).text('Save to Documents');
                    showAlert('Document saved. It is now available on the Documents page.', 'success');
                },
                error: function (xhr) {
                    btn.prop('disabled', false).text('Save to Documents');
                    const msg = xhr.responseJSON?.message || 'Failed to save the document.';
                    showAlert(msg, 'danger');
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