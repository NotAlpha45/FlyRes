package com.flyres.ui;

import atlantafx.base.theme.PrimerLight;
import com.flyres.FlyResBackend;
import com.flyres.ui.component.ErrorReporter;
import com.flyres.ui.navigation.SceneRouter;
import com.flyres.ui.navigation.ViewId;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * JavaFX application: starts the backend, applies the theme and shows the login screen.
 * If the backend cannot start (for example, the database cannot be opened), it shows an error
 * dialog and exits instead of crashing.
 */
public class FlyResApp extends Application {

    private FlyResBackend backend;
    private RuntimeException startupFailure;

    @Override
    public void init() {
        try {
            backend = FlyResBackend.create();
        } catch (RuntimeException e) {
            // init() runs before the toolkit can show dialogs; start() reports it.
            startupFailure = e;
        }
    }

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        if (startupFailure != null) {
            ErrorReporter.showStartupFailure(startupFailure);
            Platform.exit();
            return;
        }

        ErrorReporter.install(stage);

        SceneRouter router = new SceneRouter(stage, backend, new AppSession());
        router.goTo(ViewId.LOGIN);

        stage.setTitle("FlyRes");
        stage.show();
    }

    @Override
    public void stop() {
        if (backend != null) {
            backend.close();
        }
    }
}
