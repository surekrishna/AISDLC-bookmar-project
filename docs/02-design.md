# Provisional Design

## Project objective

Build a locally runnable Personal Bookmark Manager that lets a user save, find, edit, filter, and delete bookmarks while keeping the data after restart.

This design follows the confirmed planning rules in `docs/01-planning.md` and the approved technology choices:

- Spring Boot MVC with Thymeleaf
- Gradle
- embedded file-backed H2 with no separate database installation
- Flyway migrations with Hibernate in `validate` mode
- separate test storage
- proper HTTP(S) URL validation with a dotted hostname made of non-empty labels

## Approved technology choices and trade-offs

### Spring Boot MVC with Thymeleaf

**Why this choice fits the assignment**
- keeps the app small and self-contained
- avoids a separate frontend build pipeline
- works well for form-driven CRUD screens
- is a natural fit for Java and Spring Boot experience

**Trade-offs**
- less interactive than a single-page app
- page refreshes are part of normal navigation
- UI flexibility is lower than a separate frontend framework

### Gradle

**Why this choice fits the assignment**
- is our approved build-tool choice for this assignment
- keeps dependency management and test execution simple
- easy to run from the command line or IDE

**Trade-offs**
- requires keeping the build script tidy
- generated files and local database files must be kept out of Git

### Embedded file-backed H2

**Why this choice fits the assignment**
- no extra database service to install or run
- persistent across restarts
- lightweight for a local assignment project

**Trade-offs**
- H2 is convenient for local development but not the same as a production database
- the database file must be handled carefully to avoid accidental resets

### Flyway migrations with Hibernate `validate`

**Why this choice fits the assignment**
- schema changes are explicit and versioned
- startup does not rely on Hibernate to create or drop tables
- `validate` confirms the schema matches the entity model without changing data

**Trade-offs**
- adds one more tool to the setup
- requires a migration file for the initial schema

### Separate test storage

**Why this choice fits the assignment**
- automated tests never touch the saved bookmark database
- test runs can be repeated safely
- persistence behaviour can still be tested in isolation

**Trade-offs**
- requires a separate test database configuration
- persistent integration tests need temporary storage management

## Architecture overview

The application follows a simple layered design:

- **Controller layer** handles page routing, form submission, query parameters, and redirect flow.
- **Service layer** owns business rules: validation, tag normalization, duplicate checking, and edit/delete behaviour.
- **Repository layer** talks to the database and provides query methods for listing, searching, filtering, and existence checks.
- **Entity/model layer** represents bookmarks and normalized tags in persistence.

This keeps web concerns, business rules, and persistence separate while staying small enough for the assignment.

## Design details

### 1. Controller, service, and repository responsibilities

#### Controller responsibilities

The controller should only coordinate HTTP requests and views.

It is responsible for:
- showing the bookmark list page
- showing the add-bookmark form
- showing the edit-bookmark form
- handling form submissions for create and edit
- handling delete confirmation and deletion requests
- passing search and tag-filter parameters to the list view
- redirecting back to the list page after successful create, edit, or delete
- preserving the current search/filter state in redirects where practical

The controller should not:
- implement validation rules itself
- normalize tags itself beyond binding form values
- decide duplicate rules on its own
- query the database directly

#### Service responsibilities

The service layer should contain the bookmark business rules.

It is responsible for:
- trimming input values before validation and save
- validating that URL and title are present
- validating that the URL is a complete HTTP(S) URL with a syntactically valid dotted hostname made of non-empty labels
- preserving the trimmed URL exactly as entered after trimming
- normalizing tags to lowercase and removing duplicates
- rejecting duplicate URLs using exact trimmed-string comparison
- excluding the current bookmark during edit duplicate checks
- preserving creation time on edit
- deciding whether a bookmark belongs in the current search/filter result set
- raising clear validation errors for the controller to display

#### Repository responsibilities

The repository layer should provide persistence operations only.

