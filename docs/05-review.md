# Review Notes

## Review scope

I reviewed the approved planning and design docs, the current application code, the automated tests, and the latest workspace build/test output.

I focused on the approved bookmark flows: validation, duplicate handling, tag cleanup, search/filtering, edit/delete behaviour, result-state preservation, safe rendering, persistence, Flyway migration handling, and Git hygiene.

## Findings

### Medium — restart-persistence integration test was flaky on timestamp precision
- **File:** `src/test/java/com/krish/bookmarks/integration/BookmarkPersistenceIntegrationTest.java:72`
- **Related build evidence:** `build/test-results/test/TEST-com.krish.bookmarks.integration.BookmarkPersistenceIntegrationTest.xml`
- **Why it mattered:** the workspace clean build failed on this assertion, so automated verification was not fully green at that point. The test was trying to prove restart persistence, but it compared `createdAt` at microsecond precision and failed by one microsecond in this environment.
- **Suggested fix:** compare the persisted timestamp at a consistent precision, or assert the restart behaviour without depending on microsecond equality. If the storage layer can round timestamps, the test should normalize before comparing.
- **Status:** resolved. The test-only fix was applied in `src/test/java/com/krish/bookmarks/integration/BookmarkPersistenceIntegrationTest.java`, and you reported a successful local `gradlew.bat clean build` after the fix, which includes the test run and was not skipped or excluded.

## Review summary

I did not find additional application-code problems in the approved areas I reviewed:
- URL/title trimming and length limits look aligned with the planning/design rules.
- Dotted-host HTTP(S) validation matches the approved rule set.
- Duplicate detection uses the exact trimmed URL and excludes the edited bookmark.
- Tag cleanup, search, exact tag filtering, and filtered result refresh after edit are implemented and covered by tests.
- Delete confirmation is separate from the POST delete action.
- User-entered content is rendered with Thymeleaf escaping in the templates.
- The Flyway migrations and the Git ignore rules for local database files are present and look consistent with the plan.
- The restart-persistence test has been updated to compare reloaded persisted timestamps before shutdown and after reopening.
- You reported successful final verification of restart persistence, together with the other manual flows: add, list, search, filter, edit, delete, invalid-input handling, and a clean build after the fix.

## Remaining limitations

- Browser/manual smoke verification remains pending for any checks not covered in your final report.
- The workspace clean build was verified by your report, and the blocking finding is now resolved based on your reported successful build and final manual verification.
