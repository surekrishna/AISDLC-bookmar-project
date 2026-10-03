# Provisional Planning

## Project objective
Build a locally runnable Personal Bookmark Manager that lets a user save, find, edit, filter, and delete bookmarks while keeping the data after restart.

The application must support:
- saving a bookmark with a URL, title, and optional tags
- listing bookmarks with newest first
- searching by title or URL
- filtering by one tag at a time
- editing bookmarks
- deleting bookmarks with confirmation
- persistent storage across restarts
- validation for invalid URLs, duplicate URLs, empty titles, and empty states
- clear empty-state messaging
- no automatic sample data insertion

## Scope
### Confirmed in scope
- Add bookmark
- View bookmarks
- Search bookmarks by title or URL
- Filter bookmarks by tag
- Edit bookmark fields
- Delete bookmark with confirmation
- Persist bookmarks after restart
- Validate URL and title input
- Normalize tags according to the agreed rules
- Show empty states and no-results states
- Refresh displayed search/filter results after editing

### Proposed scope boundaries
The following are proposed boundaries for this phase and were not explicitly approved as part of the requirements:
- No cloud sync
- No multi-user support
- No social sharing
- No browser extension
- No secrets management work unless future implementation introduces secrets
- No technology stack selection in this planning document
- No application code in this phase

## Confirmed requirements and behaviour rules

### Bookmark fields
- Each bookmark has:
  - URL
  - title
  - optional tags
  - creation time used for ordering

### URL rules
- URL is required
- Trim surrounding spaces before validation and saving
- Accept only complete `http` or `https` URLs with a syntactically valid dotted hostname made of non-empty labels
- Reject empty or whitespace-only URLs
- Reject missing schemes, malformed URLs, single-label hosts such as `http://chatgpt`, `localhost`, IP-address URLs, and non-HTTP(S) schemes
- Do not guess or auto-add a scheme
- Do not check whether the website is reachable
- Show a helpful message such as: `Enter a valid website URL starting with http:// or https:// and including a domain such as example.com.`

### Title rules
- Title is required
- Trim surrounding spaces before saving
- Reject a title that is empty after trimming
- Show: `Title is required.`
- Do not use the URL as a fallback title

### Duplicate rules
- Duplicate detection is based on the same trimmed URL only
- A different title must not allow the same URL to be saved again
- Do not normalize duplicates beyond trimming:
  - do not merge `http` and `https`
  - do not normalize path case
  - do not remove trailing slashes
- Show: `This bookmark already exists.`

### Tag rules
- Tags are optional
- Tags are entered as comma-separated values
- Trim surrounding spaces from each tag
- Ignore empty tag entries
- Convert tags to lowercase
- Remove duplicate tags
- Example: ` Java, spring, JAVA, ` becomes `java`, `spring`

### Search rules
- Search both title and URL
- Ignore case
- Allow partial matches
- Match when either title or URL contains the search text
- Trim surrounding spaces from search text
- An empty search applies no text restriction
- Search and tag filter must work together, and both conditions must be satisfied when both are active
- Results are ordered newest first based on creation time

### Tag filter rules
- Users select one tag from tags already attached to saved bookmarks
- Match the selected tag exactly using normalized lowercase tags
- `java` must not match `javascript`
- Include an `All tags` option that removes tag restriction
- Keep combined search/filter behaviour and newest-first ordering

### Edit rules
- Editable fields: URL, title, and tags
- Apply the same validation and tag-cleanup rules as when creating
- URL and title remain mandatory
- Tags remain optional
- Cancelling an edit must leave the saved bookmark unchanged
- A failed validation must not change the saved data
- Duplicate checking during editing must exclude the bookmark being edited
- Keeping the same URL when editing is allowed
- Reject an edited URL already used by another bookmark with `This bookmark already exists.`
- Editing preserves the original creation time
- Editing must not move the bookmark to the top
- Displayed search/filter results must refresh after editing
- If the edited bookmark no longer matches the active search or tag filter, it must disappear from the filtered results

### Delete rules
- Ask for confirmation before deleting
- Cancel must leave the bookmark unchanged
- Confirm must remove the bookmark from the list and persistent storage
- Deleted bookmarks must remain deleted after restarting the application

### Persistence rules
- Bookmarks must remain available after application restart
- Saved edits must remain after application restart
- Deleted bookmarks must remain deleted after application restart

### Empty-state rules
- If no bookmarks have been saved, show: `No bookmarks yet. Add your first bookmark.`
- If search or filtering returns no matches, show: `No matching bookmarks found.`
- Provide a `Clear search and filters` action
- Do not automatically insert sample data