It is responsible for:
- saving and loading bookmarks
- returning newest-first results by creation time
- searching title and URL text efficiently enough for this small project
- filtering by a single normalized tag
- checking whether a trimmed URL already exists
- checking whether another bookmark already uses a trimmed URL during edit
- returning distinct tags for the filter dropdown

The repository should not:
- trim or normalize business inputs
- decide whether the data is valid
- implement controller flow

### 2. Bookmark fields, types, and constraints

#### Bookmark entity

Recommended fields:

- `id`: `Long`
  - database-generated primary key
- `url`: `String`
  - required
  - stored as the trimmed value exactly as entered after trimming
  - must be unique across bookmarks
  - maximum length: 2048 characters after trimming
  - values above the limit must be rejected with a clear validation message and must not be truncated
- `title`: `String`
  - required
  - stored after trimming
  - should not be blank after trimming
  - maximum length: 200 characters after trimming
  - values above the limit must be rejected with a clear validation message and must not be truncated
- `createdAt`: `Instant` or `LocalDateTime`
  - required
  - used for newest-first ordering
  - preserved during edit
- `updatedAt`: `Instant` or `LocalDateTime`
  - optional
  - useful if we want to show last-modified information later

#### Constraints

- URL is mandatory
- title is mandatory
- tags are optional
- creation time is mandatory and immutable after creation
- database-level uniqueness should exist on the stored trimmed URL
- form validation, service-layer validation, and database column lengths must use the same limits for URL and title

### 3. How normalized tags will be stored and queried

Tags are entered as comma-separated values in the form.

Normalization rules:
- trim each tag
- ignore empty entries
- convert to lowercase
- remove duplicates while preserving the cleaned set of tags

Recommended storage model:
- store tags in a separate child table using a bookmark-to-tag relationship
- each stored tag value is the normalized lowercase string
- the database contains one row per bookmark-tag pair

This design keeps querying simple because the filter compares exact normalized values.

Query behaviour:
- the list page can load all distinct tag values for the filter dropdown
- the selected tag is matched exactly against normalized stored tags
- `java` must not match `javascript`
- the `All tags` option removes the tag restriction entirely

This matches the planning rules and keeps tag handling explicit and predictable.

### 4. Page routes and form submission flows

The app should stay page-based and simple.

Inline validation messages should be rendered in the form itself, using a shared error class such as `.form-message--error` for current and future forms. The add form keeps `novalidate` so browser-native popups do not prevent consistent backend-driven validation styling, and the inline messages provide a non-colour cue through explicit wording and a visible alert-style block.

#### List page

- `GET /bookmarks`
- shows bookmarks newest first
- bookmarks are ordered by `created_at` descending, with `id` descending as a deterministic tie-breaker when timestamps match
- supports query parameters for search text and selected tag
- includes the add-bookmark entry point
- includes the clear-search-and-filters action

#### Add flow

- `GET /bookmarks/new`
  - shows the blank add form
- `POST /bookmarks`
  - submits URL, title, and tags
  - on success, redirects to the list page
  - on validation failure, redisplays the form with messages

#### Edit flow

- `GET /bookmarks/{id}/edit`
  - shows the form prefilled with the bookmark’s current values
- `POST /bookmarks/{id}` or a similarly small form submission endpoint
  - submits the updated fields
  - on success, redirects back to the list page
  - on validation failure, redisplays the edit form with messages
- the current search text and selected tag should travel with the edit form, validation errors, save redirect, and cancel action using encoded query parameters so the user returns to the same filtered list context
- if an edit removes the currently active tag from the saved data, the list page should continue showing that active tag as a visible filter until the user clears it

The edit flow must preserve the original creation time and must not move the bookmark to the top.

#### Delete flow

- `POST /bookmarks/{id}/delete`
  - submits only after confirmation
  - on success, removes the bookmark from the repository and redirects back to the list page

#### Delete confirmation page

