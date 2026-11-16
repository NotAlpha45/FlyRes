package com.flyres.ui.view.auth;

import static com.flyres.control.AuthenticationController.EMAIL;
import static com.flyres.control.AuthenticationController.PASSWORD;

import com.flyres.common.exception.ValidationException;
import com.flyres.domain.user.User;
import com.flyres.ui.component.FormValidator;
import com.flyres.ui.navigation.ViewContext;
import com.flyres.ui.navigation.ViewId;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/** &lt;&lt;boundary&gt;&gt; Login screen: email and password, with a link to self-registration. */
public class LoginViewController {

    @FXML
    private TextField emailField;
    @FXML
    private Label emailError;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label passwordError;

    private final ViewContext context;
    private FormValidator form;

    public LoginViewController(ViewContext context) {
        this.context = context;
    }

    @FXML
    private void initialize() {
        form = new FormValidator()
                .field(EMAIL, emailField, emailError)
                .field(PASSWORD, passwordField, passwordError);
        Platform.runLater(emailField::requestFocus);
    }

    /** Fills in the email, for example right after registering, and moves focus to the password. */
    public void prefillEmail(String email) {
        emailField.setText(email);
        Platform.runLater(passwordField::requestFocus);
    }

    @FXML
    private void onLogin() {
        form.clearErrors();
        try {
            User user = context.backend().authentication().login(emailField.getText(), passwordField.getText());
            context.router().startSession(user);
        } catch (ValidationException e) {
            e.getFieldErrors().forEach(form::showError);
            form.focusFirstError();
        }
    }

    @FXML
    private void onCreateAccount() {
        context.router().goTo(ViewId.REGISTER);
    }
}
