package com.flyres.ui.component;

import java.lang.System.Logger.Level;
import java.util.Objects;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

/**
 * Safety net for errors that no screen expected, such as a locked or unreadable database.
 * <p>
 * Screens still catch the business exceptions they expect ({@code ValidationException},
 * {@code BookingException}, {@code AccessDeniedException}, {@code NotFoundException}) and show
 * them inline or as a specific toast. Anything else reaches the handler installed here, which logs
 * the stack trace and shows the user a generic message. Exception text, SQL and file paths are
 * never shown in the UI.
 */
public final class ErrorReporter {

    /** Shown for any unexpected error after the window is up. */
    public static final String UNEXPECTED_ERROR =
            "Something went wrong and your last action was not saved. Please try again.";

    static final String STARTUP_TITLE = "FlyRes could not start";
    static final String STARTUP_MESSAGE =
            "FlyRes could not open its database. Close any other program that is using it and try again. "
            + "If the problem continues, reset the database by deleting the .flyres folder in your home folder.";

    private static final System.Logger LOG = System.getLogger(ErrorReporter.class.getName());

    private ErrorReporter() {
    }

    /**
     * Reports every uncaught exception in {@code stage}: on the JavaFX thread (event handlers) and,
     * as the default handler, on any other thread. Call once from {@code Application.start}.
     */
    public static void install(Stage stage) {
        Objects.requireNonNull(stage);
        Thread.UncaughtExceptionHandler handler = (thread, error) -> report(stage, error);
        Thread.currentThread().setUncaughtExceptionHandler(handler);
        Thread.setDefaultUncaughtExceptionHandler(handler);
    }

    /** Logs a failure to start the backend and tells the user in a dialog. The caller then exits. */
    public static void showStartupFailure(Throwable error) {
        LOG.log(Level.ERROR, "FlyRes could not start", error);
        showDialog(STARTUP_TITLE, STARTUP_MESSAGE);
    }

    private static void report(Stage stage, Throwable error) {
        LOG.log(Level.ERROR, "Unexpected error", error);
        try {
            if (Platform.isFxApplicationThread()) {
                notifyUser(stage);
            } else {
                Platform.runLater(() -> notifyUser(stage));
            }
        } catch (RuntimeException toolkitGone) {
            // The JavaFX toolkit has already shut down; the error is logged above.
        }
    }

    private static void notifyUser(Stage stage) {
        try {
            Scene scene = stage.getScene();
            if (scene != null && stage.isShowing()) {
                Toasts.error(scene.getRoot(), UNEXPECTED_ERROR);
            } else {
                showDialog("Something went wrong", UNEXPECTED_ERROR);
            }
        } catch (RuntimeException failure) {
            // Never let the safety net throw again: that would loop back into this handler.
            LOG.log(Level.ERROR, "Could not show an error to the user", failure);
        }
    }

    private static void showDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.CLOSE);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.showAndWait();
    }
}
