package myapp;

import java.io.IOException;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import myapp.controllers.config.FxmlView;
import myapp.controllers.config.SessioneUtente;

@Component
public class App extends Application {
    public static ConfigurableApplicationContext applicationContext;
    private static HostServices hostServices;

    private static Scene scene;
   
    @Override

    public void init() throws Exception {
        // Avvia Spring prima della finestra, così FXMLLoader può iniettare i bean nei controller.
        applicationContext =new SpringApplicationBuilder(AppApplication.class).run();
    }
    
    @Override
    
    public void start(Stage stage) throws IOException, Exception { 
        hostServices = getHostServices();
        // Carica la schermata iniziale usando Spring per creare il relativo controller.
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/myapp/Login.fxml"));
        fxmlLoader.setControllerFactory(applicationContext::getBean);
        Parent root = fxmlLoader.load();
        scene = new Scene(root);
        stage.setTitle("MyApp");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setWidth(1200);
        stage.setHeight(760);
         scene.getStylesheets().add(getClass().getResource("/css/styleChiaro.css").toExternalForm());
        
        stage.setScene(scene);
        stage.show();
    }

    @Override   
    public void stop() throws Exception {
        applicationContext.close();
    }

    public static void setRoot(String string) {
        try {
            String fxml = string.startsWith("/") ? string : "/myapp/" + string + ".fxml";
            scene.setRoot(loadFXML(fxml));
        } catch (IOException e) {
            throw new IllegalStateException("Impossibile caricare la schermata " + string, e);
        }
    }

    public static void switchToNextScene(FxmlView fxmlView) throws IOException {
        if ((fxmlView == FxmlView.DATABASEPAGE || fxmlView == FxmlView.CREADATABASE
                || fxmlView == FxmlView.CAMERIERI)
                && !SessioneUtente.isAmministratore()) {
            throw new SecurityException("L'accesso è riservato all'amministratore.");
        }
        // Sostituisce il contenuto della scena mantenendo la finestra JavaFX esistente.
        scene.setRoot(loadFXML(fxmlView.getFxmlName()));
    }

    public static void esci() throws IOException {
        SessioneUtente.termina();
        switchToNextScene(FxmlView.LOGIN);
    }

    public static void apriLinkEsterno(String indirizzo) {
        if (hostServices == null) {
            throw new IllegalStateException("Il browser può essere aperto solo dopo l'avvio dell'applicazione.");
        }
        hostServices.showDocument(indirizzo);
    }
   
    private static Parent loadFXML(String fxml) throws IOException {
        // Tutti i controller FXML vengono recuperati dal contesto Spring.
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml));
        fxmlLoader.setControllerFactory(applicationContext::getBean);
        return fxmlLoader.load();
    }
   
    




   
  
   

  /*   static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }*/

}

        
        
        
        