package com.flyres.ui.navigation;

import com.flyres.FlyResBackend;
import com.flyres.ui.AppSession;

/**
 * What a view controller needs from the rest of the app. {@link SceneRouter} passes it to every
 * {@code *ViewController} that declares a public constructor taking a {@code ViewContext}.
 */
public record ViewContext(FlyResBackend backend, AppSession session, SceneRouter router) {
}
