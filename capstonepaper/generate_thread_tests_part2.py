# -*- coding: utf-8 -*-
"""
Generates "NEW ANIHAN Thread Testing Cases.xlsx" - a second batch of thread
testing cases covering the parts of the Anihan SRMS built AFTER the original
82 cases in "OLD ANIHAN Thread Testing Cases.xlsx" were written.

Numbering continues from TC-082 so the two sheets can be read side by side.
Format (columns, fonts, banding, dropdown) mirrors the old workbook exactly.

2026-09-16: document management and document generation cases (originally
TC-083-TC-106) were removed at the user's request - that feature is out of
testing scope for now. The remaining cases were renumbered so the suite stays
continuous with no gaps. If documents come back into scope, restore those
blocks from git history and renumber from the end of this file.
"""
import openpyxl
from openpyxl.styles import Font, Alignment, PatternFill, Border, Side
from openpyxl.worksheet.datavalidation import DataValidation

HEADERS = ["Test ID", "Title", "Pre-Conditions", "Test Steps",
           "Expected Results", "Tester", "Test Date", "Results", "Comments"]

# (title, pre-conditions, steps, expected)
CASES = []


def add(title, pre, steps, expected):
    CASES.append((title, pre, steps, expected))


# =====================================================================
# STUDENT NUMBER - SINGLE ASSIGNMENT
# =====================================================================

add("A newly enrolled student has no student number yet",
    """You are logged in as registrar.
A student has just submitted their details through the Student Portal.

Setup steps:
Dependency: complete a Student Portal enrollment for a new student first (see TC-074 in the previous test sheet).
1. Log in as registrar.
2. Stay on the Registrar home page (Student Records).""",
    """1. Find the newly submitted student in the table.
2. Look at the 'Reference No.' and 'Student Number' columns for that student.""",
    """The 'Reference No.' column should show an automatically created code beginning with SR (for example SR20260018).
The 'Student Number' column should show an orange 'Not Assigned' badge.
The system must NOT invent a student number on its own - real student numbers come from the school's records and are entered by the registrar.""")

add("Registrar can assign a student number to a student",
    """You are logged in as registrar.
A student without a student number exists.

Setup steps:
Dependency: a student showing the 'Not Assigned' badge must exist.
1. Log in as registrar.
2. Stay on the Registrar home page.""",
    """1. Click 'Assign Number' on that student's row.
2. Type a student number, for example 2026-0101.
3. Click Save.""",
    """A success message should appear and the window should close.
The 'Not Assigned' badge should be replaced by the number you typed.
The activity logs should show an entry saying the student number was assigned to that student.""")

add("A student number already used by someone else is rejected",
    """You are logged in as registrar.
Two students exist and one of them already has a student number.

Setup steps:
Dependency: assign a student number to one student first, and note that number.
1. Log in as registrar.
2. Stay on the Registrar home page.""",
    """1. Click 'Assign Number' on the OTHER student's row.
2. Type the exact same number that the first student already has.
3. Click Save.""",
    """The system should refuse to save.
The message should say the number is already assigned and name the student who holds it, so the registrar knows where to look.
The first student's number should stay exactly as it was.
No activity log entry should be written for the rejected attempt.""")

add("A student number with invalid characters is rejected",
    """You are logged in as registrar and a student is open for numbering.

Setup steps:
1. Log in as registrar.
2. On the Registrar home page, click 'Assign Number' on any student row.""",
    """1. Type a number that contains a space and a symbol, for example: 2026 0101!
2. Click Save.""",
    """The system should refuse to save.
A message should appear directly under the input box explaining which characters are allowed (letters, numbers, hyphens and slashes).
It should be a specific message, not a general 'Validation failed'.""")

add("A student number longer than 20 characters is rejected",
    """You are logged in as registrar and a student is open for numbering.

Setup steps:
1. Log in as registrar.
2. On the Registrar home page, click 'Assign Number' on any student row.""",
    """1. Type a long value of more than 20 characters, for example: 2026-0101-0101-0101-0101
2. Click Save.""",
    """The system should refuse to save.
A message should say the student number can be at most 20 characters long.""")

add("Registrar can clear a student number",
    """You are logged in as registrar.
A student already has a student number assigned.

Setup steps:
Dependency: assign a student number to a student first.
1. Log in as registrar.
2. Stay on the Registrar home page.""",
    """1. Click 'Assign Number' on that student's row.
2. Delete everything in the box so it is empty.
3. Click Save.""",
    """The student number should be removed.
The orange 'Not Assigned' badge should come back on that row.
The activity logs should show an entry saying the student number was cleared.""")

