package myapp.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import myapp.App;
import myapp.controllers.Service.DatabaseService;
import myapp.controllers.Service.Prodotto;
import myapp.controllers.Service.ProdottoService;
import myapp.controllers.config.DatabaseConfig;
import myapp.controllers.config.FxmlView;
import myapp.controllers.config.SpringContextGetter;

@Controller
@Component
@Scope("prototype")
public class DatabasePage extends ControllerAutenticato {
    // Controller della vista archivio: gestisce tabelle e prodotti.
    
        @FXML
        public TableView<Prodotto> tableDatabase;
        @FXML
        public TableColumn<Prodotto, Integer> colId;
        @FXML
        public TableColumn<Prodotto, String> colNome;
        @FXML
        public TableColumn<Prodotto, String> colCognome;
         public String url = DatabaseConfig.URL;
        public String user = DatabaseConfig.USERNAME;
        public String password = DatabaseConfig.PASSWORD;
      @Autowired
    private ProdottoService prodottoService;
    @FXML
    private void switchToHome() throws IOException {
        App.switchToNextScene(FxmlView.HOMEPAGE);
    }

      @FXML
    private void switchToCreaDatabase() {
        // Apre una finestra modale per creare una tabella e aggiorna il menu al completamento.
        Stage finestra = new Stage();
        finestra.initModality(Modality.WINDOW_MODAL);
        Window finestraPrincipale = menuButton.getScene().getWindow();
        finestra.initOwner(finestraPrincipale);
        finestra.setTitle("Nuova tabella");
        finestra.setResizable(false);

        Label titolo = new Label("Nuova tabella");
        titolo.getStyleClass().add("section-title");

        Button chiudi = new Button("X");
        chiudi.getStyleClass().add("secondary-button");
        chiudi.setOnAction(event -> finestra.close());

        Region spazio = new Region();
        HBox.setHgrow(spazio, Priority.ALWAYS);
        HBox barraTitolo = new HBox(10, titolo, spazio, chiudi);

        TextField nomeTabella = new TextField();
        nomeTabella.setPromptText("Nome della tabella");

        Label messaggio = new Label("Usa solo lettere, numeri e underscore.");
        messaggio.getStyleClass().add("muted-label");

        Button conferma = new Button("Conferma");
        conferma.getStyleClass().add("primary-button");
        conferma.setMaxWidth(Double.MAX_VALUE);
        conferma.setOnAction(event -> {
            String nome = nomeTabella.getText() == null ? "" : nomeTabella.getText().trim();
            if (!nome.matches("[A-Za-z_][A-Za-z0-9_]*")) {
                messaggio.setText("Nome non valido: usa lettere, numeri e underscore.");
                return;
            }

            try {
                DatabaseService databaseService = SpringContextGetter.getBean(DatabaseService.class);
                databaseService.creaNuovaTabella(nome, "nome", "prezzo");
                popolaMenuTabelle();
                menuButton.setText(nome);
                finestra.close();
            } catch (Exception errore) {
                messaggio.setText("Errore: " + errore.getMessage());
            }
        });

        VBox contenuto = new VBox(16, barraTitolo, nomeTabella, messaggio, conferma);
        contenuto.getStyleClass().add("surface-panel");
        contenuto.setPrefWidth(360);
        contenuto.setPadding(new javafx.geometry.Insets(20));

        finestra.setScene(new Scene(contenuto));
        finestra.show();
        nomeTabella.requestFocus();
    }
       

