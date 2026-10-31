package com.flyres.ui.navigation;

import java.util.Arrays;
import java.util.List;

/**
 * Every screen of the application: its title, its FXML file and whether it is shown
 * inside the app shell (sidebar and top bar) or fills the whole window, like a login screen.
 */
public enum ViewId {

    HOME("Home", "home/HomeView.fxml", true),
    COMPONENT_DEMO("Components", "demo/ComponentDemoView.fxml", true);

    private final String title;
    private final String fxml;
    private final boolean inShell;

    ViewId(String title, String fxml, boolean inShell) {
        this.title = title;
        this.fxml = fxml;
        this.inShell = inShell;
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

    /** The screens listed in the sidebar, in order. */
    static List<ViewId> sidebarItems() {
        return Arrays.stream(values()).filter(ViewId::inShell).toList();
    }
}
