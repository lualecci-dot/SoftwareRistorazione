package myapp.controllers;

import java.io.IOException;

import javafx.fxml.FXML;
import myapp.App;

public abstract class ControllerAutenticato {
    @FXML
    public final void esci() throws IOException {
        App.esci();
    }
}