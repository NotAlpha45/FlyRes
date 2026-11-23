# ADR-0003: Handling unexpected errors

- **Status:** Accepted
- **Date:** 2026-11-22
- **Issue:** FLY-28
- **Control(s):** none (presentation and data layers)

## Context

View controllers catch only the business exceptions they expect, mostly `ValidationException`. Any other exception from a control, such as a `DataAccessException` when another program locks the database, escaped to the JavaFX thread. JavaFX printed a stack trace to the console, and the user saw nothing: the button seemed to do nothing. A failure to open the database at startup crashed the app with no window at all.

Every new screen copies the login and registration pattern, so the fix has to be central, not per screen. The constraints:

- Never swallow an exception silently.
- Show business errors inline or as a specific toast.
- Never show SQL, stack traces or file contents to the user.
- No new dependencies.

## Feature overview

- **Unexpected error while using the app:**
  - The user sees the error toast "Something went wrong and your last action was not saved. Please try again."
  - The screen stays usable.
  - The stack trace is logged.
- **Database can't be opened at startup (locked, unreadable or corrupt):**
  - A "FlyRes could not start" dialog explains what to do.
  - Closing it exits the app cleanly.
- **Briefly locked database:**
  - A statement waits up to 3 s for the lock before failing, so a short lock from another program doesn't produce an error at all.

## Design

| Layer | Class | Responsibility |
| --- | --- | --- |
| `<<boundary>>` support | `ui.component.ErrorReporter` | Installs the uncaught-exception handler on the FX thread and as the default handler; logs the error and shows the generic toast (or a dialog if no window is showing); shows the startup-failure dialog. |
| `<<boundary>>` support | `ui.FlyResApp` | Catches a backend startup failure in `init()`, reports it in `start()` and exits; otherwise installs `ErrorReporter` before building the UI. |
| Data | `data.DatabaseManager` | Sets `PRAGMA busy_timeout = 3000` on every connection; reads the schema once when opening an existing file, so a non-SQLite or corrupt file fails at startup. |

## Decisions

### D1. One global safety net, not a catch-all in every view controller

- **Options:**
  - A. Every view controller catches `RuntimeException`.
  - B. A shared helper that every view controller wraps its calls in.
  - C. An uncaught-exception handler installed once.
- **Decision:** C.
- **Why:**
  - JavaFX already routes exceptions from event handlers to the FX thread's uncaught-exception handler, so one installation covers every current and future screen.
  - View controllers keep handling only the exceptions they expect (rule 6 stays readable).
  - A forgotten wrapper (option B) or a blanket `catch` (option A) would hide real bugs.
  - The handler always logs, so nothing is swallowed.

### D2. A generic message, never the exception text

- **Options:** show `e.getMessage()`; show a generic message.
- **Decision:** a generic message, with the full stack trace in the log.
- **Why:** unexpected exceptions carry technical text such as SQL error codes and file paths. Users can't act on it, and it can leak internals.

### D3. Startup failures are caught in `init()` and reported in `start()`

- **Options:**
  - Let the exception escape `init()`.
  - Show a dialog from `init()`.
  - Catch it in `init()` and report it in `start()`.
- **Decision:** catch in `init()`, report in `start()`, then `Platform.exit()`.
- **Why:**
  - `init()` runs on the launcher thread, where dialogs can't be shown.
  - An escaping exception kills the toolkit with no message.
  - The dialog text tells the user what to do (close the other program or reset the database) without showing the path or the cause. Those go to the log.

### D4. Fail fast on a corrupt database file

- **Options:** keep SQLite's lazy open (the first query fails, e.g. at login); read the schema once when opening an existing file.
- **Decision:** read `sqlite_master` once in `openFile` for existing files.
- **Why:**
  - Without it, a non-SQLite file opens "successfully" and the first click fails instead.
  - The file is left untouched (it may be the user's data).
  - Only a newly created, half-built file is deleted, as before.

### D5. `busy_timeout` of 3 seconds

- **Decision:** `PRAGMA busy_timeout = 3000` next to `foreign_keys`.
- **Why:**
  - A brief lock (e.g. a DB browser reading the file) shouldn't turn into an error.
  - 3 s is short enough that a held lock still reaches the error toast quickly.

## Consequences

- New screens get error reporting for free, and their view controllers stay small.
- Business exceptions must still be caught where they are expected. If a view forgets one, the user sees the generic toast instead of the specific message, which is a visible signal to fix the view.
- Errors on background threads are reported on the FX thread through `Platform.runLater`.
- Logging uses `System.Logger` (JDK), so it needs no dependency. Output goes to the console.

## Verification

- `DatabaseManagerTest`:
  - `forTesting_newDatabase_busyTimeoutSet`
  - `openFile_lockedByAnotherConnection_waitsBeforeFailing`
  - `openFile_notASqliteFile_throwsAndKeepsFile`
- By hand, with a throwaway home folder (`java -Duser.home=<dir> -jar FlyRes.jar`):
  - Replace `flyres.db` with a text file → the "FlyRes could not start" dialog appears, and Close exits.
  - Hold `BEGIN EXCLUSIVE` on the database from another program, then log in → the generic error toast appears after about 3 s, the stack trace is logged, and the form stays usable. Release the lock and log in again → it succeeds.
