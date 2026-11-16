package com.flyres.ui.view.shell;

import atlantafx.base.theme.Styles;
import com.flyres.ui.navigation.ViewId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** The frame around every shell screen: sidebar navigation, top bar with the user, and content area. */
public class AppShellViewController {

    private static final String ACTIVE = "active";

    @FXML
    private VBox navigation;
    @FXML
    private Label pageTitle;
    @FXML
    private Label userName;
    @FXML
    private Label userRole;
    @FXML
    private StackPane content;

    private final Map<ViewId, Button> navigationButtons = new EnumMap<>(ViewId.class);
    private Runnable onLogout = () -> { };

    /** Fills the sidebar with one entry per view; selecting an entry calls {@code onSelect}. */
    public void setNavigation(List<ViewId> views, Consumer<ViewId> onSelect) {
        navigation.getChildren().clear();
        navigationButtons.clear();
        for (ViewId view : views) {
            Button button = new Button(view.title());
            button.getStyleClass().addAll(Styles.FLAT, "sidebar-item");
            button.setMaxWidth(Double.MAX_VALUE);
            button.setOnAction(event -> onSelect.accept(view));
            navigationButtons.put(view, button);
            navigation.getChildren().add(button);
        }
    }

    /** Shows who is logged in, in the top bar. */
    public void setUser(String name, String role) {
        userName.setText(name);
        userRole.setText(role);
    }

    /** Sets what the Log out button does. */
    public void setOnLogout(Runnable onLogout) {
        this.onLogout = Objects.requireNonNull(onLogout);
    }

    /** Shows {@code page} in the content area and marks {@code view} as the current screen. */
    public void show(ViewId view, Node page) {
        pageTitle.setText(view.title());
        content.getChildren().setAll(page);
        navigationButtons.forEach((id, button) -> {
            button.getStyleClass().remove(ACTIVE);
            if (id == view) {
                button.getStyleClass().add(ACTIVE);
            }
        });
    }

    @FXML
    private void onLogout() {
        onLogout.run();
    }
}
