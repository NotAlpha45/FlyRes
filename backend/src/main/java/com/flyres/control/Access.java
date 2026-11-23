package com.flyres.control;

import com.flyres.common.exception.AccessDeniedException;
import com.flyres.domain.user.Role;
import com.flyres.domain.user.User;
import java.util.Arrays;

/** Role checks shared by every control. Hiding a button is cosmetic; these checks are the real rule. */
final class Access {

    private Access() {
    }

    /** Throws {@link AccessDeniedException} unless {@code actor} has one of {@code allowed}. */
    static void require(User actor, String action, Role... allowed) {
        if (actor == null || Arrays.stream(allowed).noneMatch(actor::hasRole)) {
            throw new AccessDeniedException("You don't have permission to " + action + ".");
        }
    }
}