    @FXML
    private MenuButton menuButton;
    @FXML
     private void popolaMenuTabelle() {
            // Ricostruisce il menu leggendo i nomi delle tabelle pubbliche da PostgreSQL.
        
    String url = DatabaseConfig.URL;
    String user = DatabaseConfig.USERNAME;
    String password = DatabaseConfig.PASSWORD;
    
    // Query per ottenere solo i nomi delle tabelle create dall'utente
    String sql = "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'";

    try (Connection conn = DriverManager.getConnection(url, user, password);
         Statement st = conn.createStatement();
         ResultSet rs = st.executeQuery(sql)) {

        // 1. Pulisci il menu esistente
        menuButton.getItems().clear();

        while (rs.next()) {
            String tableName = rs.getString("table_name");
            if (!DatabaseService.isTabellaProdotto(tableName)) {
                continue;
            }

            // 2. Crea un nuovo MenuItem per ogni tabella
            MenuItem item = new MenuItem(tableName);
            
            // 3. Imposta l'azione quando viene selezionata la tabella
            item.setOnAction(event -> {
                // Aggiorna il testo del MenuButton con la scelta effettuata
                menuButton.setText(tableName);
                
                // Chiama il tuo metodo per caricare i dati (quello che abbiamo visto prima)
              //  caricaDatiTabella(tableName);
              //leggiDatabase(tableName);
            });

            // 4. Aggiungi l'item al MenuButton
            menuButton.getItems().add(item);
        }

    } catch (SQLException e) {
        System.err.println("Errore nel caricamento del menu tabelle: " + e.getMessage());
    }
}
   
@FXML
public void initialize() {
    // Collega le colonne della tabella al modello e carica gli archivi disponibili.
   // colId.setCellValueFactory(new PropertyValueFactory<>("id"));
    colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
    colCognome.setCellValueFactory(new PropertyValueFactory<>("prezzo"));
    this.prodottoService = SpringContextGetter.getBean(ProdottoService.class);
       
       if (this.prodottoService == null) {
           System.err.println("ERRORE: SpringContextGetter ha restituito un service NULL!");
       } else {
           //aggiornaTabella(); // Carica i dati all'avvio
       }
     // Recupera manualmente il bean dal contesto di Spring
    popolaMenuTabelle(); // Carica dinamicamente le tabelle nel menu all'avvio della pagina
}
    

    @FXML
    
private void eliminaButton(ActionEvent event)  throws SQLException {
     Prodotto prodottoSelezionato = tableDatabase.getSelectionModel().getSelectedItem();

    if (prodottoSelezionato != null) {
        // 2. Chiamata al tuo Service (passando il nome, non l'ID)
        prodottoService.deletePerNome(prodottoSelezionato.getNome());

        // 3. Aggiorna la vista
        System.out.println("Eliminato correttamente: " + prodottoSelezionato.getNome());
        aggiornaTabella(menuButton.getText());// Ricarica i dati dopo l'eliminazione
    } else {
        System.out.println("Errore: Seleziona prima un prodotto dalla tabella!");
    }
}

