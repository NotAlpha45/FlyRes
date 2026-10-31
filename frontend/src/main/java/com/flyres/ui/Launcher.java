package com.flyres.ui;

import javafx.application.Application;

/**
 * Main entry point. Kept separate from {@link FlyResApp} so the fat JAR starts
 * with JavaFX on the classpath instead of the module path.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Application.launch(FlyResApp.class, args);
    }
}
