package com.flyres.control;

import com.flyres.common.exception.ValidationException;
import com.flyres.common.security.PasswordHash;
import com.flyres.common.security.PasswordHasher;
import com.flyres.common.validation.ValidationErrors;
import com.flyres.common.validation.Validators;
import com.flyres.domain.user.Customer;
import com.flyres.domain.user.Role;
import com.flyres.domain.user.User;
import com.flyres.repository.NewUser;
import com.flyres.repository.UserCredentials;
import com.flyres.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

/**
 * &lt;&lt;control&gt;&gt; Login and customer self-registration.
 * <p>
 * Validation errors use the field names below, which the login and registration views register
 * with their {@code FormValidator}.
 */
public final class AuthenticationController {

    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    public static final String EMAIL = "email";
    public static final String PHONE = "phone";
    public static final String PASSWORD = "password";
    public static final String CONFIRM_PASSWORD = "confirmPassword";

    /** The only message a failed login ever shows, so it never reveals whether the email exists. */
    public static final String LOGIN_FAILED = "Email or password is incorrect.";

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final Clock clock;

    /** Hash checked when the email is unknown, so both failures take the same time. */
    private PasswordHash decoyHash;

    public AuthenticationController(UserRepository users, PasswordHasher hasher, Clock clock) {
        this.users = Objects.requireNonNull(users);
        this.hasher = Objects.requireNonNull(hasher);
        this.clock = Objects.requireNonNull(clock);
    }

    /** Returns the user with this email and password; any mismatch throws {@link #LOGIN_FAILED}. */
    public User login(String email, String password) {
        ValidationErrors errors = new ValidationErrors();
        if (Validators.isBlank(email)) {
            errors.add(EMAIL, "Enter your email.");
        }
        if (password == null || password.isEmpty()) {
            errors.add(PASSWORD, "Enter your password.");
        }
        errors.throwIfAny();

        Optional<UserCredentials> found = users.findCredentialsByEmail(Validators.normalizeEmail(email));
        if (found.isEmpty()) {
            hasher.verify(password, decoyHash());
            throw loginFailed();
        }
        if (!hasher.verify(password, found.get().password())) {
            throw loginFailed();
        }
        return found.get().user();
    }

    /** Creates a customer account from the registration form and returns it. */
    public Customer registerCustomer(RegistrationRequest request) {
        Objects.requireNonNull(request, "request");
        ValidationErrors errors = new ValidationErrors();
        checkName(errors, FIRST_NAME, request.firstName(), "first name");
        checkName(errors, LAST_NAME, request.lastName(), "last name");

        if (Validators.isBlank(request.email())) {
            errors.add(EMAIL, "Enter your email.");
        } else if (!Validators.isEmail(request.email())) {
            errors.add(EMAIL, "Enter a valid email, like name@example.com.");
        }

        if (!Validators.isBlank(request.phone()) && !Validators.isPhone(request.phone())) {
            errors.add(PHONE, "Enter a valid phone number, like 403-555-0142.");
        }

        if (!Validators.isStrongPassword(request.password())) {
            errors.add(PASSWORD, "Use " + Validators.PASSWORD_MIN_LENGTH + " to " + Validators.PASSWORD_MAX_LENGTH
                    + " characters, with at least one letter and one digit.");
        }
        if (!Objects.equals(request.password(), request.confirmPassword())) {
            errors.add(CONFIRM_PASSWORD, "Passwords do not match.");
        }
        errors.throwIfAny();

        String email = Validators.normalizeEmail(request.email());
        if (users.existsByEmail(email)) {
            throw new ValidationException(EMAIL, "An account with this email already exists.");
        }

        NewUser customer = new NewUser(
                Role.CUSTOMER,
                email,
                request.firstName().strip(),
                request.lastName().strip(),
                Validators.trimToNull(request.phone()),
                hasher.hash(request.password()),
                LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS));
        return (Customer) users.insert(customer);
    }

    private static void checkName(ValidationErrors errors, String field, String value, String label) {
        if (Validators.isBlank(value)) {
            errors.add(field, "Enter your " + label + ".");
        } else if (value.strip().length() > Validators.NAME_MAX_LENGTH) {
            errors.add(field, "Use at most " + Validators.NAME_MAX_LENGTH + " characters.");
        }
    }

    private static ValidationException loginFailed() {
        return new ValidationException(PASSWORD, LOGIN_FAILED);
    }

    private PasswordHash decoyHash() {
        if (decoyHash == null) {
            decoyHash = hasher.hash("decoy-password-never-matches");
        }
        return decoyHash;
    }
}
