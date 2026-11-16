package com.flyres.ui.navigation;

import com.flyres.domain.user.Role;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Every screen of the application: its title, its FXML file, whether it is shown inside the
 * app shell (sidebar and top bar) or fills the whole window, and which roles see it in the sidebar.
 * Hiding a sidebar entry is cosmetic; the controls enforce roles.
 */
public enum ViewId {

    LOGIN("Log in", "auth/LoginView.fxml", false),
    REGISTER("Create account", "auth/RegisterView.fxml", false),
    HOME("Home", "home/HomeView.fxml", true, Role.CUSTOMER, Role.AGENT, Role.ADMIN),
    COMPONENT_DEMO("Components", "demo/ComponentDemoView.fxml", true, Role.CUSTOMER, Role.AGENT, Role.ADMIN);

    private final String title;
    private final String fxml;
    private final boolean inShell;
    private final Set<Role> roles;

    ViewId(String title, String fxml, boolean inShell, Role... roles) {
        this.title = title;
        this.fxml = fxml;
        this.inShell = inShell;
        this.roles = roles.length == 0 ? EnumSet.noneOf(Role.class) : EnumSet.copyOf(Arrays.asList(roles));
    }

    public String title() {
        return title;
    }

    boolean inShell() {
        return inShell;
    }

    String resourcePath() {
        return "/com/flyres/ui/fxml/" + fxml;
    }

    /** The shell screens listed in the sidebar for {@code role}, in order. */
    static List<ViewId> sidebarItems(Role role) {
        return Arrays.stream(values())
                .filter(view -> view.inShell && view.roles.contains(role))
                .toList();
    }
}
