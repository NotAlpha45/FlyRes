package com.flyres.repository;

import com.flyres.domain.user.User;
import java.util.Optional;

/** Stores users of every role in one table. Email lookups ignore case. */
public interface UserRepository {

    /** Finds the user with this id. */
    Optional<User> findById(long id);

    /** Finds the user with this email, together with their stored password hash, for login only. */
    Optional<UserCredentials> findCredentialsByEmail(String email);

    /** Returns true if any user already has this email. */
    boolean existsByEmail(String email);

    /** Inserts a new user and returns it with its generated id. */
    User insert(NewUser user);
}
