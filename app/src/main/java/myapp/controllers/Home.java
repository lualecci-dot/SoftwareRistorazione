package myapp.controllers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import myapp.App;
import myapp.controllers.Holder.DataHolder;
import myapp.controllers.Metodi.SwichPage;
import myapp.controllers.Metodi.Tavoli;
import myapp.controllers.Service.DatabaseService;
import myapp.controllers.Service.OrdineRicevutoService;
import myapp.controllers.config.DatabaseConfig;
import myapp.controllers.config.FxmlView;
import myapp.controllers.config.SessioneUtente;

@Component
public class Home extends ControllerAutenticato {

    // Controller della cassa: collega le viste JavaFX ai tavoli e al menu prodotti.
    public ConfigurableApplicationContext applicationContext;

    @FXML
    private MenuButton menuButton;
    @FXML
    private Button btn;
    @FXML
    private Button bData;
    @FXML
    private Button bCamerieri;
    @FXML
    private Button bCassa;
        @FXML
        private Label usernameLabel;
        @FXML
        private ImageView qrCodeImage;
    @FXML
    private VBox vBox;
    @FXML
    private VBox vBoxCenter;
    @FXML
    private VBox vBoxRight;
    @FXML
    private Pane pane1;
    @FXML
    public VBox vboxPane;
    @FXML
    public VBox vboxPane1;
    @FXML
    private VBox vBoxMenu;
    @FXML
    public Label labelTotale;
    @FXML
    private Label labelIva;
    @FXML
    private BorderPane borderPane;
    @FXML
    private AnchorPane anchorHome;
    @FXML
    public VBox vBoxDestProdotti;
    @FXML
    public VBox vBoxDestPrezzi;
    @FXML
    public Label labelTotaleDest;
    @FXML
    public Label labelIvaDest;
    @FXML
    private TabPane tabPane;
    @FXML
    private Button buttonTavolo;
    @FXML
    public AnchorPane anchorTavoli;
    @FXML
    private HBox hboxHome;
    @FXML
    public GridPane gridTavoli;
    @FXML
    public String tavoloCorrente;
    @FXML
    public AnchorPane anchorSwicth;
    @FXML
    public Button ultimoBottoneTavoloCliccato;

    public Button btnStatico;

    @FXML
    public void swichHometoData(ActionEvent event) throws SQLException, IOException {
        if (!SessioneUtente.isAmministratore()) {
            new Alert(Alert.AlertType.WARNING, "La gestione del database è riservata all'amministratore.").showAndWait();
            return;
        }
        SwichPage.swichHometoData(this);
    }

    @FXML
    private void switchToCamerieri() throws IOException {
        SessioneUtente.richiediAmministratore();
        App.switchToNextScene(FxmlView.CAMERIERI);
    }

    @FXML
    private void apriPaginaCucina() {
        try {
            App.apriLinkEsterno("http://localhost:8080/cucina");
        } catch (IllegalStateException errore) {
            new Alert(Alert.AlertType.ERROR, "Impossibile aprire la pagina cucina: " + errore.getMessage())
                    .showAndWait();
        }
    }

    @FXML
    private void apriPaginaBar() {
        try {
            App.apriLinkEsterno("http://localhost:8080/bar");
        } catch (IllegalStateException errore) {
            new Alert(Alert.AlertType.ERROR, "Impossibile aprire la pagina bar: " + errore.getMessage())
                    .showAndWait();
        }
    }

    @FXML
    private void annullaConto() {
        String conto = chiaveContoAttuale();
        if (Tavoli.annullaConto(conto, DataHolder.mappaBottoniTavoli.get(conto))) {
            svuotaVistaConto();
        }
    }

