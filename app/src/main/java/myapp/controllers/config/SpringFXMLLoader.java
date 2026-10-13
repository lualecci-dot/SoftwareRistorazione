package myapp.controllers.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;


@Component
public class SpringFXMLLoader {
    private final ApplicationContext context;
     
    
    @Autowired
    public SpringFXMLLoader(ApplicationContext context) {
        this.context = context;
    }

    public Parent load(String fxmlName) throws IOException {
        // Fornisce i controller al contesto Spring, necessario per @Autowired nei controller FXML.
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlName));
        loader.setControllerFactory(context::getBean); // Spring inietta le dipendenze nei controller
       //loader.setControllerFactory(type -> context.getBean(type)); 
        //FXMLLoader loader = new FXMLLoader();
        //loader.setControllerFactory(context::getBean);
        loader.setLocation(getClass().getResource(fxmlName));
        loader.setClassLoader(context.getClassLoader());
        return loader.load();
    }
}

