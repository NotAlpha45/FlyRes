package com.flyres.ui;

import atlantafx.base.theme.PrimerLight;
import com.flyres.FlyResBackend;
import com.flyres.ui.navigation.SceneRouter;
import com.flyres.ui.navigation.ViewId;
import javafx.application.Application;
import javafx.stage.Stage;

/** JavaFX application: starts the backend, applies the theme and shows the first screen. */
public class FlyResApp extends Application {

    private FlyResBackend backend;

    @Override
    public void init() {
        backend = FlyResBackend.create();
    }

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        SceneRouter router = new SceneRouter(stage);
        router.goTo(ViewId.HOME);

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
