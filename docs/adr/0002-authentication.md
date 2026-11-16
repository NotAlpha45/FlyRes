# ADR-0002: Authentication — login, customer registration, logout

- **Status:** Accepted
- **Date:** 2026-11-16
- **Issue:** FLY-04
- **Control(s):** `AuthenticationController`

## Context

Every later feature needs to know who is acting. Booking, cancelling and all admin work check the acting `User`'s role inside the control (architecture rule 7), and promotions are delivered on a customer's first login of the month. FLY-04 therefore builds the user model and the login boundary that everything else depends on.

Rules from `CLAUDE.md` that constrain the design:

- One `users` table with a `role` column (`CUSTOMER`, `AGENT`, `ADMIN`). `UserFactory` builds the right subclass from it.
- Passwords are hashed with PBKDF2 and a per-user salt, and are never stored or logged in plain text.
- A failed login always shows *"Email or password is incorrect."*
- Customers can register themselves from the login screen. Agents and admins cannot.
- The UI may import only the facade, `control`, `domain` and `common.exception`.

## Feature overview

- **Log in (all roles):** the user enters an email and password.
  - On success, the shell opens with a sidebar for their role and their name and role in the top bar.
  - Email is matched case-insensitively and surrounding spaces are ignored.
  - An empty email or password shows a per-field error.
  - A wrong password and an unknown email both show the same generic message under the password field.
- **Create an account (customers only):** reached from the login screen. The form asks for first and last name, email, an optional phone and a password with confirmation.
  - Every invalid field is reported at once, inline.
  - A duplicate email is reported on the email field.
  - On success, a toast confirms the account and the login screen opens with the email already filled in. Registering does not log the user in.
- **Log out:** a button in the top bar clears the session and returns to the login screen.
- **Guard:** asking for a shell screen with nobody logged in shows the login screen instead.

## Design

| Layer | Class | Responsibility |
| --- | --- | --- |
| `<<boundary>>` | `LoginViewController`, `RegisterViewController` | Read the form, make one control call, show field errors with `FormValidator` or move on. |
| UI support | `AppSession`, `SceneRouter`, `ViewContext`, `ViewId` | Hold the current user, inject dependencies into view controllers, guard and filter navigation by role. |
| `<<control>>` | `AuthenticationController` | `login`, `registerCustomer`: validation, normalisation, hashing, the generic failure message. |
| `<<entity>>` | `User` (sealed) → `Customer`, `FlightAgent`, `Administrator`; `Role`; `UserFactory` | The user model. The factory is the only way to build a user. |
| Repository | `UserRepository`, `UserCredentials`, `NewUser` | Data access contract. Credentials travel separately from `User`. |
| Data | `SqliteUserRepository` | SQL with `PreparedStatement`; maps rows through `UserFactory`. |
| Common | `PasswordHasher`, `PasswordHash`, `Validators`, `ValidationErrors`, `ValidationException` (+ `NotFoundException`, `AccessDeniedException`, `BookingException`) | Shared security, validation and the exception set. |

**Login sequence.** `LoginViewController.onLogin` → `FlyResBackend.authentication().login(email, password)`. The control then:

1. Checks that both fields are filled in.
2. Normalises the email.
3. Calls `UserRepository.findCredentialsByEmail`.
4. Calls `PasswordHasher.verify`.
5. Returns the `User`.

The view controller then calls `SceneRouter.startSession(user)`. That stores the user in `AppSession`, configures the shell for the user's role, and opens `HOME`.

**Registration sequence.** `RegisterViewController.onCreateAccount` → `registerCustomer(RegistrationRequest)`. The control then:

1. Validates every field and collects the errors with `ValidationErrors`.
2. Calls `existsByEmail`.
3. Calls `PasswordHasher.hash`.
4. Calls `UserRepository.insert(NewUser)` with role `CUSTOMER` and `createdAt` from the injected `Clock`.

The view controller then shows a toast and calls `goTo(LOGIN)`, followed by `prefillEmail`.

## Decisions

### D1. Password hashing: PBKDF2WithHmacSHA512, 210 000 iterations, 256-bit key, 16-byte salt

- **Options:** PBKDF2 from the JDK; bcrypt or Argon2 from a library.
- **Decision:** PBKDF2WithHmacSHA512 from `javax.crypto`. Hash and salt are stored Base64-encoded in `password_hash` and `password_salt`. `verify` compares in constant time with `MessageDigest.isEqual`.
- **Why:**
  - Our project rules in `CLAUDE.md` ask for PBKDF2.
  - It needs no new dependency.
  - 210 000 iterations of SHA-512 is the OWASP recommendation.
  - The parameters are fixed by the hashes already in `seed.sql`, and `PasswordHasherTest` pins them against the seeded admin hash.
- `PasswordHash.toString()` is redacted so a hash cannot reach a log by accident.

### D2. A failed login is a `ValidationException` on the `password` field, and an unknown email costs the same as a wrong password

- **Options:** reuse `AccessDeniedException`; add a new `AuthenticationException`; reuse `ValidationException`.
- **Decision:** `ValidationException(PASSWORD, LOGIN_FAILED)` for both causes. When the email is unknown, the control still runs `verify` against a decoy hash.
- **Why:**
  - `AccessDeniedException` means "your role may not do this", which is authorisation. Reusing it for bad credentials would blur the two meanings.
  - A new exception type would grow the fixed exception set for one call site.
  - A failed login is invalid input, and showing it under the password field fits the existing inline-error pattern.
  - Identical message, field and timing mean the login form never reveals whether an email is registered.

### D3. `ValidationException` carries every invalid field at once

