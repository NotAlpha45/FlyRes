package com.flyres.data.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flyres.common.exception.DataAccessException;
import com.flyres.common.security.PasswordHash;
import com.flyres.data.DatabaseManager;
import com.flyres.domain.user.Administrator;
import com.flyres.domain.user.Customer;
import com.flyres.domain.user.FlightAgent;
import com.flyres.domain.user.Role;
import com.flyres.domain.user.User;
import com.flyres.repository.NewUser;
import com.flyres.repository.UserCredentials;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SqliteUserRepositoryTest {

    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 11, 16, 9, 30, 0);
    private static final PasswordHash HASH = new PasswordHash("hash", "salt");

    private DatabaseManager database;
    private SqliteUserRepository repository;

    @BeforeEach
    void setUp() {
        database = DatabaseManager.forTesting();
        repository = new SqliteUserRepository(database);
    }

    @AfterEach
    void tearDown() {
        database.close();
    }

    @Test
    void insert_newCustomer_returnsCustomerWithGeneratedId() {
        User saved = repository.insert(newUser("new@example.com", "403-555-0100"));

        assertInstanceOf(Customer.class, saved);
        assertTrue(saved.getId() > 0);
        assertEquals("new@example.com", saved.getEmail());
    }

    @Test
    void findCredentialsByEmail_afterInsert_returnsUserAndStoredHash() {
        User saved = repository.insert(newUser("new@example.com", "403-555-0100"));

        UserCredentials found = repository.findCredentialsByEmail("new@example.com").orElseThrow();

        assertEquals(saved, found.user());
        assertEquals("hash", found.password().hash());
        assertEquals("salt", found.password().salt());
        assertEquals(CREATED, found.user().getCreatedAt());
        assertEquals("403-555-0100", found.user().getPhone().orElseThrow());
    }

    @Test
    void findCredentialsByEmail_differentCase_findsUser() {
        repository.insert(newUser("new@example.com", null));

        assertTrue(repository.findCredentialsByEmail("NEW@Example.com").isPresent());
    }

    @Test
    void findCredentialsByEmail_unknownEmail_returnsEmpty() {
        assertTrue(repository.findCredentialsByEmail("nobody@example.com").isEmpty());
    }

    @Test
    void findById_afterInsert_returnsUserWithoutPhone() {
        User saved = repository.insert(newUser("new@example.com", null));

        User found = repository.findById(saved.getId()).orElseThrow();

        assertEquals(saved, found);
        assertTrue(found.getPhone().isEmpty());
    }

    @Test
    void findById_unknownId_returnsEmpty() {
        assertTrue(repository.findById(999).isEmpty());
    }

    @Test
    void existsByEmail_registeredInAnyCase_returnsTrue() {
        repository.insert(newUser("new@example.com", null));

        assertTrue(repository.existsByEmail("New@Example.com"));
        assertFalse(repository.existsByEmail("other@example.com"));
    }

    @Test
    void insert_duplicateEmail_throwsDataAccessException() {
        repository.insert(newUser("new@example.com", null));

        assertThrows(DataAccessException.class, () -> repository.insert(newUser("NEW@example.com", null)));
    }

    @Test
    void findCredentialsByEmail_seededAccounts_mapEachRoleToItsSubclass() {
        try (DatabaseManager seeded = DatabaseManager.forTestingWithSeedData()) {
            SqliteUserRepository users = new SqliteUserRepository(seeded);

            assertInstanceOf(Administrator.class, users.findCredentialsByEmail("admin@flyres.ca").orElseThrow().user());
            assertInstanceOf(FlightAgent.class, users.findCredentialsByEmail("agent@flyres.ca").orElseThrow().user());
            assertInstanceOf(Customer.class, users.findCredentialsByEmail("maya@example.com").orElseThrow().user());
        }
    }

    private static NewUser newUser(String email, String phone) {
        return new NewUser(Role.CUSTOMER, email, "New", "Customer", phone, HASH, CREATED);
    }
}
