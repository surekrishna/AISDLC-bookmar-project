# Testing Log

## Checks actually performed

- [x] Read `docs/01-planning.md` and `docs/02-design.md`
  - Outcome: confirmed the approved MVC/Thymeleaf/Gradle/Flyway/H2 stack and the save-bookmark scope.

- [x] Checked the installed Java toolchain
  - Outcome: `java` was not recognized in the shell initially.
  - Outcome: `javac` was not recognized in the shell initially.
  - Outcome: the Gradle wrapper also failed before the JDK was configured.

- [x] Verified the repository structure before implementation
  - Outcome: the assignment repository contained the scaffolded Spring Boot app, the docs, and the empty `data/` folder.

- [x] Corrected the application H2 path
  - Outcome: the main application now points to `./data/bookmarks` as approved.

- [x] Kept tests isolated from the application database
  - Outcome: test properties still point to `./build/bookmarks-test`, and the restart-style persistence test uses a temporary file-backed database under a temp directory.

- [x] Investigated the failing restart-style persistence test
  - Outcome: `com.krish.bookmarks.integration.BookmarkPersistenceIntegrationTest.persistsBookmarkAndTagsAcrossRestart(Path)` failed because the temporary datasource settings were only applied as default properties and Spring still opened `./data/bookmarks.mv.db`.
  - Outcome: the integration test was fixed to set the temporary datasource/Flyway values through JVM system properties for the duration of the test and then restore the previous values.

- [x] Ran the workspace source checker on the edited Java files
  - Outcome: no Java source errors were reported after the bookmark feature implementation.

- [x] Implemented the bookmark save feature
  - Outcome: added the bookmark form, validation, tag cleanup, duplicate handling, atomic persistence, and new Flyway schema migration.

- [x] Extended the bookmark list and validation styling coverage
  - Outcome: added tests for the empty list state, saved bookmark rendering, bookmarks without tags, newest-first ordering with a tie-breaker, escaped rendering of user-entered content, the shared form error CSS class, and the existing save flow.

- [x] Investigated the saved-list HTML assertion mismatch
  - Outcome: `BookmarkPageControllerTest.showsSavedBookmarksWithTitleUrlAndTags()` expected a readable `java, spring` string in the rendered HTML, but the template emitted nested spans around each tag.
  - Outcome: the bookmark list template was updated to render the normalized tags as one escaped comma-separated text node so the readable HTML source matches the list presentation.

- [x] Verified the JDK installation in the current PowerShell session only
  - Outcome: `Temurin JDK 21.0.10 location` contains `bin\java.exe` and `bin\javac.exe`.
  - Outcome: `JAVA_HOME` and `PATH` were configured only for the current shell session.

- [x] Re-checked the edited Java sources after the URL-validation change
  - Outcome: the workspace source checker reported no errors for `BookmarkService.java`, `BookmarkServiceTest.java`, and `BookmarkPageControllerTest.java`.

- [x] Recorded your local verification for the search/tag-filter step
  - Outcome: you reported that the latest changes build successfully locally, the saved bookmark list works, adding a new bookmark works, and validation/error messages display correctly in red.
  - Note: this log records your reported results and does not claim I ran those checks in this workspace.

- [x] Recorded your latest local verification for the search/tag-filter step
  - Outcome: you reported that the build completed successfully locally and that search/tag filtering works.
  - Note: this log records your reported results and does not claim I ran those checks in this workspace.

- [x] Recorded your reported edit status
  - Outcome: you reported that the edit feature is completed successfully.
  - Note: this log records your reported status only and does not invent test or manual-check details.

- [x] Recorded your reported deletion completion and local build pass
  - Outcome: you reported that deletion is completed and that the build passed locally.
  - Note: this log records your reported results and does not claim I re-ran those checks here.

- [x] Recorded your reported successful clean build after the persistence-test fix
  - Outcome: you reported that `gradlew.bat clean build` succeeded locally after the restart-persistence test fix.
  - Note: this log records your reported result and does not claim I re-ran the build here.

## Automated checks attempted

- [x] `java -version`
  - Outcome: succeeded against `Temurin JDK 21.0.10 location \bin\java.exe`.

- [x] `javac -version`
  - Outcome: succeeded against `Temurin JDK 21.0.10 location bin\javac.exe`.

- [x] `gradlew.bat clean build`
  - Outcome: ran from the assignment root with `JAVA_HOME` pointed at `Temurin JDK 21.0.10 location`.
  - Outcome: `com.krish.bookmarks.service.BookmarkServiceTest` passed.
  - Outcome: `com.krish.bookmarks.web.BookmarkPageControllerTest` passed.
  - Outcome: `com.krish.bookmarks.integration.BookmarkPersistenceIntegrationTest` failed in `persistsBookmarkAndTagsAcrossRestart(Path)` at `BookmarkPersistenceIntegrationTest.java:72` because the expected and actual `createdAt` values differed by one microsecond (`2026-09-29T13:35:35.780007Z` expected vs `2026-09-29T13:35:35.780008Z` actual).
  - Outcome: the build ended with `> Task :test FAILED`.

