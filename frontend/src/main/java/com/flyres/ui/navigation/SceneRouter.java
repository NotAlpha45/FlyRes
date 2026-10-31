package com.flyres.ui.navigation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Objects;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/** Swaps screens inside the single application {@link Stage}. */
public final class SceneRouter {

    private static final String STYLESHEET = "/com/flyres/ui/css/flyres.css";
    private static final double WIDTH = 1100;
    private static final double HEIGHT = 720;

    private final Scene scene;

    public SceneRouter(Stage stage) {
        this.scene = new Scene(new StackPane(), WIDTH, HEIGHT);
        scene.getStylesheets().add(resource(STYLESHEET).toExternalForm());
        stage.setScene(scene);
    }

    /** Replaces the current screen with the given one. */
    public void goTo(ViewId view) {
        try {
            Parent root = FXMLLoader.load(resource(view.resourcePath()));
            scene.setRoot(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load view " + view, e);
        }
    }

    private static URL resource(String path) {
        return Objects.requireNonNull(SceneRouter.class.getResource(path), () -> "Missing resource " + path);
    }
}
