package myapp.controllers.config;

import java.io.IOException;

import org.springframework.stereotype.Component;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


@Component
public class stateManager {

    // Gestisce il cambio delle schermate mantenendo un'unica finestra principale.


    private  Stage primaryStage;
    private  SpringFXMLLoader springFXMLLoader;

    public void StageManager(SpringFXMLLoader springFXMLLoader, Stage stage) {
        this.springFXMLLoader = springFXMLLoader;
        this.primaryStage = stage;
    }

    public void switchScene(final FxmlView view) throws IOException {
        // Risolve il file FXML, crea la vista e aggiorna titolo e contenuto della finestra.
        Parent viewRootNode = springFXMLLoader.load(view.getFxmlName());
        show(viewRootNode, view.getTitle());
    }

    private void show(Parent rootNode, String title) {
        Scene scene = primaryStage.getScene();
        if (scene == null) {
            scene = new Scene(rootNode);
        } else {
            scene.setRoot(rootNode); // Più efficiente che creare una nuova Scene
        }
        primaryStage.setTitle(title);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}


