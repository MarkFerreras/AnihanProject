/**
 * curriculum-templates.js — Static transcription of the official document templates
 * in document-templates/ (TOR.pdf and the three Form IX Student Permanent Records).
 *
 * The subjects table in the live database holds only the handful of subjects used
 * for class scheduling; the printed documents carry the full fixed curriculum with
 * hours/units mandated by the templates. This file is the source of truth for the
 * printed layout. Grades are merged in at generation time by subject code.
 */
(function () {
    'use strict';

    const BASIC = {
        label: 'BASIC COMPETENCIES:',
        subjects: [
            { code: '400311210', title: 'Participating in Workplace Communication', hours: '2', units: '0.11' },
            { code: '400311211', title: 'Working in a Team Environment', hours: '2', units: '0.11' },
            { code: '400311212', title: 'Solving and Addressing General Workplace Problems', hours: '2', units: '0.11' },
            { code: '400311213', title: 'Developing Career and Life Decisions', hours: '2', units: '0.11' },
            { code: '400311214', title: 'Contributing to Workplace Innovation', hours: '2', units: '0.11' },
            { code: '400311215', title: 'Presenting Relevant Information', hours: '2', units: '0.11' },
            { code: '400311216', title: 'Practicing Occupational Safety and Health Policies and Procedures', hours: '2', units: '0.11' },
            { code: '400311217', title: 'Exercising Efficient and Effective Sustainable Practices in the Workplace', hours: '2', units: '0.11' },
            { code: '400311218', title: 'Practicing Entrepreneurial Skills in the Workplace', hours: '2', units: '0.11' }
        ]
    };

    const COMMON = {
        label: 'COMMON COMPETENCIES:',
        subjects: [
            { code: 'TRS311201', title: 'Developing and Updating Industry Knowledge', hours: '4.5', units: '0.25' },
            { code: 'TRS311202', title: 'Observing Workplace Hygiene Procedures', hours: '4.5', units: '0.25' },
            { code: 'TRS311203', title: 'Performing Computer Operations', hours: '24', units: '1.33' },
            { code: 'TRS311204', title: 'Performing Workplace and Safety Practices', hours: '4.5', units: '0.25' },
            { code: 'TRS311205', title: 'Providing Effective Customer Service', hours: '4.5', units: '0.25' }
        ]
    };

    const CORE_BPP = {
        label: 'CORE COMPETENCIES-BREAD and PASTRY PRODUCTION NC II:',
        subjects: [
            { code: 'TRS741342', title: 'Preparing and Presenting Gateaux, Tortes and Cakes', hours: '38', units: '2.11' },
            { code: 'TRS741343', title: 'Presenting Desserts', hours: '10', units: '0.56' },
            { code: 'TRS741344', title: 'Preparing and Displaying Petit Fours', hours: '10', units: '0.56' },
            { code: 'TRS741379', title: 'Preparing and Producing Bakery Products', hours: '38', units: '2.11' },
            { code: 'TRS741380', title: 'Preparing and Producing Pastry Products', hours: '40', units: '2.22' }
        ]
    };

    const CORE_FBS = {
        label: 'CORE COMPETENCIES - FOOD AND BEVERAGE SERVICES NC II:',
        subjects: [
            { code: 'TRS512387', title: 'Preparing the Dining Room/Restaurant Area for Service', hours: '63', units: '3.50' },
            { code: 'TRS512388', title: 'Welcoming Guests and Take Food and Beverage Orders', hours: '60', units: '3.33' },
            { code: 'TRS512389', title: 'Promoting Food and Beverage Products', hours: '26', units: '1.44' },
            { code: 'TRS512390', title: 'Providing Food and Beverage Services to Guests', hours: '60', units: '3.33' },
            { code: 'TRS512391', title: 'Providing Room Service', hours: '60', units: '3.33' },
            { code: 'TRS512392', title: 'Receiving and Handling Guest Concerns', hours: '27', units: '1.50' }
        ]
    };

    const CORE_COOKERY = {
        label: 'CORE COMPETENCIES-COOKERY NC II:',
        subjects: [
            { code: 'TRS512328', title: 'Cleaning and Maintaining Kitchen Premises', hours: '13', units: '0.72' },
            { code: 'TRS512330', title: 'Preparing Sandwiches', hours: '12', units: '0.67' },
            { code: 'TRS512331', title: 'Preparing Stocks, Sauces, and Soups', hours: '24', units: '1.33' },
            { code: 'TRS512333', title: 'Preparing Poultry and Game Dishes', hours: '24', units: '1.33' },
            { code: 'TRS512334', title: 'Preparing Seafood Dishes', hours: '27', units: '1.50' },
            { code: 'TRS512335', title: 'Preparing Desserts', hours: '24', units: '1.33' },
            { code: 'TRS512340', title: 'Packaging Prepared Food', hours: '9', units: '0.50' },
            { code: 'TRS512381', title: 'Preparing Appetizers', hours: '12', units: '0.67' },
            { code: 'TRS512382', title: 'Preparing Salads and Dressings', hours: '12', units: '0.67' },
            { code: 'TRS512383', title: 'Preparing Meat Dishes', hours: '27', units: '1.50' },
            { code: 'TRS512384', title: 'Preparing Vegetable Dishes', hours: '36', units: '2.00' },
            { code: 'TRS512385', title: 'Preparing Egg Dishes', hours: '24', units: '1.33' },
            { code: 'TRS512386', title: 'Preparing Starch Dishes', hours: '36', units: '2.00' }
        ]
    };

    const OTHER_FIRST_SEM = {
        label: 'OTHER SUBJECTS',
        subjects: [
            { code: 'BFS', title: 'Basic Food Safety', hours: '18', units: '1.00' },
            { code: 'ENG', title: 'English', hours: '18', units: '1.00' },
            { code: 'ENTREP', title: 'Entrepreneurship', hours: '24', units: '1.33' },
            { code: 'MATH', title: 'Culinary Math', hours: '18', units: '1.00' },
            { code: 'CL 1', title: 'Christian Living 1', hours: '18', units: '1.00' },
            { code: 'CL 2', title: 'Christian Living 2', hours: '18', units: '1.00' },
            { code: 'PD', title: 'Personality Development 1', hours: '18', units: '1.00' },
            { code: 'VE 1', title: 'Values Education 1', hours: '18', units: '1.00' }
        ]
    };

    const OTHER_SECOND_SEM = {
        label: 'OTHER SUBJECTS',
        subjects: [
            { code: 'OJT', title: 'On-the-Job-Training', hours: '900', units: '50.00' },
            { code: 'CL 3', title: 'Christian Living 3', hours: '18', units: '1.00' },
            { code: 'CL 4', title: 'Christian Living 4', hours: '18', units: '1.00' },
            { code: 'VE 2', title: 'Values Education 2', hours: '18', units: '1.00' }
        ]
    };

    const GRADING_SYSTEM = [
        ['99-100', '1.00', 'Excellent', '75-77', '3.00', 'Passed'],
        ['96-98', '1.25', 'Very Good', 'C', '', 'Complete'],
        ['93-95', '1.50', 'Very Good', '70-74', '4.00', 'Conditional Failure'],
        ['90-92', '1.75', 'Good', 'Below 70', '5.00', 'Failure'],
        ['87-89', '2.00', 'Good', 'FA', '', 'Failure Due to Absences'],
        ['84-86', '2.25', 'Good', 'INC', '', 'Incomplete'],
        ['81-83', '2.50', 'Satisfactory', 'D', '', 'Dropped'],
        ['78-80', '2.75', 'Satisfactory', '', '', '']
    ];

    /** Template definitions keyed by the backend document-type strings. */
    const TEMPLATES = {
        'Transcript of Records (TOR)': {
            kind: 'TOR',
            shortName: 'TOR',
            firstSemesterSections: [BASIC, COMMON, CORE_BPP, CORE_FBS, CORE_COOKERY, OTHER_FIRST_SEM],
            secondSemesterSections: [OTHER_SECOND_SEM]
        },
        'Form IX - Bread and Pastry Production NC II': {
            kind: 'FORM_IX',
            shortName: 'FormIX-BPP',
            qualification: 'BREAD and PASTRY PRODUCTION NC II',
            qualificationUnderline: 'BREAD AND PASTRY PRODUCTION -NC II',
            firstSemesterSections: [BASIC, COMMON, CORE_BPP]
        },
        'Form IX - Cookery NC II': {
            kind: 'FORM_IX',
            shortName: 'FormIX-Cookery',
            qualification: 'COOKERY NC II',
            qualificationUnderline: 'COOKERY -NC II',
            firstSemesterSections: [BASIC, COMMON, CORE_COOKERY]
        },
        'Form IX - Food and Beverage Services NC II': {
            kind: 'FORM_IX',
            shortName: 'FormIX-FBS',
            qualification: 'FOOD AND BEVERAGE SERVICES NC II',
            qualificationUnderline: 'FOOD AND BEVERAGE SERVICES -NC II',
            firstSemesterSections: [BASIC, COMMON, CORE_FBS]
        }
    };

    window.AnihanCurriculum = {
        TEMPLATES: TEMPLATES,
        GRADING_SYSTEM: GRADING_SYSTEM
    };
})();