add("Registrar can filter students by Assigned or Not Assigned",
    """You are logged in as registrar.
Some students have student numbers and some do not.

Setup steps:
Dependency: assign numbers to a couple of students and leave the rest unassigned.
1. Log in as registrar.
2. Stay on the Registrar home page.""",
    """1. Set the 'Student No.' filter to 'Not Assigned'.
2. Set it to 'Assigned'.
3. Combine 'Not Assigned' with a Status filter such as 'Active'.
4. Click Reset.""",
    """'Not Assigned' should show only students with the orange badge.
'Assigned' should show only students who have a number.
The two filters should work together, showing only students matching both conditions.
Reset should clear all the filters and show everyone again.""")

add("Registrar can find a student by their student number",
    """You are logged in as registrar.
A student has a student number assigned.

Setup steps:
Dependency: assign a student number to a student first and note it down.
1. Log in as registrar.
2. Stay on the Registrar home page.""",
    """1. Type that student number into the search box above the table.""",
    """The student should be found.
Searching should work with the student number as well as with names and the Reference No.""")

add("Student number cannot be typed into on the edit form",
    """You are logged in as registrar.
A student with a student number exists.

Setup steps:
Dependency: assign a student number to a student first.
1. Log in as registrar.
2. On the Registrar home page, open that student's details and click Edit.""",
    """1. Find the Student Number box on the edit form.
2. Try to click inside it and type.""",
    """The box should show the current student number but should be greyed out and not allow typing.
A short note near it should point to the 'Assign Number' action as the place to change it.
This is on purpose - having only one place to change the number means every change is deliberate and recorded.""")

add("Editing the rest of a student's record does not erase their student number",
    """You are logged in as registrar.
A student with a student number exists.

Setup steps:
Dependency: assign a student number to a student and note it down.
1. Log in as registrar.
2. On the Registrar home page, open that student's record and click Edit.""",
    """1. Change some ordinary details, for example the contact number and the address.
2. Click Save.
3. Go back to the Registrar home page and look at that student's Student Number column.""",
    """The changes should save normally.
The student number should still be exactly as it was before - saving the edit form must never wipe it.
This is an important check: it would be easy for a routine edit to quietly clear the number.""")

add("Reference No. and Student Number are labelled clearly everywhere",
    """You are logged in as registrar.
A student record exists.

Setup steps:
1. Log in as registrar.""",
    """1. Look at the column headings on the Registrar home table.
2. Open a student's details window and read the first two cards.
3. Open the Edit form and read the labels in the identifiers row.
4. Open the Student Portal confirmation message shown after a student submits.""",
    """The automatic SR... code should always be labelled 'Reference No.' - never called the student number.
The number the registrar assigns should always be labelled 'Student Number'.
The student portal message should tell the student that the registrar assigns their student number later.
The two must never be mixed up, because only one of them is the real school student number.""")

# =====================================================================
# STUDENT NUMBERS PAGE - EXPORT / IMPORT
# =====================================================================

add("Student Numbers page shows how many students still need a number",
    """You are logged in as registrar.
Some students have student numbers and some do not.

Setup steps:
Dependency: make sure at least one student still has no student number.
1. Log in as registrar.
2. From the top menu, click 'Student Numbers'.""",
    """1. Read the summary line near the top of the page.
2. Count the 'Not Assigned' badges in the table below and compare.""",
    """The summary line should read something like '6 of 10 students still need a student number'.
It should stand out (highlighted) while any student is still missing a number.
The count should match what the table actually shows.""")

add("Registrar can export the filtered student list to Excel",
    """You are logged in as registrar and the Student Numbers page is open.
At least one student has no student number.

Setup steps:
1. Log in as registrar and click 'Student Numbers'.""",
    """1. Set the 'Student No.' filter to 'Not Assigned' and click Apply.
2. In the Export box, choose 'Excel (.xlsx)'.
3. Click Export.
4. Open the downloaded file in Excel.""",
    """An Excel file should download with a name that includes the date.
It should open in Excel without any repair warning.
It should contain one row per student shown by the filter - students who already have a number should not be in it.
The first row should be the column headings (Reference No., names, batch, course, section) with 'Student Number' as the last column, left blank and ready to be typed into.""")

add("Registrar can export the list as a CSV file",
    """You are logged in as registrar and the Student Numbers page is open.

Setup steps:
1. Log in as registrar and click 'Student Numbers'.""",
    """1. In the Export box, choose 'CSV (.csv)'.
2. Click Export.
3. Open the downloaded file in Excel or Notepad.""",
    """A .csv file should download and open correctly.
The columns should be the same as the Excel export, with the Student Number column blank at the end.
Names containing commas should still line up in the correct columns.""")