## User-reported verification after the persistence-test fix

- [x] `gradlew.bat clean build`
  - Outcome: you reported that the clean build succeeded locally after the persistence-test fix.
  - Note: this entry records your report and does not claim I re-ran the build in this workspace.

## Final user-reported manual verification results

- [x] Add bookmark
  - Outcome: adding a new bookmark works.

- [x] List bookmarks
  - Outcome: the saved bookmark list works.

- [x] Search and tag filtering
  - Outcome: search and tag filtering work.

- [x] Edit bookmark
  - Outcome: the edit feature is completed successfully.

- [x] Delete bookmark
  - Outcome: deletion is completed successfully.

- [x] Invalid input feedback
  - Outcome: invalid-input or validation error messages display correctly in red.

- [x] Restart persistence
  - Outcome: bookmark data persists after restarting the application.
  - Note: this records your reported result and preserves the earlier failure/fix history above.

- [x] Clean build after the persistence-test fix
  - Outcome: `gradlew.bat clean build` succeeded locally after the restart-persistence test fix.
  - Note: this records your reported result and does not claim I reran the build here.

## Browser verification

- [ ] Opened the app in a browser
  - Pending; no browser checks were described in the latest user report.

- [ ] Confirmed the landing page, add form, edit form, delete confirmation page, search/filter state, and the clear action in a browser
  - Pending; no browser checks were described in the latest user report.

## Acceptance criteria coverage matrix

