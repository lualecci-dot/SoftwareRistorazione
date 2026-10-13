package myapp.controllers.Metodi;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import myapp.controllers.Holder.DataHolder;
import myapp.controllers.Home;
import myapp.controllers.Service.OrdineRicevutoService;
import myapp.controllers.config.SessioneUtente;
import myapp.controllers.config.SpringContextGetter;

public class Tavoli {
    // Funzioni di supporto per trasferire gli ordini tra Home, tavoli e stato in memoria.
    public static void salvaSessioneTavolo(Home homecontroller) {
        // Copia il contenuto visibile delle due colonne nelle mappe condivise.
        if (homecontroller == null || homecontroller.tavoloCorrente == null
                || homecontroller.vBoxDestProdotti == null || homecontroller.vBoxDestPrezzi == null) {
            return;
        }

        List<String> prodotti = new ArrayList<>();
        List<String> prezzi = new ArrayList<>();

        for (Node node : homecontroller.vBoxDestProdotti.getChildren()) {
            if (node instanceof Label label) {
                prodotti.add(label.getText());
            }
        }
        for (Node node : homecontroller.vBoxDestPrezzi.getChildren()) {
            if (node instanceof Label label) {
                prezzi.add(label.getText());
            }
        }

        DataHolder.memoriaProdotti.put(homecontroller.tavoloCorrente, prodotti);
        DataHolder.memoriaPrezzi.put(homecontroller.tavoloCorrente, prezzi);
    }

    public static void chiudiSessioneTavolo(Home homecontroller) {
        if (homecontroller != null && homecontroller.tavoloCorrente != null
                && !inviaOrdiniCassa(homecontroller.tavoloCorrente)) {
            return;
        }

        // Salva prima l'ordine, poi rimuove i riferimenti alla vista che sta per essere chiusa.
        salvaSessioneTavolo(homecontroller);

        homecontroller.vBoxDestProdotti = null;
        homecontroller.vBoxDestPrezzi = null;
        homecontroller.labelTotaleDest = null;
        homecontroller.labelIvaDest = null;

        // 2. Reset del tavolo corrente
        homecontroller.tavoloCorrente = null;
        DataHolder.tavoloApertoAttualmente = "";

        // 3. Mostra di nuovo la griglia dei tavoli (se necessario)
        // anchorTavoli.getChildren().setAll(gridTavoli);

        System.out.println("Sessione tavolo chiusa. Ora i prodotti andranno nella Home.");
    }

    public static boolean inviaOrdiniCassa(String nomeTavolo) {
        List<OrdineRicevutoService.RigaDaInviare> righe = DataHolder.ordiniCassaInAttesa.get(nomeTavolo);
        if (righe == null || righe.isEmpty()) {
            return true;
        }

        try {
            SpringContextGetter.getBean(OrdineRicevutoService.class)
                    .registraComanda(nomeTavolo, SessioneUtente.getUsername(), List.copyOf(righe));
            DataHolder.ordiniCassaInAttesa.remove(nomeTavolo, righe);
            return true;
        } catch (RuntimeException errore) {
            new Alert(Alert.AlertType.ERROR,
                    "Impossibile inviare la comanda del tavolo " + nomeTavolo + ": " + errore.getMessage())
                    .showAndWait();
            return false;
        }
    }

    public static boolean annullaConto(String nomeTavolo, Button bottoneTavolo) {
        Alert conferma = new Alert(Alert.AlertType.CONFIRMATION,
                "Vuoi annullare il conto di " + nomeTavolo + "? Gli ordini non ancora evasi saranno rimossi dalle code.",
                ButtonType.CANCEL, ButtonType.OK);
        conferma.setTitle("Annulla conto");
        if (conferma.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return false;
        }

        try {
            SpringContextGetter.getBean(OrdineRicevutoService.class).annullaConto(nomeTavolo);
            DataHolder.ordiniCassaInAttesa.remove(nomeTavolo);
            DataHolder.memoriaProdotti.remove(nomeTavolo);
            DataHolder.memoriaPrezzi.remove(nomeTavolo);
            if (nomeTavolo.equals(DataHolder.tavoloApertoAttualmente)) {
                DataHolder.tavoloApertoAttualmente = "";
            }
            if (bottoneTavolo != null) {
                bottoneTavolo.setStyle("");
            }
            return true;
        } catch (RuntimeException errore) {
            mostraErrore("Impossibile annullare il conto: " + errore.getMessage());
            return false;
        }
    }