    @FXML
    private void stampaScontrino() {
        String conto = chiaveContoAttuale();
        VBox prodotti = prodottiContoAttuale();
        VBox prezzi = prezziContoAttuale();
        List<String> nomi = new ArrayList<>();
        List<String> importi = new ArrayList<>();
        for (javafx.scene.Node nodo : prodotti.getChildren()) {
            if (nodo instanceof Label label) {
                nomi.add(label.getText());
            }
        }
        for (javafx.scene.Node nodo : prezzi.getChildren()) {
            if (nodo instanceof Label label) {
                importi.add(label.getText());
            }
        }
        Tavoli.stampaScontrino(
                conto,
                nomi,
                importi,
            borderPane == null || borderPane.getScene() == null
                ? null
                : borderPane.getScene().getWindow());
    }

    @FXML
    private void pagaConto() {
        String conto = chiaveContoAttuale();
        BigDecimal totale = prezziContoAttuale().getChildren().stream()
                .filter(Label.class::isInstance)
                .map(Label.class::cast)
                .map(Label::getText)
                .map(prezzo -> new BigDecimal(prezzo.replace(',', '.')))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (Tavoli.pagaConto(conto, totale, DataHolder.mappaBottoniTavoli.get(conto))) {
            svuotaVistaConto();
        }
    }

    private String chiaveContoAttuale() {
        String tavolo = DataHolder.tavoloApertoAttualmente;
        return tavolo == null || tavolo.isBlank() ? "Cassa" : tavolo;
    }

    private VBox prodottiContoAttuale() {
        return vBoxDestProdotti == null ? vboxPane : vBoxDestProdotti;
    }

    private VBox prezziContoAttuale() {
        return vBoxDestPrezzi == null ? vboxPane1 : vBoxDestPrezzi;
    }

    private Label totaleContoAttuale() {
        return labelTotaleDest == null ? labelTotale : labelTotaleDest;
    }

    private void svuotaVistaConto() {
        prodottiContoAttuale().getChildren().clear();
        prezziContoAttuale().getChildren().clear();
        totaleContoAttuale().setText("0");
        if (labelIvaDest != null) {
            labelIvaDest.setText("0");
        }
        if (labelIva != null) {
            labelIva.setText("0");
        }
    }

    @FXML
    public void swicthDatatoHome(ActionEvent event) throws SQLException, IOException {
        SwichPage.swicthDatatoHome(this);
    }

    // Inizializza i riferimenti condivisi, carica gli archivi e prepara la griglia dei tavoli.
    @FXML
    


