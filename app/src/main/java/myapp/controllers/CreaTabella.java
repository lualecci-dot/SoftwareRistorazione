package myapp.controllers;

import java.io.IOException;
import java.sql.SQLException;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import myapp.App;
import myapp.controllers.Service.DatabaseService;
import myapp.controllers.config.FxmlView;
import myapp.controllers.config.SpringContextGetter;

public class CreaTabella {
  // Controller del modulo JavaFX che raccoglie il nome del nuovo archivio.

        @FXML
        private TextField fieldCognome;     
          @FXML
        private TextField fieldNome; 
          @FXML
        private TextField fieldNomedatabase; 




    @FXML
    private void creaDatabase() throws SQLException, IOException {
      // Valida il nome e delega al servizio la creazione della tabella PostgreSQL.
     
      try {
        // 1. Recupera i nomi dai campi di testo
      String nomeTabella = fieldNomedatabase.getText() == null ? "" : fieldNomedatabase.getText().trim();
      String nomeColonnaProdotto = fieldNome.getText() == null ? "" : fieldNome.getText().trim();
      String nomeColonnaPrezzo = fieldCognome.getText() == null ? "" : fieldCognome.getText().trim();

        // Validazione minima
        if (nomeTabella.isEmpty()) {
            System.err.println("Tutti i campi sono obbligatori!");
            return;
        }

        // 2. Recupera il Service e crea la tabella
        DatabaseService dbService = SpringContextGetter.getBean(DatabaseService.class);
      dbService.creaNuovaTabella(nomeTabella, nomeColonnaProdotto, nomeColonnaPrezzo);

        System.out.println("✅ Tabella " + nomeTabella + " creata con successo!");

        // 3. Torna alla pagina del database
        App.switchToNextScene(FxmlView.DATABASEPAGE);

    } catch (Exception e) {
        System.err.println("❌ Errore durante la creazione della tabella: " + e.getMessage());
        // Qui potresti mostrare un Alert di errore
    }
     
     
     
     
      /*   System.out.println("creazione tabella in corso");
        String url = "jdbc:sqlite:mydatabase.db";
       Connection connection = DriverManager.getConnection(url);
       System.out.println("connessione al database stabilita");
         String nomeProdotto = fieldNome.getText();
         System.out.println(nomeProdotto);
        String prezzo = fieldCognome.getText();
         String nomeDatabase = fieldNomedatabase.getText();
         
         System.out.println(nomeProdotto+prezzo+nomeDatabase);
        
        String sql= "create table " + nomeDatabase + " ("+nomeProdotto+" varchar(20), "+prezzo+" float)";
           Statement statement= connection.createStatement();
           statement.executeUpdate(sql);
            
            System.out.println("tabella creata  "+nomeProdotto+"  "+prezzo+"  "+nomeDatabase);
        
        
        
        
        // Dopo la creazione, torna alla pagina del database
        App.setRoot("Database");
    }*/
    }
}
