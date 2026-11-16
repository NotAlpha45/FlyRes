package com.flyres.domain.user;

import java.time.LocalDateTime;

/** A customer: books, changes and cancels their own flights. The customer is also the passenger. */
public final class Customer extends User {

    Customer(long id, String email, String firstName, String lastName, String phone, LocalDateTime createdAt) {
        super(id, email, firstName, lastName, phone, createdAt);
    }

    @Override
    public Role getRole() {
        return Role.CUSTOMER;
    }
}
