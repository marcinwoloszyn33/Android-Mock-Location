# SDD ledger — plan: docs/superpowers/plans/2026-09-22-follow-real-movement.md

Setup: local container cannot resolve github.com; implementation is staged locally and RED→GREEN/build verification executes in GitHub Actions against the real repository.

Pre-flight: Task 1 produces GeoMover/HeadingFilter/StepDetector consumed by Task 2 — signatures aligned.
Pre-flight: Task 2 produces current movement metadata consumed by Task 3 MockLocationFix — aligned.
Pre-flight: Task 3 boolean provider injection result is consumed by Task 4 watchdog — aligned.
Pre-flight: Task 5 session snapshot consumes manager current point/mode and service START_STICKY restore — aligned.
Pre-flight: Tasks 6–7 consume provider/settings state without changing injection interfaces — aligned.

Ruling: use new EnhancedPrefs backed by the existing SharedPrefs file rather than expanding SharedPrefsState/diff bits — minimizes risk to the existing backup/preferences schema; cost if wrong: enhanced settings are not included in the old app's explicit backup/export format.
Ruling: persist TRIP as a session mode and current fake point, but START_STICKY recovery resumes that point as fixed rather than reconstructing the unfinished route — remaining trip destination/time are not part of the approved implementation-plan snapshot interface; cost if wrong: a process death during Trip mode stops route progression even though spoofing itself resumes.
Ruling: GitHub Actions performs one final implementation commit after all RED→GREEN tests and both builds pass rather than one commit per task — necessary for the one-upload/no-manual-edit workflow; cost if wrong: git history is less granular for bisect/review.