add("An exported sheet can be imported back unchanged",
    """You are logged in as registrar and the Student Numbers page is open.

Setup steps:
Dependency: export a sheet first and do not edit it at all.
1. Log in as registrar and click 'Student Numbers'.""",
    """1. In the Import box, choose the file you just exported.
2. Click 'Preview Import'.""",
    """The system should read every row without complaining about unrecognised columns - the export is written using the same column names the import understands.
Because the Student Number column is still empty, every row should be reported as blank / nothing to do.
This proves the export and import work together as a round trip.""")

add("Import preview does not change anything",
    """You are logged in as registrar and the Student Numbers page is open.
You have an exported sheet with a few student numbers typed in.

Setup steps:
Dependency: export a sheet, type valid student numbers into two or three rows, and save the file.
1. Log in as registrar and click 'Student Numbers'.""",
    """1. Choose the completed file in the Import box.
2. Click 'Preview Import'.
3. Look at the student table on the same page.
4. Log in as admin in another session and check the activity logs.""",
    """A results table should appear showing what would happen to each row.
The page should make it clear this is only a preview.
NO student number in the table should have changed yet.
NO new rows should appear in the activity logs - a preview must never write anything.""")

add("Apply is only available after a preview has been run",
    """You are logged in as registrar and the Student Numbers page is open.
You have two different completed import files ready.

Setup steps:
Dependency: prepare two different completed sheets so you can switch between them.
1. Log in as registrar and click 'Student Numbers'.""",
    """1. Look at the 'Apply' button before doing anything.
2. Choose the first file and click 'Preview Import'.
3. Look at the 'Apply' button again.
4. Without clicking Apply, choose the SECOND file in the Import box.
5. Look at the 'Apply' button once more.""",
    """'Apply' should be switched off at the start.
It should switch on only after a preview has been run.
Choosing a different file should switch it off again and clear the old preview, so you can never apply a preview that belongs to a different file.""")

add("Apply assigns the good rows and reports the rest",
    """You are logged in as registrar and the Student Numbers page is open.
You have a completed sheet containing a mix of good rows and problem rows.

Setup steps:
Dependency: prepare one sheet that contains two valid new numbers plus at least one blank row and one row with a made-up Reference No.
1. Log in as registrar and click 'Student Numbers'.
2. Choose the file and click 'Preview Import'.""",
    """1. Click 'Apply'.
2. Read the results table and the summary line.
3. Look at the student table further down the page.""",
    """Only the valid rows should be saved - the problem rows should be skipped, not applied.
The results table should show a clear outcome for each row with the spreadsheet row number.
The summary should say how many were assigned and how many were skipped.
The student table and the 'still need a student number' line should update to match.""")

add("A student's existing number is not overwritten by accident",
    """You are logged in as registrar and the Student Numbers page is open.
A student already has a student number.

Setup steps:
Dependency: assign a student number to a student, then prepare a sheet that gives that same student a DIFFERENT number.
1. Log in as registrar and click 'Student Numbers'.
2. Leave the 'Allow overwriting existing student numbers' box unticked.""",
    """1. Choose the file and click 'Preview Import'.
2. Read the result for that student's row.
3. Click 'Apply' and check the student's number in the table afterwards.""",
    """That row should be reported as a conflict because the student already has a different number.
The message should mention the tick box that allows overwriting, so the registrar knows what to do next.
The student's stored number should stay exactly as it was - a stale spreadsheet must never quietly rewrite student identities.""")

add("Overwriting works when it is explicitly allowed",
    """You are logged in as registrar and the Student Numbers page is open.
You have the same sheet used in the previous test.

Setup steps:
Dependency: use the sheet that gives an already-numbered student a different number.
1. Log in as registrar and click 'Student Numbers'.""",
    """1. Tick 'Allow overwriting existing student numbers'.
2. Choose the file and click 'Preview Import'.
3. Read the result for that student's row.
4. Click 'Apply'.""",
    """With the box ticked, the row should now be reported as an overwrite instead of a conflict.
After applying, the student should have the new number from the sheet.
The change should appear in the activity logs.""")

add("The same number appearing twice in one file blocks both rows",
    """You are logged in as registrar and the Student Numbers page is open.
You have a sheet where two different students were given the same student number by mistake.

Setup steps:
Dependency: prepare a sheet where two rows carry the identical student number.
1. Log in as registrar and click 'Student Numbers'.""",
    """1. Choose the file and click 'Preview Import'.
2. Read the results for both rows.
3. Click 'Apply' and check both students in the table.""",
    """Both rows should be reported as duplicates within the file and both should be skipped.
Neither student should receive the number, because the system cannot know which one was meant.
The rest of the file should still be processed normally.""")