    private void initialize() throws SQLException, IOException {

        // Espone le aree dell'ordine al controller web, che le aggiorna sul thread JavaFX.
        DataHolder.vBoxDestinazioneprod = vBoxDestProdotti;
        DataHolder.vBoxDestinazioneprezzo = vBoxDestPrezzi;
        // Salva il riferimento statico del bottone per l'uso globale
        btnStatico = btn;
        usernameLabel.setText(SessioneUtente.getUsername());
        generaQrCode();
        // Il cassiere usa la cassa ma non può aprire la gestione degli archivi.
        boolean amministratore = SessioneUtente.isAmministratore();
        bData.setVisible(amministratore);
        bData.setManaged(amministratore);
        bCamerieri.setVisible(amministratore);
        bCamerieri.setManaged(amministratore);
        // Carica i nomi delle tabelle dal database
        initializeDatabase();
        // Popola la griglia dei tavoli se non è già stata popolata

        List<String> nomiTavoli = new ArrayList<>(); // Caricali dal DB
        if (DataHolder.mappaBottoniTavoli.isEmpty()) {

            // Configura qui il bottone (dimensione, click, ecc.)
            Tavoli.popolaGrigliaTavoli(this, nomiTavoli);

        } else {
            // Se la mappa non è vuota, significa che i bottoni esistono già (sono stati
            // creati in precedenza)
            // Quindi li riusiamo semplicemente senza ricrearli
            Tavoli.popolaGrigliaTavoli(this, nomiTavoli); // Rinfresca la griglia con i bottoni esistenti
        }
        // gridTavoli.getChildren().clear();

        // Listener per il cambio tab
        tabPane.getSelectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) -> {
            if (newTab != null && (newTab.getText().equals("Home") || newTab.getText().equals("Cassa"))) {
                Tavoli.chiudiSessioneTavolo(this);
                Tavoli.popolaGrigliaTavoli(this, nomiTavoli); // Rinfresca i bottoni se torni in Home
            }
        });

        FontIcon archiveIcon = new FontIcon("fas-archive"); // Icona Archivio
        archiveIcon.setIconSize(12); // Dimensione dell'icona
        bData.setGraphic(archiveIcon);
        bData.setContentDisplay(ContentDisplay.LEFT); // Icona a sinistra del testo

        FontIcon archiveIcon1 = new FontIcon("fas-cash-register"); // Icona Archivio
        archiveIcon1.setIconSize(12); // Dimensione dell'icona
        bCassa.setGraphic(archiveIcon1);
        bCassa.setContentDisplay(ContentDisplay.LEFT); // Icona a sinistra del testo

    }

    private void generaQrCode() {
        String host = "localhost";
        try {
            host = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            System.err.println("Impossibile determinare l'indirizzo IP locale: " + e.getMessage());
        }

        String urlOrdini = "http://" + host + ":8080/mobile/tavoli";
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(
                    urlOrdini,
                    BarcodeFormat.QR_CODE,
                    150,
                    150,
                    Map.of(EncodeHintType.MARGIN, 1)
            );
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            qrCodeImage.setImage(new Image(new ByteArrayInputStream(output.toByteArray())));
        } catch (Exception e) {
            System.err.println("Errore nella generazione del QR code: " + e.getMessage());
        }
    }

    @FXML
    private void initializeDatabase() {
        // Legge gli archivi PostgreSQL e crea un pulsante per aprire ciascun menu.
        System.out.println("Avvio caricamento tabelle da PostgreSQL...");

        String url = DatabaseConfig.URL;
        String user = DatabaseConfig.USERNAME;
        String password = DatabaseConfig.PASSWORD;

        // Query corretta per PostgreSQL per ottenere i nomi delle tabelle
        String sqlGetTables = "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'";

        try (Connection conn = DriverManager.getConnection(url, user, password);
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(sqlGetTables)) {

            vBox.getChildren().clear(); // Pulisci i bottoni precedenti

            while (rs.next()) {
                String tableName = rs.getString("table_name");
                if (!DatabaseService.isTabellaProdotto(tableName)) {
                    continue;
                }
                Button btnTable = new Button(tableName);
                btnTable.setId("buttonDatabase");
                vBox.getChildren().add(btnTable);

                btnTable.setOnAction(event -> {
                    vBoxCenter.getChildren().clear();
                    caricaDatiTabella(tableName); // Chiamiamo una funzione dedicata
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void caricaDatiTabella(String tableName) {

        String url = DatabaseConfig.URL;
       
        if (!tableName.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            System.err.println("Nome tabella non valido: " + tableName);
            return;
        }
        String sql = "SELECT * FROM \"" + tableName + "\"";

        // Le tabelle selezionabili devono avere le colonne convenzionali nome e prezzo.
        vBoxCenter.getChildren().clear();

        try (Connection conn = DriverManager.getConnection(url, DatabaseConfig.USERNAME, DatabaseConfig.PASSWORD);
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(sql)) {

            boolean datiTrovati = false;

            while (rs.next()) {
                datiTrovati = true;
                String name = rs.getString("nome");
                float prezzo = rs.getFloat("prezzo");

                Button btnItem = new Button(name + "   € " + prezzo);
                btnItem.setPrefWidth(150); // Dai una dimensione minima
                btnItem.setId("buttonDatabase2"); // Usa il tuo stile CSS

                btnItem.setOnAction(e -> {
                    String tavoloOrdine = DataHolder.tavoloApertoAttualmente;
                    String chiaveOrdine = tavoloOrdine == null || tavoloOrdine.isBlank()
                            ? "Cassa"
                            : tavoloOrdine;
                    // Seleziona la vista dell'ordine aperto o, in sua assenza, quella della Home.
                    VBox targetP = (vBoxDestProdotti != null) ? vBoxDestProdotti : vboxPane;
                    VBox targetPr = (vBoxDestPrezzi != null) ? vBoxDestPrezzi : vboxPane1;
                    Label targetTot = (labelTotaleDest != null) ? labelTotaleDest : labelTotale;

                    // 2. Aggiunta grafica immediata
                    targetP.getChildren().add(new Label(name));
                    targetPr.getChildren().add(new Label(String.valueOf(prezzo)));

                    // 3. Aggiornamento Totale visivo
                    float nuovoTot = Float.parseFloat(targetTot.getText()) + prezzo;
                    targetTot.setText(String.valueOf(nuovoTot));

                    System.out.println("VBox vuoto? " + targetP.getChildren().isEmpty());
                    System.out.println("Bottone salvato? " + (ultimoBottoneTavoloCliccato != null));
                    // Conserva l'ordine in memoria finché il tavolo resta aperto nell'applicazione.
                    DataHolder.ordiniCassaInAttesa
                            .computeIfAbsent(chiaveOrdine, k -> new ArrayList<>())
                            .add(new OrdineRicevutoService.RigaDaInviare(
                                    name,
                                    new BigDecimal(Float.toString(prezzo)),
                                    tableName));

                    if (tavoloOrdine != null && !tavoloOrdine.isBlank()) {
                        // Aggiungiamo i dati alle liste che vivono già nella mappa
                        DataHolder.memoriaProdotti
                            .computeIfAbsent(tavoloOrdine, k -> new ArrayList<>())
                                .add(name);

                        DataHolder.memoriaPrezzi
                            .computeIfAbsent(tavoloOrdine, k -> new ArrayList<>())
                                .add(String.valueOf(prezzo));

                        Button bottoneTavolo = ultimoBottoneTavoloCliccato != null
                            ? ultimoBottoneTavoloCliccato
                            : DataHolder.mappaBottoniTavoli.get(tavoloOrdine);
                        if (bottoneTavolo != null) {
                            // Se il VBox ha figli (quindi non è vuoto)
                            if (!targetP.getChildren().isEmpty()) {
                                bottoneTavolo.setStyle(
                                        "-fx-background-color: #ff4444 !important; -fx-text-fill: white; -fx-font-weight: bold;");
                                System.out.println("Colore cambiato al volo per: " + tavoloOrdine);
                            }
                        } else {
                            // Se arrivi qui, il riferimento è andato perso tra i vari FXML
                            System.out.println("ATTENZIONE: ultimoBottoneTavoloCliccato è NULL!");
                        }
                    }

                });

                // Aggiungi al VBox
                vBoxCenter.getChildren().add(btnItem);
            }

            if (!datiTrovati) {
                System.out.println("ATTENZIONE: La tabella " + tableName + " è vuota nel DB.");
            }

        } catch (SQLException e) {
            System.err.println("ERRORE SQL: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Riferimenti ai componenti del tavolo aperto (Tab 2)

    @FXML
    private void changeTavoli() throws IOException {
    // Ripristina la griglia dei tavoli quando viene selezionata la relativa scheda.

        if (anchorTavoli == null || gridTavoli == null) {
            return;
        }

        // 1. Carica il nuovo FXML (es. DettaglioProdotto.fxml)
        // Node nuovaFinestra =
        // FXMLLoader.load(getClass().getResource("/myapp/views/Dettaglio.fxml"));

        // 2. Rimuovi il GridPane e metti la nuova interfaccia
        anchorTavoli.getChildren().setAll(gridTavoli);

        // 3. (Opzionale) Se vuoi che la nuova finestra occupi tutto lo spazio
        // VBox.setVgrow(hboxHome, Priority.ALWAYS);
    }

}