- `GET /bookmarks/{id}/delete`
  - shows a dedicated confirmation page
  - provides **Confirm** and **Cancel** actions
- `POST /bookmarks/{id}/delete`
  - confirms the deletion and removes the bookmark
- `GET /bookmarks/{id}/edit` and redirect targets should preserve the current search and tag-filter query parameters

The dedicated confirmation page is the approved delete flow.

#### Search and filter flow

- search text and selected tag are passed as query parameters on the list page
- the service combines both conditions when both are present
- an empty search text applies no text restriction
- the `All tags` choice removes tag filtering

#### Result-refresh flow after edit

After a successful edit, the app should return to the list page and re-run the active search/tag filters so the refreshed view is correct.

If the edited bookmark no longer matches the active filter, it disappears from the current results as required by the planning document.

The edit form should carry the current search and tag-filter state in query parameters so the user returns to the same filtered list after saving or cancelling.

### 5. How exact trimmed-URL uniqueness is enforced, including editing

Uniqueness must be based on the exact trimmed URL string only.

Design rules:
- trim whitespace before validation and saving
- do not auto-add a scheme
- do not canonicalize or rewrite the URL value after validation
- do not normalize path case
- do not remove trailing slashes
- do not merge `http` and `https`

Hostname rule:
- require at least one dot in the host name
- reject single-label hosts such as `http://chatgpt` and `localhost`
- reject IP-address URLs
- reject malformed hostnames with empty labels such as `https://example..com` or `https://.com`
- continue allowing valid subdomains, paths, query parameters, fragments, and ports

Storage rule:
- the stored URL is the trimmed URL value only
- the app preserves that trimmed string as the persisted value

Create flow:
- before saving, the service checks whether the trimmed URL already exists
- if it exists, the save is rejected with `This bookmark already exists.`

URL validation message:
- invalid website URLs should display `Enter a valid website URL starting with http:// or https:// and including a domain such as example.com.`

Edit flow:
- the service checks whether another bookmark already uses the trimmed URL
- the bookmark being edited is excluded from the duplicate search
- keeping the same URL is allowed
- changing the URL to one used by another bookmark is rejected with `This bookmark already exists.`

Database rule:
- a unique constraint on the stored `url` column should back up the service-level duplicate check
- the service remains responsible for friendly error handling
- the database constraint remains the final safety net

### 6. Database location and how to run consistently

Approved location:
- `./data/bookmarks` relative to the assignment application working directory

The H2 file should therefore live under the application working directory, for example as files such as:
- `./data/bookmarks.mv.db`
- `./data/bookmarks.trace.db` if trace output is ever created

Consistency rule:
- the app must always be started with the assignment repository root as the working directory
- that ensures the relative `./data/bookmarks` path always resolves to the same file location

Recommended run habit:
- use the assignment repository root when running from the terminal
- in the IDE, set the run configuration working directory to the same repository root

This is important because H2 file paths are resolved relative to the working directory.
If the app is started from a different folder, it would point to a different database file.

### 7. How database files will be excluded from Git

Database files should be treated as local runtime data, not source control content.

Planned Git exclusion approach:
- ignore the `data/` directory that stores the H2 database file
- ignore H2 database artifacts such as `*.mv.db` and `*.trace.db`

This prevents personal bookmark data from being committed and avoids noisy local-only file changes.

### 8. Testing, including a temporary file-backed database test that closes and reopens the database

Testing should stay separated from the saved bookmark database.

#### Unit tests

Focus on business rules:
- URL validation
- title validation
- tag normalization
- duplicate detection
- edit rules
- empty-state decisions

These tests should not require a real persistent database file.

#### Repository and integration tests

Use a dedicated test storage location that is separate from `./data/bookmarks`.

Recommended approach:
- use a temporary file-backed H2 database for persistence tests
- make the database file path unique to the test run
- ensure the test database is deleted after the test completes