## Planning interaction record

### First planning pass
- **Your prompt:** You asked to plan a locally runnable Personal Bookmark Manager, explain requirements in simple language, ask clarification questions in small batches, and suggest acceptance criteria and edge cases without choosing a technology stack or writing code.
- **My response:** I explained the requirements in plain language and listed initial acceptance criteria and edge cases, but I did not ask clarification questions in that response.
- **Your follow-up prompt:** You prompted me again to identify assumptions and ask questions in small batches.
- **My response after that:** I then asked the first batch of clarification questions about storage, duplicate rules, tags, search behaviour, empty states, and sample data.
- **Your response:** You specified the duplicate rule, title rule, and URL validation rule.
- **Accepted:** Same-URL-only duplicate checking with trimmed comparison; title is mandatory; only complete HTTP or HTTPS URLs with a dotted hostname made of non-empty labels are accepted.
- **Modified/rejected:** No title fallback from URL; no auto-guessing of schemes; no URL reachability check; no duplicate normalization beyond trimming.

### Second planning pass
- **Your prompt:** You specified tags as free-form with cleanup, search across title and URL with case-insensitive partial matches, and empty-state messages with no automatic sample data.
- **My response:** I recorded those decisions and asked the next batch of unresolved questions about tag filtering and a few remaining gaps.
- **Your response:** You decided tag filtering should select one existing tag, match exactly using normalized lowercase tags, and include an `All tags` option.
- **Accepted:** Free-form tags with cleanup; search across title and URL; no automatic sample data; exact tag filter matching; `All tags` removes the filter.
- **Modified/rejected:** Tag filtering does not use partial matches; sample data is not auto-inserted.

### Third planning pass
- **Your prompt:** You asked for the full consolidated plan and acceptance criteria, and you asked me to check whether edit/delete behaviour was still unresolved.
- **My response:** I summarized the confirmed plan and asked about the editable fields, duplicate checking during editing, edit ordering, and delete confirmation/cancellation.
- **Your response:** You approved editing URL, title, and tags; duplicate checking should exclude the bookmark being edited; editing preserves creation-time ordering; and deletion must require confirmation.
- **Accepted:** Full editing of URL, title, and tags; duplicate checking excludes the edited bookmark itself; creation-time ordering remains the basis for newest-first; delete confirmation is required.
- **Modified/rejected:** Editing does not move bookmarks to the top; cancel on delete leaves the bookmark unchanged.

### Final clarification before saving this planning file
- **Your prompt:** You clarified that editing preserves creation time and creation-based ordering, but displayed results must refresh after editing and the bookmark must disappear from filtered results if it no longer matches the active search or tag filter. You then asked for the planning file to be populated directly and requested several corrections.
- **My response:** I prepared the planning content for the file and then corrected it to include the refreshed-result behaviour, the expanded conversation record, the revised implementation order, scope boundaries, and the new acceptance criterion for empty or whitespace-only URLs.
- **Accepted:** Refresh displayed results after editing; remove edited bookmarks from filtered results if they no longer match; preserve the approved requirements and IDs.
- **Modified/rejected:** Unapproved exclusions are listed as proposed scope boundaries rather than confirmed decisions.

## Feature implementation order
1. Define bookmark data rules, URL validation, title validation, duplicate detection, and tag cleanup rules.
2. Implement add/save bookmark flow, including tag cleanup and persistent storage.
3. Test and review the add/save flow.
4. Implement persistent storage load/reload behaviour.
5. Test and review persistence behaviour.
6. Implement newest-first list display based on creation time.
7. Test and review list ordering.
8. Implement search by title and URL.
9. Test and review search behaviour.
10. Implement tag parsing, tag cleanup, and tag filtering with `All tags`.
11. Test and review tag filtering and combined search/filter behaviour.
12. Implement edit flow with validation, duplicate checking, creation-time preservation, and result refresh.
13. Test and review editing behaviour.
14. Implement delete confirmation and persistent removal.
15. Test and review delete behaviour.
16. Implement empty states and no-results states.
17. Test and review empty-state behaviour.
18. Review edge cases and consistency checks.
19. Final planning review.

## Testable acceptance criteria