    public static boolean pagaConto(String nomeTavolo, BigDecimal importo, Button bottoneTavolo) {
        if (importo.signum() <= 0) {
            mostraErrore("Non ci sono prodotti da pagare.");
            return false;
        }

        ChoiceDialog<String> sceltaMetodo = new ChoiceDialog<>("Contanti", "Contanti", "Carta");
        sceltaMetodo.setTitle("Pagamento");
        sceltaMetodo.setHeaderText("Pagamento di " + formattaImporto(importo) + " per " + nomeTavolo);
        sceltaMetodo.setContentText("Metodo di pagamento:");
        Optional<String> metodo = sceltaMetodo.showAndWait();
        if (metodo.isEmpty()) {
            return false;
        }

        try {
            SpringContextGetter.getBean(OrdineRicevutoService.class).registraPagamento(
                    nomeTavolo, importo, metodo.get(), SessioneUtente.getUsername());
            DataHolder.memoriaProdotti.remove(nomeTavolo);
            DataHolder.memoriaPrezzi.remove(nomeTavolo);
            if (nomeTavolo.equals(DataHolder.tavoloApertoAttualmente)) {
                DataHolder.tavoloApertoAttualmente = "";
            }
            if (bottoneTavolo != null) {
                bottoneTavolo.setStyle("");
            }
            new Alert(Alert.AlertType.INFORMATION,
                    "Pagamento di " + formattaImporto(importo) + " registrato con " + metodo.get() + ".")
                    .showAndWait();
            return true;
        } catch (RuntimeException errore) {
            mostraErrore("Pagamento non registrato: " + errore.getMessage());
            return false;
        }
    }

    public static void stampaScontrino(
            String nomeTavolo,
            List<String> prodotti,
            List<String> prezzi,
            Window finestra) {
        if (prodotti == null || prodotti.isEmpty()) {
            mostraErrore("Non ci sono prodotti da stampare.");
            return;
        }

        BigDecimal totale = BigDecimal.ZERO;
        StringBuilder testo = new StringBuilder("MYAPP\nScontrino - ").append(nomeTavolo).append("\n\n");
        VBox ricevuta = new VBox(8);
        ricevuta.setPadding(new Insets(24));
        ricevuta.setPrefWidth(320);
        ricevuta.getChildren().addAll(
                new Label("MYAPP"),
                new Label("Scontrino - " + nomeTavolo),
                new Label(java.time.LocalDateTime.now().format(
                        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))),
                new javafx.scene.control.Separator());

        for (int indice = 0; indice < prodotti.size(); indice++) {
            String prezzo = indice < prezzi.size() ? prezzi.get(indice) : "0";
            BigDecimal valore = new BigDecimal(prezzo.replace(',', '.'));
            totale = totale.add(valore);
            testo.append(prodotti.get(indice)).append("  ").append(formattaImporto(valore)).append('\n');
            HBox riga = new HBox(12,
                    new Label(prodotti.get(indice)),
                    new javafx.scene.layout.Region(),
                    new Label(formattaImporto(valore)));
            HBox.setHgrow(riga.getChildren().get(1), Priority.ALWAYS);
            ricevuta.getChildren().add(riga);
        }

        testo.append("\nTOTALE: ").append(formattaImporto(totale));
        ricevuta.getChildren().addAll(new javafx.scene.control.Separator(), new Label("TOTALE: " + formattaImporto(totale)));

        PrinterJob stampa = PrinterJob.createPrinterJob();
        if (stampa == null) {
            Alert anteprima = new Alert(Alert.AlertType.INFORMATION, testo.toString());
            anteprima.setTitle("Scontrino");
            anteprima.setHeaderText("Anteprima scontrino");
            anteprima.showAndWait();
            return;
        }