#### Temporary file-backed persistence test

One important test should prove persistence across a restart-like cycle:

1. start with a temporary file-backed H2 database
2. save a bookmark
3. close the Spring context or repository resources
4. reopen the database using the same temporary file path
5. verify the bookmark is still present

This test confirms that the file-backed database survives a restart without touching the real application database.

#### Why separate storage matters

- it prevents tests from altering the real saved bookmarks
- it allows repeated test runs without manual cleanup
- it keeps production-like persistence behaviour under test without risk

## Actual design interactions and confirmed decisions

This section records the actual discussion that led to the approved design choices.

### 1. Initial approach comparison

We compared two suitable approaches before choosing one:

- **Approach A:** Spring Boot MVC with Thymeleaf
- **Approach B:** Spring Boot REST API with a separate frontend

The comparison focused on how much setup each approach would require, how small the assignment should stay, and how comfortably the solution would fit Java and Spring Boot.

**Decision:** Spring Boot MVC with Thymeleaf was chosen because it keeps the app smaller, avoids a separate frontend build, and is easier to complete as an assignment.

### 2. Database-installation concern and embedded H2 explanation

You raised a concern that a separate database server could not be installed or run on the laptop.

That led to the embedded H2 design:

- H2 is obtained as a normal project dependency through Gradle
- it runs inside the Spring Boot application process
- no separate database installation or service is required
- the database is stored in a local file so data survives restarts

**Decision:** use embedded file-backed H2 so the app remains self-contained and does not require any external database service.

### 3. URL-validation correction

We first discussed a simple scheme check, but that was corrected because checking only for `http://` or `https://` would not satisfy the planning rules.

The approved rule is to validate a complete HTTP(S) URL with a dotted hostname made of non-empty labels, while preserving the trimmed URL string exactly as entered after trimming.

**Decision:** the app must reject malformed URLs, missing schemes, non-HTTP(S) schemes, and URLs without a dotted hostname made of non-empty labels.

### 4. Playground clarification

You later clarified that `Playground` was a separate testing repository and is not part of this assignment.

**Decision:** all design and implementation planning must target the actual assignment repository containing `docs/01-planning.md`, and must not reference or recreate `Playground`.

### 5. Approved decisions that shape the final design

- the app is a locally runnable Personal Bookmark Manager
- bookmarks have URL, title, optional tags, and creation time
- URL and title are mandatory
- tags are optional and comma-separated
- tags are normalized to lowercase, trimmed, and deduplicated
- duplicate detection is based on the exact trimmed URL only
- the URL must be a complete HTTP(S) URL with a dotted hostname made of non-empty labels
- the app must not auto-add a scheme or check website reachability
- search applies to title and URL with partial, case-insensitive matching
- a single selected tag is matched exactly
- `All tags` removes the tag filter
- editing keeps the original creation time
- editing must refresh displayed search/filter results
- delete requires confirmation
- persistence across restart is required
- no sample data is inserted automatically
- Spring Boot MVC with Thymeleaf is the approved UI approach
- Gradle is the approved build-tool choice
- embedded file-backed H2 is the approved local persistence solution
- Flyway migrations with Hibernate `validate` are the approved schema approach
- separate test storage is required
- the approved maximum lengths after trimming are 2048 characters for URL and 200 characters for title
- the database should live at `./data/bookmarks` relative to the application working directory

## Remaining proposals awaiting approval

No remaining proposals are awaiting approval for the URL/title length limits.

### Already approved in this update

- delete uses a separate confirmation page with **Confirm** and **Cancel**
- search and tag-filter state is preserved using query parameters through the edit flow and redirect
- filter-dropdown tags are displayed alphabetically

## Notes for later phases

- This design document intentionally does not contain application code, migrations, or build-file changes.
- The implementation phase must follow the approved rules exactly.
- Any future changes should remain consistent with the confirmed bookmark behaviour unless the requirements are intentionally revised.

