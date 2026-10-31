package com.flyres.ui.navigation;

import com.flyres.ui.component.Toasts;
import com.flyres.ui.view.shell.AppShellViewController;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Objects;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Swaps screens inside the single application {@link Stage}. Shell screens are placed in the
 * content area of the app shell; full-window screens replace the shell entirely.
 */
public final class SceneRouter {

    private static final String STYLESHEET = "/com/flyres/ui/css/flyres.css";
    private static final String SHELL_FXML = "/com/flyres/ui/fxml/shell/AppShell.fxml";
    private static final double WIDTH = 1100;
    private static final double HEIGHT = 720;
    private static final double MIN_WIDTH = 900;
    private static final double MIN_HEIGHT = 600;

    private final StackPane page = new StackPane();
    private final Parent shellView;
    private final AppShellViewController shell;

    public SceneRouter(Stage stage) {
        FXMLLoader shellLoader = loader(SHELL_FXML);
        this.shellView = load(shellLoader);
        this.shell = shellLoader.getController();
        shell.setNavigation(ViewId.sidebarItems(), this::goTo);

        StackPane root = new StackPane(page);
        Toasts.install(root);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        scene.getStylesheets().add(resource(STYLESHEET).toExternalForm());
        stage.setScene(scene);
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
    }

    /** Replaces the current screen with the given one. */
    public void goTo(ViewId view) {
        Parent content = load(loader(view.resourcePath()));
        if (view.inShell()) {
            shell.show(view, content);
            page.getChildren().setAll(shellView);
        } else {
            page.getChildren().setAll(content);
        }
    }

    private static FXMLLoader loader(String path) {
        return new FXMLLoader(resource(path));
    }

    private static Parent load(FXMLLoader loader) {
        try {
            return loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load view " + loader.getLocation(), e);
        }
    }

    private static URL resource(String path) {
        return Objects.requireNonNull(SceneRouter.class.getResource(path), () -> "Missing resource " + path);
    }
}