        if (stampa.showPrintDialog(finestra)) {
            if (stampa.printPage(ricevuta)) {
                stampa.endJob();
            } else {
                stampa.cancelJob();
                mostraErrore("La stampa dello scontrino non è riuscita.");
            }
        }
    }

    private static String formattaImporto(BigDecimal importo) {
        return NumberFormat.getCurrencyInstance(Locale.ITALY).format(importo);
    }

    private static void mostraErrore(String messaggio) {
        new Alert(Alert.AlertType.WARNING, messaggio).showAndWait();
    }

    /*
     * public static void popolaGrigliaTavoli(Home homecontroller, List<String>
     * nomiTavoli) {
     * // 1. Determina il numero di righe e colonne dai vincoli (Constraints)
     * System.out.println("Popolamento griglia tavoli...");
     * int numColonne = homecontroller.gridTavoli.getColumnConstraints().size();
     * int numRighe = homecontroller.gridTavoli.getRowConstraints().size();
     * 
     * // Puliamo la griglia e la mappa prima di ripopolare (per evitare doppioni)
     * homecontroller.gridTavoli.getChildren().clear();
     * // DataHolder.mappaBottoniTavoli.clear();
     * 
     * for (int riga = 0; riga < numRighe; riga++) {
     * for (int col = 0; col < numColonne; col++) {
     * 
     * // Crea il nome del tavolo (deve coincidere con quello inviato dal Web)
     * String nomeTavolo = "Tavolo " + (riga * numColonne + col + 1);
     * 
     * // Creiamo il bottone locale
     * Button btnTavolo = new Button(nomeTavolo);
     * MenuTendina.aggiungiMenuContestuale(btnTavolo, nomeTavolo);
     * 
     * // 2. SALVATAGGIO NELLA MAPPA STATICA
     * // Questo permette a Spring di fare:
     * DataHolder.mappaBottoniTavoli.get("Tavolo
     * // 1")
     * DataHolder.mappaBottoniTavoli.put(nomeTavolo, btnTavolo);
     * 
     * if (DataHolder.memoriaProdotti.containsKey(nomeTavolo) &&
     * !DataHolder.memoriaProdotti.get(nomeTavolo).isEmpty()) {
     * 
     * btnTavolo.
     * setStyle("-fx-background-color: #ff4444; -fx-text-fill: white; -fx-font-weight: bold;"
     * );
     * }
     * 
     * // Layout Responsive
     * btnTavolo.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
     * GridPane.setHgrow(btnTavolo, Priority.ALWAYS);
     * GridPane.setVgrow(btnTavolo, Priority.ALWAYS);
     * 
     * // Azione al click
     * btnTavolo.setOnAction(event -> {
     * 
     * try {
     * // Reset dello stile (togliamo il verde/rosso quando entriamo nel tavolo)
     * 
     * homecontroller.ultimoBottoneTavoloCliccato = btnTavolo;
     * homecontroller.tavoloCorrente = nomeTavolo; // Imposta il tavolo per la
     * memoria
     * 
     * // Carica l'interfaccia del tavolo
     * selectTavolo();
     * 
     * aggiornaStatoColoreBottone(btnTavolo);
     * // Se il tavolo ha già prodotti (es. caricati dalla Home o Web), lo coloriamo
     * di
     * // rosso
     * if (homecontroller.vBoxDestProdotti != null &&
     * !homecontroller.vBoxDestProdotti.getChildren().isEmpty()) {
     * btnTavolo.setStyle(
     * "-fx-background-color: #ff4444; -fx-text-fill: white; -fx-font-weight: bold;"
     * );
     * }
     * 
     * } catch (IOException | SQLException e) {
     * e.printStackTrace();
     * }
     * });
     * // Aggiunge il bottone alla griglia visuale
     * homecontroller.gridTavoli.add(btnTavolo, col, riga);
     * 
     * 
     * }
     * }
     * System.out.println(
     * "✅ Griglia popolata e " + DataHolder.mappaBottoniTavoli.size() +
     * " bottoni registrati in memoria.");
     * }
     */

    public static void popolaGrigliaTavoli(Home homecontroller, List<String> nomiTavoli) {
            // Riutilizza i pulsanti già registrati per mantenere il collegamento con gli ordini web.
        System.out.println("Popolamento griglia tavoli...");

        // Sicurezza: se il controller o la griglia sono null, esci
        if (homecontroller == null || homecontroller.gridTavoli == null)
            return;

        int numColonne = homecontroller.gridTavoli.getColumnConstraints().size();
        int numRighe = homecontroller.gridTavoli.getRowConstraints().size();

        homecontroller.gridTavoli.getChildren().clear();

        for (int riga = 0; riga < numRighe; riga++) {
            for (int col = 0; col < numColonne; col++) {

                String nomeTavolo = "Tavolo " + (riga * numColonne + col + 1);

                // RECUPERO O CREAZIONE (per non perdere il link col Web)
                Button btnTavolo;
                if (DataHolder.mappaBottoniTavoli.containsKey(nomeTavolo)) {
                    btnTavolo = DataHolder.mappaBottoniTavoli.get(nomeTavolo);
                    // Stacca dal vecchio genitore prima di riaggiungerlo
                    if (btnTavolo.getParent() != null) {
                        ((javafx.scene.layout.Pane) btnTavolo.getParent()).getChildren().remove(btnTavolo);
                    }
                } else {
                    btnTavolo = new Button(nomeTavolo);
                    DataHolder.mappaBottoniTavoli.put(nomeTavolo, btnTavolo);
                }

                MenuTendina.aggiungiMenuContestuale(btnTavolo, nomeTavolo);

                // Stato colore iniziale
                if (DataHolder.memoriaProdotti.containsKey(nomeTavolo)
                        && !DataHolder.memoriaProdotti.get(nomeTavolo).isEmpty()) {
                    btnTavolo.setStyle("-fx-background-color: #ff4444; -fx-text-fill: white; -fx-font-weight: bold;");
                }

                btnTavolo.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                GridPane.setHgrow(btnTavolo, Priority.ALWAYS);
                GridPane.setVgrow(btnTavolo, Priority.ALWAYS);

                btnTavolo.setOnAction(event -> {
                    try {
                        homecontroller.ultimoBottoneTavoloCliccato = btnTavolo;
                        homecontroller.tavoloCorrente = nomeTavolo;
                        DataHolder.tavoloApertoAttualmente = nomeTavolo;

                        // AGGIORNA QUESTI METODI PER ACCETTARE homecontroller
                        selectTavolo(homecontroller);
                        aggiornaStatoColoreBottone(homecontroller, btnTavolo);

                    } catch (IOException | SQLException e) {
                        e.printStackTrace();
                    }
                });

                // CORRETTO: usa l'istanza reale
                homecontroller.gridTavoli.add(btnTavolo, col, riga);
            }
        }
    }

    public static void selectTavolo(Home homecontroller) throws IOException, SQLException {
    // Carica la schermata dell'ordine e ripristina i dati del tavolo selezionato.

        FXMLLoader loader = null;
        try {
            loader = new FXMLLoader(Tavoli.class.getResource("/myapp/Home.fxml"));
            loader.load();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }

        Node soloHBox = (Node) loader.getNamespace().get("hboxHome");

        // Catturiamo i riferimenti della nuova istanza per il bottone
        homecontroller.vBoxDestProdotti = (VBox) loader.getNamespace().get("vboxPane");
        homecontroller.vBoxDestPrezzi = (VBox) loader.getNamespace().get("vboxPane1");
        homecontroller.labelTotaleDest = (Label) loader.getNamespace().get("labelTotale");

        if (soloHBox != null) {
            homecontroller.anchorTavoli.getChildren().setAll(soloHBox);
            AnchorPane.setTopAnchor(soloHBox, 0.0);
            AnchorPane.setBottomAnchor(soloHBox, 0.0);

            if (DataHolder.memoriaProdotti.containsKey(homecontroller.tavoloCorrente)) {
                // --- CARICA DA MEMORIA ---
                float totaleRecuperato = 0;
                List<String> prod = DataHolder.memoriaProdotti.get(homecontroller.tavoloCorrente);
                List<String> prez = DataHolder.memoriaPrezzi.getOrDefault(homecontroller.tavoloCorrente,
                        new ArrayList<>());

                for (int i = 0; i < prod.size(); i++) {
                    homecontroller.vBoxDestProdotti.getChildren().add(new Label(prod.get(i)));
                    if (i < prez.size()) {
                        homecontroller.vBoxDestPrezzi.getChildren().add(new Label(prez.get(i)));
                        totaleRecuperato += Float.parseFloat(prez.get(i));
                    }
                }
                homecontroller.labelTotaleDest.setText(String.valueOf(totaleRecuperato));
                System.out.println("siamo carica memoria" + homecontroller.vBoxDestProdotti.getChildren().isEmpty());

            } else {
                // --- PRIMA APERTURA: COPIA DA HOME ---
                List<String> listaP = new ArrayList<>();
                List<String> listaPr = new ArrayList<>();

                for (Node n : homecontroller.vboxPane.getChildren()) {
                    if (n instanceof Label) {
                        String t = ((Label) n).getText();
                        homecontroller.vBoxDestProdotti.getChildren().add(new Label(t));
                        listaP.add(t);
                    }
                }
                for (Node n1 : homecontroller.vboxPane1.getChildren()) {
                    if (n1 instanceof Label) {
                        String t1 = ((Label) n1).getText();
                        homecontroller.vBoxDestPrezzi.getChildren().add(new Label(t1));
                        listaPr.add(t1);
                    }
                }
                // Salva le liste iniziali nella mappa
                DataHolder.memoriaProdotti.put(homecontroller.tavoloCorrente, listaP);
                DataHolder.memoriaPrezzi.put(homecontroller.tavoloCorrente, listaPr);

                homecontroller.vboxPane.getChildren().clear();
                homecontroller.vboxPane1.getChildren().clear();
                homecontroller.labelTotale.setText("0");
                System.out.println("siamo nel copia da home" + homecontroller.vBoxDestProdotti.getChildren().isEmpty());

            }
        }
    }

    public static void aggiornaStatoColoreBottone(Home homecontroller, Button btn) {
            // Il colore segnala se il tavolo ha prodotti associati.
        // Se ci sono prodotti nel carrello/tavolo, deve essere rosso
        if (homecontroller.vBoxDestProdotti != null && !homecontroller.vBoxDestProdotti.getChildren().isEmpty()) {
            btn.setStyle("-fx-background-color: #ff4444; -fx-text-fill: white; -fx-font-weight: bold;");
        } else {
            // Se è vuoto, torna allo stile di default (grigio/standard)
            btn.setStyle("");
        }
    }

}
