package com.flyres.ui.navigation;

import com.flyres.FlyResBackend;
import com.flyres.domain.user.User;
import com.flyres.ui.AppSession;
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
 * content area of the app shell; full-window screens, such as login, replace the shell entirely.
 * <p>
 * Every FXML controller is created here: one with a public {@code (ViewContext)} constructor receives
 * the backend, the session and this router; otherwise its no-argument constructor is used.
 */
public final class SceneRouter {

    private static final String STYLESHEET = "/com/flyres/ui/css/flyres.css";
    private static final String SHELL_FXML = "/com/flyres/ui/fxml/shell/AppShell.fxml";
    private static final double WIDTH = 1100;
    private static final double HEIGHT = 720;
    private static final double MIN_WIDTH = 900;
    private static final double MIN_HEIGHT = 600;

    private final AppSession session;
    private final ViewContext context;
    private final StackPane page = new StackPane();
    private final Parent shellView;
    private final AppShellViewController shell;

    public SceneRouter(Stage stage, FlyResBackend backend, AppSession session) {
        this.session = Objects.requireNonNull(session);
        this.context = new ViewContext(Objects.requireNonNull(backend), session, this);

        FXMLLoader shellLoader = loader(SHELL_FXML);
        this.shellView = load(shellLoader);
        this.shell = shellLoader.getController();
        shell.setOnLogout(this::logout);

        StackPane root = new StackPane(page);
        Toasts.install(root);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        scene.getStylesheets().add(resource(STYLESHEET).toExternalForm());
        stage.setScene(scene);
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
    }

    /**
     * Replaces the current screen with {@code view} and returns its FXML controller.
     * A shell screen requested while nobody is logged in shows the login screen instead.
     */
    public <C> C goTo(ViewId view) {
        ViewId target = view.inShell() && !session.isLoggedIn() ? ViewId.LOGIN : view;
        FXMLLoader loader = loader(target.resourcePath());
        Parent content = load(loader);
        if (target.inShell()) {
            shell.show(target, content);
            page.getChildren().setAll(shellView);
        } else {
            page.getChildren().setAll(content);
        }
        return loader.getController();
    }

    /** Starts a session for {@code user}: fills the shell for their role and opens the home screen. */
    public void startSession(User user) {
        session.login(user);
        shell.setUser(user.getFullName(), user.getRole().displayName());
        shell.setNavigation(ViewId.sidebarItems(user.getRole()), this::goTo);
        goTo(ViewId.HOME);
    }

    /** Ends the session and returns to the login screen. */
    public void logout() {
        session.logout();
        goTo(ViewId.LOGIN);
    }

    private FXMLLoader loader(String path) {
        FXMLLoader loader = new FXMLLoader(resource(path));
        loader.setControllerFactory(this::createController);
        return loader;
    }

    private Object createController(Class<?> type) {
        try {
            try {
                return type.getConstructor(ViewContext.class).newInstance(context);
            } catch (NoSuchMethodException noContextConstructor) {
                return type.getConstructor().newInstance();
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not create view controller " + type.getName(), e);
        }
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
