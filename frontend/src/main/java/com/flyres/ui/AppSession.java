package com.flyres.ui;

import com.flyres.domain.user.User;
import java.util.Objects;
import java.util.Optional;

/** Who is logged in right now. One per application; cleared on logout. */
public final class AppSession {

    private User currentUser;

    /** Starts a session for {@code user}. */
    public void login(User user) {
        this.currentUser = Objects.requireNonNull(user);
    }

    /** Ends the session. */
    public void logout() {
        this.currentUser = null;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public Optional<User> currentUser() {
        return Optional.ofNullable(currentUser);
    }

    /** The logged-in user, to pass as the acting user to a control; fails if nobody is logged in. */
    public User requireUser() {
        if (currentUser == null) {
            throw new IllegalStateException("No user is logged in");
        }
        return currentUser;
    }
}
