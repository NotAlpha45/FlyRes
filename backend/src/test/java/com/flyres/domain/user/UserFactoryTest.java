package com.flyres.domain.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class UserFactoryTest {

    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 1, 1, 9, 0);

    @Test
    void create_customerRole_returnsCustomer() {
        assertInstanceOf(Customer.class, create(Role.CUSTOMER));
    }

    @Test
    void create_agentRole_returnsFlightAgent() {
        assertInstanceOf(FlightAgent.class, create(Role.AGENT));
    }

    @Test
    void create_adminRole_returnsAdministrator() {
        assertInstanceOf(Administrator.class, create(Role.ADMIN));
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void create_anyRole_userReportsThatRole(Role role) {
        User user = create(role);

        assertEquals(role, user.getRole());
        assertTrue(user.hasRole(role));
    }

    @Test
    void create_withoutPhone_hasEmptyPhoneAndFullName() {
        User user = create(Role.CUSTOMER);

        assertTrue(user.getPhone().isEmpty());
        assertEquals("Maya Patel", user.getFullName());
    }

    private static User create(Role role) {
        return UserFactory.create(role, 7, "maya@example.com", "Maya", "Patel", null, CREATED);
    }
}
