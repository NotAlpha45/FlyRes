package com.flyres.ui.component;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** Short, self-dismissing notifications shown in the bottom-right corner of the window. */
public final class Toasts {

    private static final String LAYER_KEY = Toasts.class.getName();
    private static final Duration FADE = Duration.millis(200);
    private static final Duration VISIBLE = Duration.seconds(3);

    private Toasts() {
    }

    /** Adds the toast layer on top of {@code root}. Called once by the router. */
    public static void install(StackPane root) {
        VBox layer = new VBox();
        layer.getStyleClass().add("toast-layer");
        layer.setMouseTransparent(true);
        root.getChildren().add(layer);
        root.getProperties().put(LAYER_KEY, layer);
    }

    /** Shows a success toast in the window that contains {@code source}. */
    public static void success(Node source, String message) {
        show(source, message, "toast-success");
    }

    /** Shows an error toast in the window that contains {@code source}. */
    public static void error(Node source, String message) {
        show(source, message, "toast-error");
    }

    private static void show(Node source, String message, String kind) {
        Pane layer = layerFor(source);
        Label toast = new Label(message);
        toast.getStyleClass().addAll("toast", kind);
        toast.setOpacity(0);
        layer.getChildren().add(toast);

        FadeTransition fadeIn = new FadeTransition(FADE, toast);
        fadeIn.setToValue(1);
        FadeTransition fadeOut = new FadeTransition(FADE, toast);
        fadeOut.setToValue(0);
        SequentialTransition lifecycle = new SequentialTransition(fadeIn, new PauseTransition(VISIBLE), fadeOut);
        lifecycle.setOnFinished(event -> layer.getChildren().remove(toast));
        lifecycle.play();
    }

    private static Pane layerFor(Node source) {
        Scene scene = source.getScene();
        if (scene == null) {
            throw new IllegalStateException("Cannot show a toast for a node that is not in a scene");
        }
        if (!(scene.getRoot().getProperties().get(LAYER_KEY) instanceof Pane layer)) {
            throw new IllegalStateException("Toasts.install was not called for this scene");
        }
        return layer;
    }
}