| AC | Implementation | Automated test coverage | Verification status | Manual check still needed |
| --- | --- | --- | --- | --- |
| AC-01 | `BookmarkService.save`, `BookmarkPageController.saveBookmark` | `BookmarkServiceTest.savesBookmarkWithAcceptedDottedHttpUrls`, `BookmarkPageControllerTest.savesValidBookmarkWithoutTags`, `BookmarkPageControllerTest.savesValidBookmarkWithCleanedTags` | Covered by passing service/controller tests in the workspace run | Browser smoke of valid save flow |
| AC-02 | `BookmarkService.save` with optional tags | `BookmarkServiceTest.savesBookmarkWithoutTags`, `BookmarkPageControllerTest.savesValidBookmarkWithoutTags` | Covered by passing tests | Browser smoke of save-without-tags |
| AC-03 | `BookmarkService.validate`, form error rendering | `BookmarkServiceTest.rejectsBlankRequiredFieldsWithoutCallingRepository`, `BookmarkPageControllerTest.rejectsBlankRequiredFieldsAndKeepsFormInput` | Covered by passing tests | Browser smoke of empty title handling |
| AC-04 | `BookmarkService.isValidHttpUrl` | `BookmarkServiceTest.rejectsUrlsThatAreNotDottedHttpWebsites`, `BookmarkPageControllerTest.rejectsMissingSchemeAndUnsupportedScheme` | Covered by passing tests | Browser smoke of invalid URL messages |
| AC-05 | `trimToNull` in service, trim helpers in controller | `BookmarkServiceTest.savesBookmarkWithAcceptedDottedHttpUrls`, `BookmarkServiceTest.rejectsDuplicateTrimmedUrl`, `BookmarkPageControllerTest.savesValidBookmarkWithoutTags` | Covered by passing tests | Browser smoke of whitespace trimming |
| AC-06 | `BookmarkService.normalizeTags` | `BookmarkServiceTest.savesBookmarkWithNormalizedTagsAndDeduplicatesThem`, `BookmarkPageControllerTest.savesValidBookmarkWithCleanedTags` | Covered by passing tests | Browser smoke of comma-separated tags |
| AC-07 | `BookmarkService.normalizeTags` | Same as AC-06 | Covered by passing tests | Browser smoke of lowercase/deduped tags |
| AC-36 | `BookmarkService.validate`, `Bookmark` column lengths | `BookmarkServiceTest.acceptsMaximumLengthUrlAndTitleAtTrimmedBoundary` | Covered by passing tests | Manual spot-check not required unless the UI is changed |
| AC-37 | `BookmarkService.validate`, `Bookmark` column lengths | `BookmarkServiceTest.rejectsOverLimitUrlAndTitleWithoutTruncating` | Covered by passing tests | Browser smoke of over-limit rejection |
| AC-08 | `BookmarkService.validate`, `BookmarkRepository.existsByUrl` | `BookmarkServiceTest.rejectsDuplicateTrimmedUrl`, `BookmarkPageControllerTest.rejectsDuplicateTrimmedUrlWithFriendlyMessage` | Covered by passing tests | Browser smoke of duplicate save rejection |
| AC-09 | Duplicate-message path in service/controller | Same as AC-08 | Covered by passing tests | Same as AC-08 |
| AC-10 | `BookmarkService.update` excludes the current ID | `BookmarkServiceTest.allowsKeepingCurrentUrlWhileEditingButRejectsAnotherBookmarksUrl`, `BookmarkPageControllerTest.updatesAllEditableFieldsAndPreservesCreationTimeAndOrdering` | Covered by passing tests | Browser smoke of keeping the same URL while editing |
| AC-11 | `BookmarkService.update` duplicate handling | Same as AC-10 plus `BookmarkPageControllerTest.rejectsUpdatingToAnotherBookmarksTrimmedUrlAndLeavesDataUnchanged` | Covered by passing tests | Browser smoke of duplicate-on-edit rejection |
| AC-12 | `BookmarkRepository.findAllByOrderByCreatedAtDescIdDesc`, `BookmarkService.listBookmarks` | `BookmarkServiceTest.listsBookmarksNewestFirst`, `BookmarkPageControllerTest.ordersBookmarksNewestFirstAndUsesIdForTies` | Covered by passing tests | Browser smoke of newest-first ordering |
| AC-13 | `BookmarkService.update` preserves `createdAt` | `BookmarkServiceTest.updatesBookmarkPreservingIdAndCreationTimeAndReplacingAllFields`, `BookmarkPageControllerTest.updatesAllEditableFieldsAndPreservesCreationTimeAndOrdering` | Covered by passing tests | Browser smoke of edit order staying stable |
| AC-14 | Restart persistence flow in `BookmarkPersistenceIntegrationTest` | `BookmarkPersistenceIntegrationTest.persistsBookmarkAndTagsAcrossRestart(Path)` | Not fully verified in this workspace: the test failed on the `createdAt` comparison before the restart assertions completed | Re-run after addressing the timestamp-precision assertion |
| AC-15 | Delete persistence flow in `BookmarkPersistenceIntegrationTest` | `BookmarkPersistenceIntegrationTest.deletedBookmarkRemainsDeletedAcrossRestart(Path)` | Verified by a passing integration test in the workspace run | Browser smoke of delete-after-restart behaviour |
| AC-16 | File-backed persistence after restart | `BookmarkPersistenceIntegrationTest.persistsBookmarkAndTagsAcrossRestart(Path)` | Not fully verified in this workspace for the same reason as AC-14 | Re-run after addressing the timestamp-precision assertion |
| AC-17 | `BookmarkService.listBookmarks(searchText, selectedTag)` | `BookmarkServiceTest.filtersBookmarksByPartialCaseInsensitiveSearchAcrossTitleAndUrl`, `BookmarkPageControllerTest.filtersBookmarksBySearchAndTagAndRetainsTheSubmittedControls` | Covered by passing tests | Browser smoke of search |
| AC-18 | `normalizeSearchText`, controller search param trimming | `BookmarkServiceTest.trimsBlankSearchAndLeavesResultsUnrestricted` | Covered by passing tests | Browser smoke of blank search |
| AC-19 | Combined filtering in service/controller | `BookmarkServiceTest.combinesSearchTextAndTagFilters`, `BookmarkPageControllerTest.filtersBookmarksBySearchAndTagAndRetainsTheSubmittedControls` | Covered by passing tests | Browser smoke of combined search + tag |
| AC-20 | Combined filtering keeps newest-first order | Same as AC-19 plus `BookmarkPageControllerTest.ordersBookmarksNewestFirstAndUsesIdForTies` | Covered by passing tests | Browser smoke of combined results ordering |
| AC-21 | `BookmarkService.matchesTag` exact equality | `BookmarkServiceTest.filtersBookmarksByExactSelectedTag`, `BookmarkPageControllerTest.showsAlphabeticalUniqueTagOptionsAcrossAllSavedBookmarks` | Covered by passing tests | Browser smoke of exact tag filtering |
| AC-22 | Same exact-tag path | Same as AC-21 | Covered by passing tests | Same as AC-21 |
| AC-23 | `BookmarkPageController.index` `All tags` option | `BookmarkPageControllerTest.filtersBookmarksBySearchAndTagAndRetainsTheSubmittedControls`, `BookmarkPageControllerTest.showsNoMatchingMessageAndClearLinkWhenFiltersReturnNoResults` | Covered by passing tests | Browser smoke of clearing tag filter |
| AC-24 | Redirects and query-state preservation after edit | `BookmarkPageControllerTest.showsPrefilledEditFormAndPreservesSearchStateInLinks`, `BookmarkPageControllerTest.updatesAllEditableFieldsAndPreservesCreationTimeAndOrdering` | Covered by passing tests | Browser smoke of post-edit filter refresh |
| AC-25 | Active filter preservation when edited bookmark no longer matches | `BookmarkPageControllerTest.removesAllOptionalTagsWhenEditingAndPreservesVisibleFilterStateOnCancel` | Covered by passing tests | Browser smoke of filtered results disappearing after edit |
| AC-26 | `BookmarkPageController.editBookmark`, `BookmarkService.update` | `BookmarkPageControllerTest.showsPrefilledEditFormAndPreservesSearchStateInLinks`, `BookmarkPageControllerTest.updatesAllEditableFieldsAndPreservesCreationTimeAndOrdering` | Covered by passing tests | Browser smoke of full edit flow |
| AC-27 | Validation failure path in controller/service | `BookmarkServiceTest.rejectsInvalidEditedDataAndLeavesStoredBookmarkUnchanged`, `BookmarkPageControllerTest.rejectsInvalidEditAndKeepsStoredDataUnchanged` | Covered by passing tests | Browser smoke of failed edit preserving data |
| AC-28 | Cancel links in edit flow | `BookmarkPageControllerTest.showsPrefilledEditFormAndPreservesSearchStateInLinks`, `BookmarkPageControllerTest.removesAllOptionalTagsWhenEditingAndPreservesVisibleFilterStateOnCancel` | Covered by passing tests | Browser smoke of edit cancel |
| AC-29 | Delete confirmation page and POST delete endpoint | `BookmarkPageControllerTest.showsDeleteConfirmationPageWithEscapedBookmarkDetailsAndPreservedState`, `BookmarkPageControllerTest.confirmsDeleteRemovesBookmarkAndItsTagsWhileLeavingOtherBookmarksUntouched` | Covered by passing tests | Browser smoke of delete confirmation |
| AC-30 | Delete cancel link and GET confirmation page | `BookmarkPageControllerTest.cancelingDeleteLeavesStoredDataUnchangedAndKeepsTheCancelLinkSafe` | Covered by passing tests | Browser smoke of delete cancel |
| AC-31 | `BookmarkService.delete`, `BookmarkPageController.confirmDeleteBookmark` | `BookmarkPageControllerTest.confirmsDeleteRemovesBookmarkAndItsTagsWhileLeavingOtherBookmarksUntouched`, `BookmarkPersistenceIntegrationTest.deletedBookmarkRemainsDeletedAcrossRestart(Path)` | Covered by passing tests | Browser smoke of delete persistence |
| AC-32 | `index.html` empty-state branch | `BookmarkPageControllerTest.showsEmptyListMessageWhenNoBookmarksExist`, `BookmarkPageControllerTest.showsEmptyStateAfterDeletingTheLastBookmark` | Covered by passing tests | Browser smoke of empty list state |
| AC-33 | `index.html` no-results branch | `BookmarkPageControllerTest.showsNoMatchingMessageAndClearLinkWhenFiltersReturnNoResults`, `BookmarkPageControllerTest.removesAllOptionalTagsWhenEditingAndPreservesVisibleFilterStateOnCancel` | Covered by passing tests | Browser smoke of no-match state |
| AC-34 | `index.html` clear action | `BookmarkPageControllerTest.showsNoMatchingMessageAndClearLinkWhenFiltersReturnNoResults`, `BookmarkPageControllerTest.filtersBookmarksBySearchAndTagAndRetainsTheSubmittedControls` | Covered by passing tests | Browser smoke of the clear action |
| AC-35 | No sample-data insertion in app startup | Application startup and page tests do not seed bookmarks; `BookmarkPageControllerTest.showsEmptyListMessageWhenNoBookmarksExist` checks the empty state | Covered by the current codebase and tests; no auto-seed path was found | Manual startup check if desired |

## Remaining manual acceptance checklist

- Open the app in a browser and smoke-test add, edit, search, filter, and delete.
- Confirm the delete confirmation page uses POST for the destructive action.
- Re-run the restart persistence scenario after addressing the timestamp-precision test failure.

## Post-fix verification attempt

- [ ] `gradlew.bat test --tests com.krish.bookmarks.integration.BookmarkPersistenceIntegrationTest`
  - Blocked in this workspace session: after the test-only fix, the shell bridge did not produce a usable rerun result or output file, so I could not capture a fresh pass/fail result for the corrected test.

- [ ] `gradlew.bat clean build`
  - Blocked in this workspace session for the same reason, so I could not capture a fresh clean-build result after the fix.

