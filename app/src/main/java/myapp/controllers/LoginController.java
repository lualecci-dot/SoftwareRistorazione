package myapp.controllers;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import myapp.App;
import myapp.controllers.Service.AutenticazioneService;
import myapp.controllers.config.FxmlView;
import myapp.controllers.config.RuoloUtente;
import myapp.controllers.config.SessioneUtente;

@Component
@Scope("prototype")
public class LoginController {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label messaggioErrore;

    @Autowired
    private AutenticazioneService autenticazioneService;

    @FXML
    private void accedi() {
        String username = usernameField.getText();
        RuoloUtente ruolo = autenticazioneService.autenticaDesktop(username, passwordField.getText());

        if (ruolo == null) {
            messaggioErrore.setText("Username o password non corretti.");
            messaggioErrore.setVisible(true);
            messaggioErrore.setManaged(true);
            passwordField.clear();
            return;
        }

        SessioneUtente.avvia(username.trim(), ruolo);
        try {
            App.switchToNextScene(FxmlView.HOMEPAGE);
        } catch (IOException errore) {
            messaggioErrore.setText("Impossibile aprire la cassa: " + errore.getMessage());
            messaggioErrore.setVisible(true);
            messaggioErrore.setManaged(true);
        }
    }
}