add("An unknown Reference No. is reported, not silently ignored",
    """You are logged in as registrar and the Student Numbers page is open.
You have a sheet containing a Reference No. that does not exist in the system.

Setup steps:
Dependency: prepare a sheet with one row whose Reference No. is made up (for example SR99999999).
1. Log in as registrar and click 'Student Numbers'.""",
    """1. Choose the file and click 'Preview Import'.
2. Find that row in the results table.""",
    """The row should be clearly reported as an unknown reference.
The spreadsheet row number should be shown so the registrar can find and correct it.
The row should be skipped rather than quietly dropped without mention.""")

add("Student numbers with leading zeros survive the Excel round trip",
    """You are logged in as registrar and the Student Numbers page is open.

Setup steps:
Dependency: export a sheet to Excel and open it in Excel.
1. Log in as registrar and click 'Student Numbers'.""",
    """1. In the exported Excel file, type 0012 as the student number for one row.
2. Save the Excel file.
3. Back on the Student Numbers page, choose that file, click 'Preview Import', then 'Apply'.
4. Look at that student's Student Number in the table.""",
    """The stored student number should be 0012 - with the leading zeros kept.
It must NOT become 12. Excel likes to drop leading zeros, so this is an important check for real archive numbers.""")

add("The import still finds the headings when there are extra title rows above them",
    """You are logged in as registrar and the Student Numbers page is open.

Setup steps:
Dependency: export a sheet, then open it and insert two rows at the very top containing free text such as 'Anihan Technical School' and 'Student Master List 2026'.
1. Log in as registrar and click 'Student Numbers'.""",
    """1. Type a few valid student numbers into the sheet.
2. Save the file.
3. Choose the file and click 'Preview Import'.""",
    """The system should still find the real heading row underneath the two title rows and read the data correctly.
The row numbers shown in the results should match the actual row numbers in the spreadsheet, so they are easy to find.
This matters because school-supplied files often have a title block at the top.""")

add("A name that disagrees with the record is flagged but still applied",
    """You are logged in as registrar and the Student Numbers page is open.

Setup steps:
Dependency: export a sheet, then change the Last Name on one row to a different name while leaving the Reference No. as it is.
1. Log in as registrar and click 'Student Numbers'.""",
    """1. Type a valid student number on that row.
2. Save the file, choose it and click 'Preview Import'.
3. Read that row's result.
4. Click 'Apply'.""",
    """The row should be marked as a name mismatch so the registrar can double-check it.
The number should still be assigned, because the Reference No. is what identifies the student.
The warning is there to catch rows that have accidentally slipped out of line in a hand-edited sheet.""")

add("Bulk import is recorded in the activity logs",
    """You are logged in as admin.
A registrar has just applied an import that assigned some numbers and skipped others.

Setup steps:
Dependency: run an import that assigns two numbers and skips a few rows.
1. Log in as admin.
2. From the top menu, click 'Logs'.""",
    """1. Look through the most recent log entries.""",
    """There should be one entry for each student number that was actually assigned.
There should also be one summary entry naming the imported file and giving the counts, for example '2 assigned, 5 skipped'.
There should be NO entries for the rows that were skipped, and none at all from a preview.""")

# =====================================================================
# TESDA GRADING
# =====================================================================

add("Trainer enters a percentage and the system works out the equivalent grade",
    """You are logged in as a trainer who owns a class with enrolled students.

Setup steps:
Dependency: the trainer must own a class with enrolled students (see TC-045 and TC-048 in the previous test sheet).
1. Log in as the trainer account.
2. Click 'My Classes' and click 'Input Grades' on a class.""",
    """1. In the 'Final %' box for one student, type 88.
2. In the 'Hours Rendered' box for that student, type 40.
3. Look at the Equivalent and Remarks columns on that row.
4. Click 'Save Grades'.""",
    """The Equivalent column should immediately show 2.00 - the trainer only types the raw percentage and the system does the conversion.
The Remarks column should show 'Competent'.
After saving and reopening the window, the same values should still be there.""")

add("The percentage to equivalent conversion follows the TESDA table",
    """You are logged in as a trainer with a class open in the Grade Input window.

Setup steps:
Dependency: the trainer must own a class with at least one enrolled student.
1. Log in as the trainer account.
2. Click 'My Classes' and click 'Input Grades' on a class.""",
    """1. Type each of these values into the 'Final %' box in turn and note the Equivalent shown: 99, 96, 93, 90, 87, 84, 81, 78, 75, 74.99, 69.""",
    """The equivalents should be, in the same order: 1.00, 1.25, 1.50, 1.75, 2.00, 2.25, 2.50, 2.75, 3.00, 4.00, 5.00.
Note the two boundary cases: 75 gives 3.00 (the lowest passing mark) while 74.99 already gives 4.00.
Anything above 3.00 counts as a failing mark.""")

