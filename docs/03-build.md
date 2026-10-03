# Build Log

## Goal for this step
Implement search and tag filtering on `/bookmarks` while preserving the existing add/save flow, newest-first ordering, and shared red validation styling.

## Environment check
- `java -version` failed because `java` was not installed or not on `PATH`.
- `javac -version` failed because `javac` was not installed or not on `PATH`.
- `.\gradlew.bat -version` failed because `JAVA_HOME` was not set to a valid JDK and no `java` command was available.

## Dependency check
- No new Flyway/H2 helper dependency was added.
- The existing Flyway Core setup was kept as-is, in line with the instruction not to assume a separate `flyway-database-h2` helper.

## Chosen versions
- Spring Boot **3.4.1**
- Gradle Wrapper **8.10.2**
- Java toolchain target **21**

These were selected because they are stable, widely supported, and compatible with Spring Boot MVC, Thymeleaf, Spring Data JPA, Flyway, embedded H2, and Hibernate schema validation.

## Implementation work completed
1. Corrected the application database path to `./data/bookmarks` in `src/main/resources/application.properties`.
2. Kept tests isolated on the existing `./build/bookmarks-test` file-backed H2 database.
3. Added bookmark persistence entities:
   - `Bookmark`
   - `BookmarkTag`
4. Added `BookmarkRepository` with exact trimmed-URL duplicate lookup and a tag-fetching query for verification.
5. Added `BookmarkService` to:
   - trim URL/title/tags
   - validate HTTP(S) URLs with a dotted hostname made of non-empty labels
   - enforce the 2048/200 post-trim length limits
   - normalize tags to lowercase
   - remove empty and duplicate tags
   - reject duplicate URLs before save
   - catch database uniqueness failures and return the same friendly duplicate message
   - generate the creation timestamp on the backend
6. Added the add-bookmark form flow in `BookmarkPageController` and a Thymeleaf form template.
7. Updated the landing page to link to the add form and show success messages after redirects.
8. Added a new Flyway migration for the bookmark and tag schema, leaving the existing migration untouched.
9. Added automated tests for service rules, controller flows, and restart-style file-backed persistence.
10. Fixed the migration history issue by restoring the original empty `V2` migration and moving the bookmark schema into `V3`.
11. Replaced the `/bookmarks` placeholder page with a bookmark list view that shows title, URL, and normalized tags, including the empty-state message when no bookmarks exist.
12. Added a shared form error style and inline alert message so application-rendered validation errors are red, accessible, and consistent across the save form.
13. Added a repository/service list path that sorts bookmarks by `created_at` descending with `id` descending as a deterministic tie-breaker.
14. Tightened URL validation so the app requires a dotted hostname with non-empty labels, rejects single-label hosts and IP addresses, and shows the clarified inline validation message.
15. Added search text and tag filtering to the bookmark list page, including retained filter state, `All tags`, `Clear search and filters`, and the no-results empty state.
16. Added bookmark editing with prefilled forms, filtered-list state preservation, duplicate exclusion for the edited bookmark, atomic updates, and a friendly not-found response.

## Technical choices
- The application uses Spring Boot MVC rather than a separate frontend stack.
- Thymeleaf is used for the landing page and add-bookmark form.
- The application database stays under `./data/bookmarks` so it follows the approved relative path.
- The test database is separated from the application database to keep test runs isolated.
- Flyway remains enabled and Hibernate validation remains in `validate` mode.
- The bookmark/tags schema was added in a new versioned migration instead of changing the existing migration.

## Fixes and adjustments made during setup
- Replaced the placeholder setup page with the bookmark landing page and add-bookmark flow.
- Added the backend validation and normalization logic needed by the save feature.
- Added persistence tests that use a temporary file-backed H2 database and reopen the same file to verify persistence.
- Cleaned up a warning-only restart-test helper after the source checker flagged it.
- Verified the edited Java sources with the workspace source checker; it reported no errors, only a warning about the new ordered-list repository method being unused and a suggestion to use a method reference in one test.
- Simplified the bookmark list tag markup so normalized tags render as a single escaped comma-separated text node, matching the saved-list expectation while still avoiding unsafe HTML injection.

## Commands run
```powershell
java -version
javac -version
.\gradlew.bat -version
.\gradlew.bat test
.\gradlew.bat clean build
```

