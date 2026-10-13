package myapp.controllers.Metodi;

import java.io.IOException;
import java.sql.SQLException;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import myapp.controllers.Home;
import myapp.controllers.config.SessioneUtente;

public class SwichPage {

    public static void swichHometoData(Home home) throws SQLException, IOException {
        if (!SessioneUtente.isAmministratore()) {
            return;
        }
       FXMLLoader loader = null;
        try {
            loader = new FXMLLoader(Tavoli.class.getResource("/myapp/Database.fxml"));
            loader.load();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }

        Node soloHBox = (Node) loader.getNamespace().get("paneDatabase");
        System.out.println("Riferimento a paneDatabase: " + soloHBox);
        if (soloHBox != null) {
            home.anchorSwicth.getChildren().setAll(soloHBox);
            AnchorPane.setTopAnchor(soloHBox, 0.0);
            AnchorPane.setBottomAnchor(soloHBox, 0.0);
            AnchorPane.setLeftAnchor(soloHBox, 0.0);
            AnchorPane.setRightAnchor(soloHBox, 0.0);
        }
    }

     public static void swicthDatatoHome(Home home) throws SQLException, IOException {
          Tavoli.salvaSessioneTavolo(home);
        FXMLLoader loader = new FXMLLoader(home.getClass().getResource("/myapp/Home.fxml"));
        loader.load();

        Node soloHBox = (Node) loader.getNamespace().get("tabPane");
        System.out.println("Riferimento a tabPane: " + soloHBox);
        if (soloHBox != null) {
            home.anchorSwicth.getChildren().setAll(soloHBox);
            AnchorPane.setTopAnchor(soloHBox, 0.0);
            AnchorPane.setBottomAnchor(soloHBox, 0.0);
            AnchorPane.setLeftAnchor(soloHBox, 0.0);
            AnchorPane.setRightAnchor(soloHBox, 0.0);
        }
    }


}