    @FXML
    private void eliminaTabella(ActionEvent event) {
        // Richiede conferma perché la cancellazione rimuove in modo permanente l'intera tabella.
        String nomeTabella = menuButton.getText();
        if (nomeTabella == null || nomeTabella.isBlank() || nomeTabella.equals("Seleziona Archivio")) {
            mostraMessaggio("Seleziona prima la tabella da eliminare.", Alert.AlertType.WARNING);
            return;
        }

        Alert conferma = new Alert(Alert.AlertType.CONFIRMATION,
                "Vuoi eliminare definitivamente la tabella '" + nomeTabella + "'?",
                ButtonType.CANCEL, ButtonType.OK);
        conferma.setTitle("Elimina tabella");
        conferma.setHeaderText("Operazione irreversibile");

        if (conferma.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            prodottoService.eliminaTabella(nomeTabella);
            tableDatabase.getItems().clear();
            menuButton.setText("Seleziona Archivio");
            popolaMenuTabelle();
            mostraMessaggio("Tabella eliminata correttamente.", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            System.err.println("Errore durante l'eliminazione della tabella: " + e.getMessage());
            mostraMessaggio("Impossibile eliminare la tabella: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void mostraMessaggio(String testo, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo, testo, ButtonType.OK);
        alert.showAndWait();
    }
@FXML
public String nomeTabella; 

@FXML
private void aggiornaTabella(String nomeTabella) {
        // Carica dal service le righe dell'archivio scelto e le mostra nella TableView.
    // Sfrutta il metodo findAll() del tuo Service
    try {
            List<Prodotto> dati = prodottoService.trovaDaTabella(nomeTabella);
            tableDatabase.setItems(FXCollections.observableArrayList(dati));
            System.out.println("Caricate " + dati.size() + " righe da " + nomeTabella);
        } catch (Exception e) {
            System.err.println("Errore nel caricamento: " + e.getMessage());
        }
    }


    
   

    @FXML
    public void leggiDatabase(ActionEvent event ) throws SQLException {
            // L'azione del pulsante delega il caricamento al service usando la selezione corrente.
        //svuota tabella
      tableDatabase.getItems().clear();
      String nomeTabella = menuButton.getText();
      aggiornaTabella(nomeTabella);

   /*  System.out.println("Tabella selezionata: " + nomeTabella);

    // Utilizzo del try-with-resources per gestire la chiusura automatica
    try (Connection connection = DriverManager.getConnection(url, user, password);
    
         Statement statement = connection.createStatement();
         
         ResultSet rs = statement.executeQuery("SELECT * FROM " + nomeTabella)) {
System.out.println("Connessione al database stabilita e query eseguita");
        // Creazione di una lista osservabile per i prodotti
        ObservableList<Prodotto> listaProdotti = FXCollections.observableArrayList();
            System.out.println("Connessione al database stabilita e query eseguita");
        while (rs.next()) {
            // Crea un nuovo oggetto Prodotto per ogni riga del database
            System.out.println("Lettura riga: " + rs.getString("nome") + ", " + rs.getDouble("prezzo"));
            Prodotto p = new Prodotto();
           // p.setId(rs.getInt("id"));             // Assicurati che i nomi delle colonne ("id", "nome", ecc.) 
            p.setNome(rs.getString("nome"));      // corrispondano a quelli sul tuo database Postgres
            p.setPrezzo(rs.getDouble("prezzo")); 
            System.out.println("Prodotto creato: " + p.getNome() + ", " + p.getPrezzo());
            listaProdotti.add(p);
            System.out.println("Prodotto aggiunto alla lista: " + listaProdotti.size());
        }

        // Imposta i dati nella TableView
        tableDatabase.setItems(listaProdotti);

    } catch (SQLException e) {
        System.err.println("Errore durante la lettura: " + e.getMessage());
    }

*/
       
    }

    @FXML
    private TextField fieldName;
    @FXML
    private TextField fieldCognome;

    @FXML
    public void inserisciInDatabase(ActionEvent event) throws SQLException {
        // Valida il prezzo, inserisce il prodotto nell'archivio selezionato e aggiorna la vista.
       try {
        // 1. Recupera i dati dai componenti UI
        String arch = menuButton.getText();
        String nome = fieldName.getText();
        String prezzoString = fieldCognome.getText(); // Considera di rinominare questo campo in fieldPrezzo

        // Verifica che sia stata selezionata una tabella
        if (arch.isEmpty() || arch.equals("Seleziona Tabella")) {
            System.out.println("Errore: Seleziona una tabella dal MenuButton!");
            return;
        }

        // 2. Converte il prezzo
        Float prezzo = Float.parseFloat(prezzoString);

        // 3. Chiama il Service (Spring gestisce la connessione e la transazione)
        prodottoService.inserisciProdotto(arch, nome, prezzo);

        System.out.println("Inserimento completato in: " + arch);
        
        // 4. Pulisce i campi di testo
        fieldName.clear();
        fieldCognome.clear();

        // 5. Aggiorna la tabella visiva
        leggiDatabase(event);

    } catch (NumberFormatException e) {
        System.err.println("Errore: Il prezzo deve essere un numero valido!");
    } catch (Exception e) {
        System.err.println("Errore durante l'inserimento: " + e.getMessage());
    }
      
      
      
      
      
        /*   //String url = "jdbc:postgresql://localhost:5432/SpringDatabaseProdotti";
        Connection connection = DriverManager.getConnection(url, user, password);
        String arch = menuButton.getText();
        String a = fieldName.getText();
        Float b = Float.parseFloat(fieldCognome.getText());
        System.out.println(arch);
        String sql = "insert into " + arch + " (nome,prezzo) values (?,?)";
        // Statement statement= connection.createStatement();

        PreparedStatement statement = connection.prepareStatement(sql);
        System.out.println("siamo qua");
        
        //inserimento variabili all sql del database
        statement.setString(1, a);
        statement.setFloat(2, b);
        statement.executeUpdate();
        
        //aggiorna tabella dopo inserimento
        leggiDatabase(event);
       
       
       
       
       
     
    }*/}
}


/* 


//creazione tabella
              String sql= "create table utenti (nome varchar(20), cognome varchar(20))";
            Statement statement= connection.createStatement();
            statement.executeUpdate(sql);
            
            System.out.println("tabella creata");*/   


 // inserimento dati in una riga
            /*  String sql= "insert into utenti values ('luca' ,'alecci')";
            Statement statement= connection.createStatement();
            int row= statement.executeUpdate(sql);
            if (row>0){
                System.out.println("riga creata");

            }*/