- **Options:** one field per exception; a map of field → message.
- **Decision:** `ValidationException` holds an ordered `Map<String, String>`. Controls collect errors with `common.validation.ValidationErrors` and call `throwIfAny()`. The field names are public constants on `AuthenticationController` (`EMAIL`, `PASSWORD`, …), and the views register their inputs under the same constants.
- **Why:** a registration form that reveals one error per click is frustrating. The single-field constructor is kept for the common case. Shared constants make it impossible for a control and its view to disagree on a field name.

### D4. Password hashes never travel inside `User`

- **Options:** add `passwordHash` and `salt` to `User`; return the hash separately.
- **Decision:** `UserRepository.findCredentialsByEmail` returns a `UserCredentials(User, PasswordHash)` record from the `repository` package. `User` has no password fields.
- **Why:** `User` objects go all the way up to the UI (`AppSession`, the top bar). The UI cannot import `repository` or `common.security`, and `ArchitectureTest` rule 1 enforces that. Keeping the hash in those packages means the compiler and ArchUnit, not discipline, stop it from reaching a boundary.

### D5. A sealed `User` hierarchy built only by `UserFactory`

- **Options:** one `User` class with a role field; an open hierarchy with public constructors; a sealed hierarchy with package-private constructors.
- **Decision:**
  - `User` is `abstract sealed`, permitting `Customer`, `FlightAgent` and `Administrator`.
  - Their constructors are package-private.
  - `UserFactory.create(role, …)` switches over `Role` and builds the matching subclass.
- **Why:**
  - It makes the Factory pattern real, not decorative: the factory is the only way to build a user.
  - The `switch` is exhaustive, so adding a role does not compile until the factory handles it.
  - The subclasses let later controls accept a `Customer` where only a customer makes sense.

### D6. Email normalisation and duplicate handling

- **Decision:** the control trims and lower-cases every email before a lookup or insert. The column is also `COLLATE NOCASE UNIQUE`. `registerCustomer` checks `existsByEmail` first and reports "An account with this email already exists." on the email field. The `UNIQUE` constraint is only a backstop.
- **Why:** normalising gives one stored form and the friendly error message. The database constraint keeps the data correct even if a later code path forgets to check. Registration is a single insert, not a multi-step write, so it does not go through `TransactionRunner`. In a single-user desktop app the check-then-insert gap cannot be raced.
- **Accepted trade-off:** registration reveals that an email exists. That is needed to give a useful error, and login, the attack surface that matters, reveals nothing.

### D7. Registration policy

- **Decision:**
  - Names: required, at most 50 characters.
  - Email: `name@domain.tld`, at most 254 characters.
  - Phone: optional; 7–20 digits, spaces, dashes or parentheses, with an optional leading `+`.
  - Password: 8–128 characters with at least one letter and one digit; confirmation must match.
  - The role is always `CUSTOMER`. There is no role field to tamper with.
- **Why:** these are simple rules that are easy to demo, and every seeded demo password satisfies them. The checks live in `common.validation.Validators` so the agent-created customer profile (a later issue) can reuse them.

### D8. View controllers receive their dependencies through `ViewContext`

- **Options:** static singletons or a service locator; a DI framework (not allowed); constructor injection through an FXML controller factory.
- **Decision:**
  - `SceneRouter` installs a controller factory on every `FXMLLoader`.
  - A view controller with a public `(ViewContext)` constructor receives `ViewContext(backend, session, router)`; other controllers use their no-argument constructor.
  - `goTo` returns the new view's controller, so one screen can hand data to the next, as `prefillEmail` does.
- **Why:** dependencies stay explicit and testable without globals or a framework. The view controller still does only what rule 6 allows: read input, call one control method, show the result.

### D9. Session and role-based navigation live in the UI, and role checks stay in the controls

- **Decision:**
  - `AppSession` holds the logged-in `User`; `requireUser()` supplies the acting user for later control calls.
  - Each `ViewId` lists the roles that see it in the sidebar.
  - `SceneRouter.startSession` rebuilds the sidebar for the user's role. `goTo` sends a logged-out user to the login screen.
- **Why:** this is navigation state, so it belongs in the boundary layer. Hiding a sidebar entry is cosmetic only (rule 7). Every role-restricted control method from FLY-05 onwards still takes the acting `User` and throws `AccessDeniedException`.

## Consequences

- Later issues can call `session.requireUser()` and pass the result into controls; `AccessDeniedException`, `NotFoundException` and `BookingException` already exist for them.
- The agent-created customer profile (`CustomerManagementController`) can reuse `Validators`, `PasswordHasher` and `UserRepository.insert`.
- The promotions issue adds its first-login-of-the-month hook after `login` succeeds. This issue leaves `login` returning only the `User`.
- A login takes roughly 0.1–0.3 s of PBKDF2 work on the FX thread. That is acceptable for one deliberate click and consistent with the "synchronous unless measurably slow" UI rule.
- **Limitations, by design:** no password reset, no account lockout, no "remember me".

## Verification

- `PasswordHasherTest`: round trip; wrong password; a fresh salt per hash; key and salt lengths; the seeded admin hash verifies; redacted `toString`.
- `ValidatorsTest`: email, password, phone and normalisation rules; `ValidationErrors` ordering.
- `UserFactoryTest`: each role maps to its subclass.
- `SqliteUserRepositoryTest`: insert and lookup; case-insensitive email; duplicate email rejected by the database; seeded rows map to the right subclasses.
- `AuthenticationControllerTest`:
  - All four demo accounts log in; case and spaces in the email are ignored.
  - A wrong password and an unknown email fail identically; blank fields are reported per field.
  - Registration succeeds, stores only the hash, and the new account can log in.
  - Duplicate email, weak password, mismatched confirmation and all-fields-invalid each fail on the right fields, and an invalid request persists nothing.
- By hand: `rm -rf ~/.flyres && ./gradlew run`, then walk through the FLY-04 acceptance criteria.
