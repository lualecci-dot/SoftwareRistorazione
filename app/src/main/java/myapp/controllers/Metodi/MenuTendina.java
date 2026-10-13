package myapp.controllers.Metodi;

import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import myapp.controllers.Holder.DataHolder;

public class MenuTendina {

    // Aggiunge al pulsante del tavolo le azioni contestuali accessibili col tasto destro.
    public static void aggiungiMenuContestuale(Button btn, String nomeTavolo) {
        // Creazione della tendina
        ContextMenu contextMenu = new ContextMenu();

        MenuItem itemAnnulla = new MenuItem("Annulla conto");
        MenuItem itemScontrino = new MenuItem("Stampa scontrino");
        MenuItem itemPaga = new MenuItem("Paga conto");
        MenuItem itemInfo = new MenuItem("Visualizza Info");

        itemAnnulla.setOnAction(e -> {
            if (Tavoli.annullaConto(nomeTavolo, btn)) {
                System.out.println("Conto annullato per " + nomeTavolo);
            }
        });

        itemScontrino.setOnAction(e -> {
            Tavoli.stampaScontrino(
                    nomeTavolo,
                    DataHolder.memoriaProdotti.getOrDefault(nomeTavolo, java.util.List.of()),
                    DataHolder.memoriaPrezzi.getOrDefault(nomeTavolo, java.util.List.of()),
                    btn.getScene() == null ? null : btn.getScene().getWindow());
        });

        itemPaga.setOnAction(e -> {
            java.util.List<String> prezzi = DataHolder.memoriaPrezzi.getOrDefault(nomeTavolo, java.util.List.of());
            java.math.BigDecimal totale = prezzi.stream()
                    .map(prezzo -> new java.math.BigDecimal(prezzo.replace(',', '.')))
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            Tavoli.pagaConto(nomeTavolo, totale, btn);
        });

        // Aggiungiamo gli elementi alla tendina
        contextMenu.getItems().addAll(itemAnnulla, itemScontrino, itemPaga, itemInfo);

        // Colleghiamo la tendina al bottone (JavaFX la aprirà col tasto destro)
        btn.setContextMenu(contextMenu);
    }

}