### Add and validation
- **AC-01:** A user can save a bookmark with a valid `http` or `https` URL and a non-empty title.
- **AC-02:** A user can save a bookmark without tags.
- **AC-03:** The app rejects a title that is empty after trimming and shows `Title is required.`
- **AC-04:** The app rejects an empty or whitespace-only URL, a malformed URL, a URL without `http` or `https`, a URL with another scheme, a single-label host such as `http://chatgpt`, `localhost`, an IP-address URL, or a hostname with empty labels.
- **AC-05:** The app trims spaces from URL, title, and tags before validation and saving.
- **AC-06:** The app stores tags entered as comma-separated values.
- **AC-07:** The app removes empty tag entries, lowercases tags, and removes duplicate tags.

### Length limits
- **AC-36:** The app accepts a trimmed URL of up to 2048 characters and a trimmed title of up to 200 characters.
- **AC-37:** The app rejects a trimmed URL longer than 2048 characters or a trimmed title longer than 200 characters, shows a clear validation message, and does not truncate the value.

### Duplicate handling
- **AC-08:** The app rejects a bookmark if the trimmed URL already exists, even when the title is different.
- **AC-09:** The app shows `This bookmark already exists.` when a duplicate URL is entered.
- **AC-10:** A bookmark is allowed to keep its own URL when edited, as long as no other bookmark uses that URL.
- **AC-11:** The app rejects an edited URL already used by another bookmark and shows `This bookmark already exists.`

### Listing, ordering, and persistence
- **AC-12:** The bookmark list shows newest bookmarks first by creation time.
- **AC-13:** Editing preserves the original creation time and creation-based ordering. Displayed results may change when the edited bookmark no longer matches the active search or tag filter.
- **AC-14:** Saved edits remain after application restart.
- **AC-15:** Deleted bookmarks remain deleted after application restart.
- **AC-16:** Bookmarks remain available after application restart.

### Search and filtering
- **AC-17:** A user can search bookmarks by partial match in title or URL, ignoring case.
- **AC-18:** A blank search input applies no text restriction.
- **AC-19:** Search and tag filtering can be active at the same time.
- **AC-20:** Search and tag filtering must both be satisfied when both are active, and the resulting list remains ordered newest first.
- **AC-21:** Tag filtering matches only the selected normalized tag exactly.
- **AC-22:** Selecting `java` does not match `javascript`.
- **AC-23:** An `All tags` option removes the tag restriction.
- **AC-24:** After editing a bookmark, the displayed search/filter results refresh immediately.
- **AC-25:** If an edited bookmark no longer matches the active search or tag filter, it disappears from the displayed filtered results.

### Edit and delete flows
- **AC-26:** A user can edit a bookmark’s URL, title, and tags.
- **AC-27:** Failed edit validation leaves the saved bookmark unchanged.
- **AC-28:** Cancelling an edit leaves the saved bookmark unchanged.
- **AC-29:** Deleting a bookmark asks for confirmation first.
- **AC-30:** Cancelling delete leaves the bookmark unchanged.
- **AC-31:** Confirming delete removes the bookmark from the list and persistent storage.

### Empty states
- **AC-32:** When no bookmarks exist, the app shows `No bookmarks yet. Add your first bookmark.`
- **AC-33:** When search or filtering returns no matches, the app shows `No matching bookmarks found.`
- **AC-34:** When there are no matches, the app provides a `Clear search and filters` action.
- **AC-35:** The app does not auto-insert sample data.

## Edge cases
- URL is empty or whitespace-only
- Title is empty after trimming
- URL contains leading or trailing spaces
- Title contains leading or trailing spaces
- Tags contain extra spaces
- Tags contain empty entries between commas
- Tags repeat with different casing
- Bookmark has no tags
- Search text is empty
- Search text contains only spaces
- Search returns no results
- Tag filter has no matching bookmarks
- Search and tag filter are both active and only one constraint matches
- Editing a bookmark keeps its own URL unchanged
- Editing a bookmark changes the URL to one that belongs to another bookmark
- Editing a bookmark causes it to stop matching the current search or tag filter
- Editing fails validation and must not alter stored data
- Cancelling an edit must not alter stored data
- Deleting the last bookmark
- Confirming delete and then restarting the application
- Persisted data loading after restart
- Very long titles, URLs, or tag lists
- Duplicate URL entered with different title
- Duplicate URL entered with surrounding spaces
- `http` versus `https` treated as distinct for duplicate checking
- Trailing slash differences treated as distinct for duplicate checking
- Path-case differences treated as distinct for duplicate checking

## Notes for later phases
- The planning phase is complete once the approved behaviour and acceptance criteria are captured here.
- Implementation must follow the confirmed decisions exactly.
- Any future design or build decisions should stay consistent with these rules unless the requirements are intentionally changed.