add("Percentages outside 0 to 100 are refused",
    """You are logged in as a trainer with a class open in the Grade Input window.

Setup steps:
1. Log in as the trainer account.
2. Click 'My Classes' and click 'Input Grades' on a class.""",
    """1. Type 120 in the 'Final %' box for a student, fill in Hours Rendered and click 'Save Grades'.
2. Try again with -5.""",
    """Both attempts should be refused.
A message should say the percentage must be between 0 and 100 and should name the student concerned.
Nothing should be saved for that student.""")

add("Hours Rendered is required for every graded student",
    """You are logged in as a trainer with a class open in the Grade Input window.

Setup steps:
1. Log in as the trainer account.
2. Click 'My Classes' and click 'Input Grades' on a class.""",
    """1. Type 85 in the 'Final %' box for one student.
2. Leave the 'Hours Rendered' box empty.
3. Click 'Save Grades'.""",
    """The system should refuse to save.
A message should say Hours Rendered is required and name the student.
Filling in the hours and saving again should then work.""")

add("Hours Rendered above 100 is refused",
    """You are logged in as a trainer with a class open in the Grade Input window.

Setup steps:
1. Log in as the trainer account.
2. Click 'My Classes' and click 'Input Grades' on a class.""",
    """1. Type 85 in the 'Final %' box and 250 in the 'Hours Rendered' box for one student.
2. Click 'Save Grades'.""",
    """The system should refuse to save.
A message should say Hours Rendered must be between 0 and 100 and name the student.""")

add("Trainer can record a status code instead of a percentage",
    """You are logged in as a trainer with a class open in the Grade Input window.
The class has at least four enrolled students.

Setup steps:
Dependency: the class should have enough students to try each status.
1. Log in as the trainer account.
2. Click 'My Classes' and click 'Input Grades' on a class.""",
    """1. Leave the 'Final %' box empty for a student and choose 'Complete' in the Status dropdown. Fill in Hours Rendered.
2. Do the same for other students using 'Dropped', 'Incomplete' and 'Failure Due to Absences'.
3. Click 'Save Grades'.""",
    """All four should save successfully.
The Equivalent column should stay empty for these students - a status is recorded instead of a number.
Remarks should show 'Competent' for Complete, 'Not Competent' for Failure Due to Absences, and stay blank for Incomplete and Dropped.""")

add("A percentage and a status cannot both be recorded for the same student",
    """You are logged in as a trainer with a class open in the Grade Input window.

Setup steps:
1. Log in as the trainer account.
2. Click 'My Classes' and click 'Input Grades' on a class.""",
    """1. Type 85 in the 'Final %' box for a student and then try to use the Status dropdown on the same row.
2. Clear the percentage, choose a Status, and then try to type into the 'Final %' box.""",
    """Once a percentage is typed, the Status dropdown on that row should become unavailable, and once a status is chosen the percentage box should become unavailable.
If both are somehow sent through anyway, the system should refuse to save and explain that each graded student needs exactly one of the two.""")

add("The Re-exam column only appears when the final grade is a failing mark",
    """You are logged in as a trainer with a class open in the Grade Input window.

Setup steps:
1. Log in as the trainer account.
2. Click 'My Classes' and click 'Input Grades' on a class.""",
    """1. Type 60 in the 'Final %' box for a student and look at the 'Re-exam %' box on that row.
2. Change the percentage to 85 and look at the same box again.""",
    """With 60 (which converts to 5.00, a failing mark) the Re-exam box should become available.
With 85 (which converts to 2.00, a pass) the Re-exam box should become unavailable again.
A re-exam should never be possible for a student who already passed.""")

add("A passed re-exam replaces the failing mark",
    """You are logged in as a trainer with a class open in the Grade Input window.

Setup steps:
1. Log in as the trainer account.
2. Click 'My Classes' and click 'Input Grades' on a class.""",
    """1. Type 60 in the 'Final %' box for a student and 40 in Hours Rendered.
2. Type 80 in the 'Re-exam %' box.
3. Look at the Re-exam Equiv and Remarks columns.
4. Click 'Save Grades' and reopen the window.""",
    """The Re-exam Equiv should show 2.50 and the Remarks should change to 'Competent'.
The original failing final percentage and its 5.00 equivalent should still be visible - the record keeps both, it does not erase the first attempt.
Everything should still be there after saving and reopening.""")

add("A re-exam cannot be recorded together with a status code",
    """You are logged in as a trainer with a class open in the Grade Input window.

Setup steps:
1. Log in as the trainer account.
2. Click 'My Classes' and click 'Input Grades' on a class.""",
    """1. Choose 'Incomplete' in the Status dropdown for a student.
2. Try to enter a re-exam percentage for that same student.
3. Click 'Save Grades'.""",
    """The re-exam box should not be usable for a student recorded with a status.
If a re-exam is sent through anyway, the system should refuse to save and explain that a re-exam cannot go with a status code.""")

