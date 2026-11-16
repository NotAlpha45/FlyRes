package com.flyres.control;

import static com.flyres.control.AuthenticationController.CONFIRM_PASSWORD;
import static com.flyres.control.AuthenticationController.EMAIL;
import static com.flyres.control.AuthenticationController.FIRST_NAME;
import static com.flyres.control.AuthenticationController.LAST_NAME;
import static com.flyres.control.AuthenticationController.LOGIN_FAILED;
import static com.flyres.control.AuthenticationController.PASSWORD;
import static com.flyres.control.AuthenticationController.PHONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flyres.common.exception.ValidationException;
import com.flyres.common.security.PasswordHasher;
import com.flyres.data.DatabaseManager;
import com.flyres.data.sqlite.SqliteUserRepository;
import com.flyres.domain.user.Customer;
import com.flyres.domain.user.Role;
import com.flyres.domain.user.User;
import com.flyres.repository.UserCredentials;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AuthenticationControllerTest {

    private static final ZoneId ZONE = ZoneId.of("America/Edmonton");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-16T16:30:45.123Z"), ZONE);

    private DatabaseManager database;
    private SqliteUserRepository users;
    private AuthenticationController authentication;

    @BeforeEach
    void setUp() {
        database = DatabaseManager.forTestingWithSeedData();
        users = new SqliteUserRepository(database);
        authentication = new AuthenticationController(users, new PasswordHasher(), CLOCK);
    }

    @AfterEach
    void tearDown() {
        database.close();
    }

    // ---------- login ----------

    @ParameterizedTest
    @CsvSource({
            "admin@flyres.ca,  Admin#2026,    ADMIN",
            "agent@flyres.ca,  Agent#2026,    AGENT",
            "maya@example.com, Customer#2026, CUSTOMER",
            "omar@example.com, Customer#2026, CUSTOMER"})
    void login_seededAccount_returnsUserWithRole(String email, String password, Role role) {
        User user = authentication.login(email, password);

        assertEquals(email, user.getEmail());
        assertEquals(role, user.getRole());
    }

    @Test
    void login_emailInDifferentCaseWithSpaces_returnsUser() {
        User user = authentication.login("  MAYA@Example.com ", "Customer#2026");

        assertEquals("maya@example.com", user.getEmail());
    }

    @Test
    void login_wrongPassword_throwsWithGenericMessage() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> authentication.login("maya@example.com", "Wrong#2026"));

        assertEquals(LOGIN_FAILED, e.getMessage());
    }

    @Test
    void login_unknownEmail_throwsSameGenericMessageOnSameField() {
        ValidationException unknown = assertThrows(ValidationException.class,
                () -> authentication.login("nobody@example.com", "Customer#2026"));
        ValidationException wrong = assertThrows(ValidationException.class,
                () -> authentication.login("maya@example.com", "Wrong#2026"));

        assertEquals(LOGIN_FAILED, unknown.getMessage());
        assertEquals(wrong.getFieldErrors(), unknown.getFieldErrors());
    }

    @Test
    void login_blankEmailAndPassword_throwsValidationExceptionForBoth() {
        ValidationException e = assertThrows(ValidationException.class, () -> authentication.login("  ", ""));

        assertEquals(List.of(EMAIL, PASSWORD), List.copyOf(e.getFieldErrors().keySet()));
        assertNotEquals(LOGIN_FAILED, e.getFieldErrors().get(EMAIL));
    }

    // ---------- registerCustomer ----------

    @Test
    void registerCustomer_validRequest_createsCustomer() {
        Customer customer = authentication.registerCustomer(request("  Nadia ", " Khan ", " Nadia@Example.com ",
                "403-555-0177", "Fly#2026x", "Fly#2026x"));

        assertTrue(customer.getId() > 0);
        assertEquals("nadia@example.com", customer.getEmail());
        assertEquals("Nadia Khan", customer.getFullName());
        assertEquals("403-555-0177", customer.getPhone().orElseThrow());
        assertEquals(LocalDateTime.of(2026, 11, 16, 9, 30, 45), customer.getCreatedAt());
    }

    @Test
    void registerCustomer_validRequest_canLogInAndStoresOnlyHash() {
        authentication.registerCustomer(request("Nadia", "Khan", "nadia@example.com", "", "Fly#2026x", "Fly#2026x"));

        User user = authentication.login("nadia@example.com", "Fly#2026x");
        UserCredentials stored = users.findCredentialsByEmail("nadia@example.com").orElseThrow();

        assertEquals(Role.CUSTOMER, user.getRole());
        assertTrue(user.getPhone().isEmpty());
        assertFalse(stored.password().hash().contains("Fly#2026x"));
        assertEquals(44, stored.password().hash().length());
    }

    @Test
    void registerCustomer_duplicateEmailInOtherCase_throwsValidationExceptionOnEmail() {
        ValidationException e = assertThrows(ValidationException.class, () -> authentication.registerCustomer(
                request("Maya", "Again", "MAYA@example.com", null, "Fly#2026x", "Fly#2026x")));

        assertEquals(EMAIL, e.getField());
    }

    @Test
    void registerCustomer_weakPassword_throwsValidationExceptionOnPassword() {
        ValidationException e = assertThrows(ValidationException.class, () -> authentication.registerCustomer(
                request("Nadia", "Khan", "nadia@example.com", null, "password", "password")));

        assertEquals(List.of(PASSWORD), List.copyOf(e.getFieldErrors().keySet()));
    }

    @Test
    void registerCustomer_passwordsDoNotMatch_throwsValidationExceptionOnConfirmPassword() {
        ValidationException e = assertThrows(ValidationException.class, () -> authentication.registerCustomer(
                request("Nadia", "Khan", "nadia@example.com", null, "Fly#2026x", "Fly#2026y")));

        assertEquals(List.of(CONFIRM_PASSWORD), List.copyOf(e.getFieldErrors().keySet()));
    }

    @Test
    void registerCustomer_everyFieldInvalid_reportsAllFieldsInFormOrder() {
        ValidationException e = assertThrows(ValidationException.class, () -> authentication.registerCustomer(
                request("", " ", "not-an-email", "12", "short", "different")));

        assertEquals(List.of(FIRST_NAME, LAST_NAME, EMAIL, PHONE, PASSWORD, CONFIRM_PASSWORD),
                List.copyOf(e.getFieldErrors().keySet()));
    }

    @Test
    void registerCustomer_invalidRequest_persistsNothing() {
        assertThrows(ValidationException.class, () -> authentication.registerCustomer(
                request("Nadia", "Khan", "nadia@example.com", null, "Fly#2026x", "mismatch")));

        assertFalse(users.existsByEmail("nadia@example.com"));
    }

    @Test
    void registerCustomer_nameTooLong_throwsValidationExceptionOnName() {
        ValidationException e = assertThrows(ValidationException.class, () -> authentication.registerCustomer(
                request("N".repeat(51), "Khan", "nadia@example.com", null, "Fly#2026x", "Fly#2026x")));

        assertEquals(FIRST_NAME, e.getField());
    }

    private static RegistrationRequest request(String first, String last, String email, String phone,
                                               String password, String confirm) {
        return new RegistrationRequest(first, last, email, phone, password, confirm);
    }
}
