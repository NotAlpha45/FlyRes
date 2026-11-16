package com.flyres.ui.view.auth;

import static com.flyres.control.AuthenticationController.CONFIRM_PASSWORD;
import static com.flyres.control.AuthenticationController.EMAIL;
import static com.flyres.control.AuthenticationController.FIRST_NAME;
import static com.flyres.control.AuthenticationController.LAST_NAME;
import static com.flyres.control.AuthenticationController.PASSWORD;
import static com.flyres.control.AuthenticationController.PHONE;

import com.flyres.common.exception.ValidationException;
import com.flyres.control.RegistrationRequest;
import com.flyres.domain.user.Customer;
import com.flyres.ui.component.FormValidator;
import com.flyres.ui.component.Toasts;
import com.flyres.ui.navigation.ViewContext;
import com.flyres.ui.navigation.ViewId;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/** &lt;&lt;boundary&gt;&gt; Customer self-registration, reached from the login screen. */
public class RegisterViewController {

    @FXML
    private TextField firstNameField;
    @FXML
    private Label firstNameError;
    @FXML
    private TextField lastNameField;
    @FXML
    private Label lastNameError;
    @FXML
    private TextField emailField;
    @FXML
    private Label emailError;
    @FXML
    private TextField phoneField;
    @FXML
    private Label phoneError;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label passwordError;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Label confirmPasswordError;
    @FXML
    private Button createButton;

    private final ViewContext context;
    private FormValidator form;

    public RegisterViewController(ViewContext context) {
        this.context = context;
    }

    @FXML
    private void initialize() {
        form = new FormValidator()
                .field(FIRST_NAME, firstNameField, firstNameError)
                .field(LAST_NAME, lastNameField, lastNameError)
                .field(EMAIL, emailField, emailError)
                .field(PHONE, phoneField, phoneError)
                .field(PASSWORD, passwordField, passwordError)
                .field(CONFIRM_PASSWORD, confirmPasswordField, confirmPasswordError);
        Platform.runLater(firstNameField::requestFocus);
    }

    @FXML
    private void onCreateAccount() {
        form.clearErrors();
        RegistrationRequest request = new RegistrationRequest(
                firstNameField.getText(),
                lastNameField.getText(),
                emailField.getText(),
                phoneField.getText(),
                passwordField.getText(),
                confirmPasswordField.getText());
        try {
            Customer customer = context.backend().authentication().registerCustomer(request);
            Toasts.success(createButton, "Account created. Log in to continue.");
            LoginViewController login = context.router().goTo(ViewId.LOGIN);
            login.prefillEmail(customer.getEmail());
        } catch (ValidationException e) {
            e.getFieldErrors().forEach(form::showError);
            form.focusFirstError();
        }
    }

    @FXML
    private void onBackToLogin() {
        context.router().goTo(ViewId.LOGIN);
    }
}
