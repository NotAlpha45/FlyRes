package com.flyres.ui.navigation;

/** Every screen of the application and the FXML file that defines it. */
public enum ViewId {

    HOME("home/HomeView.fxml");

    private final String fxml;

    ViewId(String fxml) {
        this.fxml = fxml;
    }

    String resourcePath() {
        return "/com/flyres/ui/fxml/" + fxml;
    }
}
