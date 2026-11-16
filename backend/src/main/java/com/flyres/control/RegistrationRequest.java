package com.flyres.control;

/**
 * What a customer types on the registration form, unvalidated. {@code phone} is optional.
 * {@link AuthenticationController#registerCustomer} checks every field.
 */
public record RegistrationRequest(String firstName, String lastName, String email, String phone,
                                  String password, String confirmPassword) {

    @Override
    public String toString() {
        return "RegistrationRequest[email=" + email + "]";
    }
}