## Results
- `java -version` failed because `java` is not installed or not on `PATH`.
- `javac -version` failed because `javac` is not installed or not on `PATH`.
- `.\gradlew.bat -version` failed because the environment does not have a usable JDK/JAVA_HOME.
- `.\gradlew.bat test` failed for the same reason.
- `.\gradlew.bat clean build` failed for the same reason.
- Workspace source checks on the edited Java files reported no errors.
- After the JDK was configured for the current PowerShell session, `java -version`, `javac -version`, and `.\gradlew.bat -version` succeeded.
- `.\gradlew.bat clean build` initially failed because `V2__bookmark_schema.sql` had been populated after the migration was already in play, which caused Flyway validation/checksum issues.
- Restoring `V2__bookmark_schema.sql` to its original empty form and moving the schema into `V3__bookmark_schema.sql` resolved the Flyway validation failure.
- `.\gradlew.bat clean build` then succeeded.
- `.\gradlew.bat bootRun` started successfully, and `GET http://localhost:8080/bookmarks` returned `200` with the landing page and `Add bookmark` link.
- A later save-feature test run exposed one failing integration test: `com.krish.bookmarks.integration.BookmarkPersistenceIntegrationTest.persistsBookmarkAndTagsAcrossRestart(Path)`.
- Expected behavior: the restart-style test should use a temporary file-backed H2 database, save a bookmark, close the first context, reopen the same temp file, and read the saved bookmark and tags back.
- Actual behavior: the test booted against `./data/bookmarks.mv.db` instead of the temp file and failed with an H2 file-lock error because `SpringApplicationBuilder.properties(...)` only supplied default properties and did not override `application.properties`.
- Fix applied: the test now sets the temporary datasource/Flyway values through JVM system properties for the duration of the test and restores the previous values in a `finally` block.
- Rerun status: the shell execution tool in this workspace did not produce side effects when invoked here, so the post-fix `test`/`build` rerun could not be completed in this session and still needs local verification.
- For the current bookmark-list step, the terminal execution tool again returned only the interactive `>>` prompt and did not produce usable build output, so the new `test`/`build` rerun could not be completed from this workspace session.
- The bookmark-list HTML assertion failure was traced to the template’s nested tag spans; the list template was updated to render the joined tags directly, but the suite still needs a fresh rerun in this workspace to confirm the fix.
- The clarified URL-validation rule was implemented and the edited Java files were checked with the workspace source checker again; it reported no errors.
- The search/tag-filter implementation and tests were added with the workspace source checker still reporting no errors for the edited Java files.
- You reported that the latest changes now build successfully locally, the saved bookmark list works, adding a new bookmark works, and validation/error messages display correctly in red; this log records that user-reported verification without claiming it was run here.
- A fresh `gradlew.bat test` attempt in this workspace again returned only the interactive `>>` prompt and did not produce usable output.
- The edit-flow implementation was added and the edited Java files were checked again; the workspace source checker reported no errors.
- You reported that the edit feature is completed successfully; this log records that user-reported status without inventing specific test or manual-check results.
- The delete-feature implementation was added with a dedicated confirmation page, POST-confirmed removal, preserved search/tag state, and a new restart-style persistence test; the edited Java sources were checked again and reported no errors.
- A fresh Gradle build/test verification attempt in this workspace still could not be trusted because the terminal tool did not create even a small probe file, so no actual build result is being claimed here.
- A fresh `gradlew.bat clean build` attempt in this workspace again returned only the interactive `>>` prompt and did not produce usable output.

## Acceptance criteria covered by this step
- Save a bookmark with URL, title, and optional tags.
- Trim input before validation and storage.
- Reject blank required fields.
- Reject invalid HTTP(S) URLs and unsupported schemes.
- Reject over-limit URL/title values without truncation.
- Reject duplicate trimmed URLs with the friendly message.
- Normalize tags to trimmed lowercase values without duplicates.
- Persist bookmark and tags atomically.
- Generate the creation timestamp on the backend.
- Provide a link from the existing landing page to the add form.
- Show saved bookmarks newest first at `/bookmarks` with a deterministic `id` tie-breaker.
- Render the list page empty state when there are no saved bookmarks.
- Apply shared red inline validation/error styling to the save form.
- Edit a bookmark with prefilled URL, title, and tags while preserving the original ID and creation time.
- Preserve search/tag state through the edit form, save, cancel, and filtered redirects.
- Show a friendly not-found response when an edit target is missing.

## Restart-persistence investigation and fix

- The failing restart-persistence assertion compared the detached `createdAt` value returned from the service layer with a value reloaded from H2, and those two values differed by one microsecond (`2026-09-29T13:35:35.780007Z` vs `2026-09-29T13:35:35.780008Z`).
- Root cause: Java `Instant` carries nanosecond precision, but the persisted value is round-tripped through Hibernate/JDBC into the H2 `TIMESTAMP` column defined in `V3__bookmark_schema.sql`, which stores a lower-precision persisted timestamp. The test was therefore comparing an in-memory entity value to a storage-reloaded value instead of comparing two values read back from persisted storage.
- Fix applied in `src/test/java/com/krish/bookmarks/integration/BookmarkPersistenceIntegrationTest.java`:
  - after save/update, the test now reloads the bookmark from the repository in the first application context,
  - captures the persisted `createdAt` before shutdown,
  - compares that persisted value with the value read after reopening the database,
  - and keeps the edit-path assertion that `createdAt` is unchanged.
- This keeps the restart verification meaningful without adding an arbitrary time tolerance or touching the saved application database.
- Verification of the updated test and clean build is still pending in this session because the shell bridge did not produce a fresh runnable result after the test-only change.

## Manual checks still pending
- Open the app in a browser after Java is installed.
- Submit a valid bookmark and verify the redirect and success message.
- Submit invalid input and verify field-specific validation messages.
- Confirm duplicate detection from the add form.

## H2 browser console for local development

- The H2 console is configured in `src/main/resources/application.properties`.
- It stays off by default and can be enabled locally with the `H2_CONSOLE_ENABLED=true` environment variable.
- Remote access is not allowed: `spring.h2.console.settings.web-allow-others=false`.
- Start the app with the console enabled:

```powershell
$env:H2_CONSOLE_ENABLED = "true"
.\gradlew.bat bootRun
```

- Console URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:file:./data/bookmarks;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`
- Credentials:
  - Username: `sa`
  - Password: empty
  - Source: `src/main/resources/application.properties`

### Read-only SQL to inspect saved bookmarks

Use these queries in the console to inspect the saved data without changing it:

```sql
SELECT id, url, title, created_at
FROM bookmarks
ORDER BY created_at DESC, id DESC;

SELECT id, bookmark_id, tag_value
FROM bookmark_tags
ORDER BY bookmark_id ASC, tag_value ASC;

SELECT b.id,
       b.title,
       b.url,
       b.created_at,
       bt.tag_value
FROM bookmarks b
LEFT JOIN bookmark_tags bt ON bt.bookmark_id = b.id
ORDER BY b.created_at DESC, b.id DESC, bt.tag_value ASC;
```