add("Locked grades cannot be changed",
    """You are logged in as a trainer whose class already has grades entered.

Setup steps:
Dependency: enter and save grades for a class first.
1. Log in as the trainer account.
2. Click 'My Classes' and click 'Input Grades' on that class.""",
    """1. Click 'Lock Grades'.
2. Close the window and open 'Input Grades' for the same class again.
3. Try to type in any of the grade boxes.
4. Look at the 'Save Grades' and 'Lock Grades' buttons.""",
    """After locking, all the grade boxes should be greyed out and impossible to type in.
'Save Grades' and 'Lock Grades' should be switched off, and only 'Unlock Grades' should be available.
Even if a change were somehow sent through, the system should refuse it and say the grade is locked.""")

add("Grades cannot be saved for a student who is not in the class",
    """You are logged in as a trainer who owns at least one class.
A student exists who is NOT enrolled in that class.

Setup steps:
Dependency: this check goes past the on-screen form, so ask a developer to help send the request directly.
1. Log in as the trainer account.
2. Note the class number and the Reference No. of a student who is not enrolled in it.""",
    """1. With a developer's help, send a grade save for that class using the Reference No. of the student who is not enrolled.""",
    """The system should refuse.
The message should say that the student is not enrolled in that class.
No grade row should be created for that student.""")

add("Registrar can see the student's Total GWA on the details window",
    """You are logged in as registrar.
A student has grades recorded in at least two subjects with different unit values.

Setup steps:
Dependency: have a trainer enter and save grades for a student in two or more classes first.
1. Log in as registrar.
2. On the Registrar home page, find that student.""",
    """1. Click to open the student's details window.
2. Find the 'Total GWA' card and read the value.
3. Also open the details of a student who has no grades at all.""",
    """The Total GWA should show a weighted average of the student's equivalents, where subjects with more units count more.
Where a student passed on a re-exam, the re-exam result should be the one counted.
For a student with no grades yet, the Total GWA should be blank or show 'Not Available' rather than 0 or an error.""")

# =====================================================================
# SUBJECTS - COMPETENCY TYPE AND CODE RENAME
# =====================================================================

add("A Core subject must have a qualification",
    """You are logged in as registrar.
At least one qualification exists in the system.

Setup steps:
1. Log in as registrar.
2. From the top menu, click 'Subjects'.""",
    """1. Click 'Create Subject'.
2. Fill in a subject code, a subject name and the units.
3. Set Competency Type to 'Core'.
4. Leave the Qualification dropdown unselected and click Save.""",
    """The Qualification dropdown should appear as soon as 'Core' is chosen.
The system should refuse to save and explain that a qualification is required for Core subjects.
Core subjects belong to one specific qualification, which is why this is required.""")

add("Basic and Common subjects are saved without a qualification",
    """You are logged in as registrar and the Subjects page is open.

Setup steps:
1. Log in as registrar.
2. From the top menu, click 'Subjects'.""",
    """1. Click 'Create Subject'.
2. Fill in a subject code, subject name and units.
3. Set Competency Type to 'Basic' and watch the Qualification dropdown.
4. Click Save.
5. Repeat with Competency Type set to 'Common'.""",
    """The Qualification dropdown should disappear when Basic or Common is chosen, because these subjects are shared across all qualifications.
Both subjects should save successfully.
In the subjects table, the Qualification column for those rows should be empty rather than showing a wrong qualification.""")

add("The Subjects table shows the Competency Type",
    """You are logged in as registrar and the Subjects page is open.
Subjects of more than one competency type exist.

Setup steps:
Dependency: create at least one Core subject and one Basic or Common subject.
1. Log in as registrar.
2. From the top menu, click 'Subjects'.""",
    """1. Look at the columns of the subjects table.
2. Sort the table by the Competency Type column.""",
    """There should be a 'Competency Type' column showing Basic, Common or Core for every subject.
No subject should have an empty competency type.
Sorting by that column should work.""")

add("Renaming a subject code carries its classes and grades with it",
    """You are logged in as registrar.
A subject exists that is used by at least one class which already has grades.

Setup steps:
Dependency: a subject with a class and saved grades must exist (see the grading tests above).
1. Log in as registrar.
2. From the top menu, click 'Subjects'.""",
    """1. Click 'Edit' on that subject.
2. Change the Subject Code to a new, unused code.
3. Click 'Save Changes'.
4. Go to the Classes page and look at that class.
5. Log in as the trainer and open 'Input Grades' for that class.""",
    """The rename should succeed, and a note on the form should warn that renaming updates every class and grade linked to the subject.
The class should now show the new subject code.
The grades that were already entered should still be there, attached to the renamed subject - nothing should be lost or left pointing at a code that no longer exists.""")

