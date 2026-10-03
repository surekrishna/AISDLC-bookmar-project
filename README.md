# Personal Bookmark Manager

Personal Bookmark Manager is a Spring Boot MVC and Thymeleaf application for saving and managing bookmarks locally. It supports adding, listing, searching, filtering, editing, and deleting bookmarks with validation and restart persistence.

## Features
- Add bookmarks with URL, title, and optional tags
- List bookmarks newest first
- Search by title or URL
- Filter by one tag or view all tags
- Edit bookmarks while preserving creation order
- Delete bookmarks with confirmation
- Validate required fields, duplicate URLs, URL format, and input lengths
- Keep data after application restart
- Use embedded H2 with no separate database installation

## Required Java version
- Java 21 or later

## Build, test, and run from Windows PowerShell
Run these commands from the assignment root:

```powershell
.\gradlew.bat clean build
.\gradlew.bat test
.\gradlew.bat bootRun
```

## Correct working directory
Start the app from the assignment repository root (BookmarkManagerApplication.java main class) so the relative database path resolves correctly.

## Browser URL
After the app starts, open:

```text
http://localhost:8080/bookmarks
```

## Embedded H2 and persistence
The application uses an embedded file-backed H2 database, so there is no separate database server to install or start.

- Application database location: `./data/bookmarks`
- H2 creates local files such as `bookmarks.mv.db` and, if needed, `bookmarks.trace.db`
- Persistence works because the database is file-backed, so saved bookmarks remain available after restart
- Keep the app started from the repository root so the relative path points to the same database file every time

## Project documents
- [`docs/01-planning.md`](docs/01-planning.md)
- [`docs/02-design.md`](docs/02-design.md)
- [`docs/03-build.md`](docs/03-build.md)
- [`docs/04-testing.md`](docs/04-testing.md)
- [`docs/05-review.md`](docs/05-review.md)
- [`docs/06-reflection.md`](docs/06-reflection.md)

## Notes
- The repository is intended to run from the assignment root shown above.
- Do not hardcode a personal JDK path in this README.
- If you use IntelliJ, reimport the Gradle project from the assignment root after opening the workspace.
