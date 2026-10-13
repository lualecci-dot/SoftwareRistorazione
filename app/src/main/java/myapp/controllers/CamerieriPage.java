package myapp.controllers;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import myapp.App;
import myapp.controllers.Service.CameriereService;
import myapp.controllers.config.FxmlView;
import myapp.controllers.config.SessioneUtente;

@Component
@Scope("prototype")
public class CamerieriPage extends ControllerAutenticato {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private ListView<String> listaCamerieri;
    @FXML
    private Label usernameLabel;

    @Autowired
    private CameriereService cameriereService;

    @FXML
    private void initialize() {
        SessioneUtente.richiediAmministratore();
        usernameLabel.setText(SessioneUtente.getUsername());
        aggiornaLista();
    }

    @FXML
    private void aggiungiCameriere() {
        try {
            if (!cameriereService.aggiungiCameriere(usernameField.getText(), passwordField.getText())) {
                mostraAvviso("Esiste già un cameriere con questo username.");
                return;
            }
            usernameField.clear();
            passwordField.clear();
            aggiornaLista();
        } catch (IllegalArgumentException errore) {
            mostraAvviso(errore.getMessage());
        }
    }

    @FXML
    private void eliminaCameriere() {
        String selezionato = listaCamerieri.getSelectionModel().getSelectedItem();
        if (selezionato == null) {
            mostraAvviso("Seleziona un cameriere da eliminare.");
            return;
        }
        cameriereService.eliminaCameriere(selezionato);
        aggiornaLista();
    }

    @FXML
    private void switchToHome() throws IOException {
        App.switchToNextScene(FxmlView.HOMEPAGE);
    }

    @FXML
    private void switchToDatabase() throws IOException {
        App.switchToNextScene(FxmlView.DATABASEPAGE);
    }

    @FXML
    private void aggiornaLista() {
        listaCamerieri.setItems(FXCollections.observableArrayList(cameriereService.elencoCamerieri()));
    }

    private void mostraAvviso(String messaggio) {
        new Alert(Alert.AlertType.WARNING, messaggio).showAndWait();
    }
}