add("Renaming a subject to a code that already exists is refused",
    """You are logged in as registrar.
At least two subjects exist.

Setup steps:
Dependency: note the subject codes of two different existing subjects.
1. Log in as registrar.
2. From the top menu, click 'Subjects'.""",
    """1. Click 'Edit' on the first subject.
2. Change its Subject Code to the code of the second subject.
3. Click 'Save Changes'.""",
    """The system should refuse to save.
A message should say that the subject code already exists.
Both subjects should be left exactly as they were.""")

add("Trainers are shown per subject from the classes, not set on the subject itself",
    """You are logged in as registrar.
A subject is taught by at least one class that has a trainer assigned.

Setup steps:
Dependency: create a class for a subject and assign a trainer to that class.
1. Log in as registrar.
2. From the top menu, click 'Subjects'.""",
    """1. Look at the 'Trainer(s)' column for that subject.
2. Click 'Edit' on the subject and look at the fields on the form.""",
    """The 'Trainer(s)' column should list the trainers of the classes that teach the subject.
The Edit form should have NO field for setting a trainer directly on the subject - trainers are now assigned on the class, which is the only place that matters.
This replaces the older behaviour of assigning a default trainer to a subject.""")

# =====================================================================
# TRAINER ACCOUNT LIFECYCLE
# =====================================================================

add("Permanently deleting a trainer who has locked grades is blocked",
    """You are logged in as admin.
A trainer owns at least one class whose grades have been locked.

Setup steps:
Dependency: have the trainer enter grades for a class and click 'Lock Grades' first.
1. Log in as admin.
2. On the Admin home page, open that trainer's account details.""",
    """1. Click 'Delete'.
2. Choose 'Permanently Delete'.
3. Type 'delete' and confirm.""",
    """The system should refuse to delete the account.
The message should say how many of the trainer's classes have locked grades and suggest deactivating the account instead, or reassigning those classes first.
The trainer account should still be there and nothing should be recorded in the activity logs for the blocked attempt.""")

add("Deleting a trainer without locked grades reports the classes it freed",
    """You are logged in as admin.
A test trainer account owns one or more classes, none of which have locked grades.

Setup steps:
Dependency: create a throwaway trainer account and assign it to a class with unlocked grades, so no real data is lost.
1. Log in as admin.
2. On the Admin home page, open that trainer's account details.""",
    """1. Click 'Delete', choose 'Permanently Delete', type 'delete' and confirm.
2. Read the message that appears.
3. Log in as registrar, open the Classes page and find those classes.""",
    """The account should be deleted.
The message should tell you how many classes were left without a trainer as a result.
Those classes should still exist on the Classes page but with no trainer shown, ready to be reassigned.
The activity log entry should mention how many classes were unassigned.""")

add("Deactivating a trainer warns that classes still name them",
    """You are logged in as admin.
A trainer account owns at least one class.

Setup steps:
Dependency: a trainer must be assigned to at least one class.
1. Log in as admin.
2. On the Admin home page, open that trainer's account details.""",
    """1. Click 'Delete' and choose 'Deactivate Account'.
2. Confirm.
3. Read the message that appears.""",
    """The account should be deactivated successfully - this is reversible, so it is not blocked.
A warning banner should appear saying the trainer is still the trainer of record on a stated number of classes.
The activity log entry should mention the same thing.""")

add("A deactivated trainer still shows on the class's Edit Trainer list",
    """You are logged in as registrar.
A class is assigned to a trainer whose account has been deactivated.

Setup steps:
Dependency: deactivate a trainer who owns a class (see the previous test).
1. Log in as registrar.
2. From the top menu, click 'Classes'.""",
    """1. Click 'Edit Trainer' on that class.
2. Look at the dropdown and at which trainer is currently selected.
3. Close the window, click 'Create Class' and look at its trainer dropdown.""",
    """On the Edit Trainer window, the deactivated trainer should still be listed and still shown as the current choice, with '(deactivated)' beside their name - so the class does not look as though it has no trainer.
On the Create Class window, only active trainers should be offered, since a new class should not be given to a deactivated account.""")

# =====================================================================
# ACCESS AND NAVIGATION FOR THE NEWER PAGES
# =====================================================================

add("The registrar menu shows all six pages on every registrar page",
    """You are logged in as registrar.

Setup steps:
1. Log in as registrar.""",
    """1. On the Registrar home page, read the links in the top menu.
2. Click each link in turn: Subjects, Classes, Sections, Documents, Student Numbers.
3. On each page that opens, check the top menu again.
4. Open a student's Edit form from the Registrar home page and check the menu there too.
5. Make the browser window narrow and open the collapsed menu.""",
    """The top menu should show six links on every registrar page: Home, Subjects, Classes, Sections, Documents and Student Numbers.
Every link should open the right page.
No page should be left with an older, shorter menu.
On a narrow window the menu should collapse into a button and still list all six links.""")

add("Trainers and signed-out visitors cannot open the registrar's newer pages",
    """You have the trainer account details.
The system is running.

Setup steps:
1. Log in as the trainer account.""",
    """1. While logged in as the trainer, type each of these addresses into the browser one at a time: /documents.html, /generate-document.html, /student-numbers.html
2. Log out completely.
3. Try the same three addresses again while logged out.""",
    """As a trainer, each address should send you away from the page (back to your own dashboard or to the login page) - none of the three should open.
While logged out, all three should send you to the login page.
No student information should ever be visible on the screen during these attempts.""")

add("The newer pages survive a page refresh while logged in",
    """You are logged in as registrar.

Setup steps:
1. Log in as registrar.""",
    """1. Open the Student Numbers page and press F5 to refresh.
2. Open the Subjects page and press F5 to refresh.
3. Leave the Student Numbers page open and untouched for longer than the 30-minute session limit, then press F5 again.""",
    """After each refresh, the page should reload with its table and filters working and you should still be logged in.
After the session has timed out, refreshing should send you back to the login page rather than showing a broken page or an error.""")


# =====================================================================
# WORKBOOK BUILD - styling mirrors "OLD ANIHAN Thread Testing Cases.xlsx"
# =====================================================================

START_NUMBER = 83  # continues from TC-082 in the old sheet

HEADER_FONT = Font(name="Arial", bold=True, color="FFFFFF", size=11)
HEADER_FILL = PatternFill("solid", start_color="FF2E5C8A", end_color="FF2E5C8A")
HEADER_ALIGN = Alignment(wrap_text=True, vertical="center", horizontal="center")

THIN = Side(style="thin", color="999999")
BORDER = Border(top=THIN, left=THIN, right=THIN, bottom=THIN)
WRAP = Alignment(wrap_text=True, vertical="top", horizontal="left")

BODY_FILL = PatternFill("solid", start_color="FFF4F7FB", end_color="FFF4F7FB")
INPUT_FILL_EVEN = PatternFill("solid", start_color="FFFFFBEB", end_color="FFFFFBEB")
INPUT_FILL_ODD = PatternFill("solid", start_color="FFFFF4D6", end_color="FFFFF4D6")

ID_FONT = Font(name="Arial", bold=True, size=11)
WIDTHS = {"A": 8.75, "B": 39.38, "C": 42.0, "D": 48.13,
          "E": 52.5, "F": 15.75, "G": 12.25, "H": 12.5, "I": 43.88}


def build_sheet(ws):
    ws.append(HEADERS)
    for cell in ws[1]:
        cell.font = HEADER_FONT
        cell.fill = HEADER_FILL
        cell.alignment = HEADER_ALIGN
        cell.border = BORDER

    for i, (title, pre, steps, expected) in enumerate(CASES):
        ws.append(["TC-%03d" % (START_NUMBER + i), title, pre, steps, expected])

    last_row = ws.max_row
    for row in range(2, last_row + 1):
        even = (row % 2 == 0)
        for col in range(1, 10):
            cell = ws.cell(row=row, column=col)
            cell.alignment = WRAP
            cell.border = BORDER
            if col <= 5:
                if even:
                    cell.fill = BODY_FILL
            else:
                cell.fill = INPUT_FILL_EVEN if even else INPUT_FILL_ODD
        ws.cell(row=row, column=1).font = ID_FONT
        ws.cell(row=row, column=7).number_format = "yyyy-mm-dd"

    for col, width in WIDTHS.items():
        ws.column_dimensions[col].width = width

    ws.freeze_panes = "A2"

    dv = DataValidation(type="list", formula1='"Pass,Fail,Blocked,Not Tested"',
                        allow_blank=True, showDropDown=False)
    ws.add_data_validation(dv)
    dv.add("H2:H%d" % last_row)
    return last_row


wb = openpyxl.Workbook()
ws1 = wb.active
ws1.title = "Thread Testing 2"
rows = build_sheet(ws1)

ws2 = wb.create_sheet("Template")
build_sheet(ws2)

out = r"C:\Users\Sean\Documents\AnihanProject\capstonepaper\NEW ANIHAN Thread Testing Cases.xlsx"
wb.save(out)
print("Wrote", out)
print("Cases:", len(CASES), "-> TC-%03d .. TC-%03d" % (START_NUMBER, START_NUMBER + len(CASES) - 1))
