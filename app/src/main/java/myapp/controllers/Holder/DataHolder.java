package myapp.controllers.Holder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import myapp.controllers.Service.OrdineRicevutoService.RigaDaInviare;

// All'interno della tua classe (es. MobileOrderController o HomeService)
public class DataHolder {
    // Stato condiviso in memoria tra il server Spring e l'interfaccia JavaFX.
    // Le mappe concorrenti proteggono l'accesso alla struttura, non rendono sincronizzate le liste contenute.
    public static Map<String, List<String>> memoriaProdotti = new ConcurrentHashMap<>();
    public static Map<String, List<String>> memoriaPrezzi = new ConcurrentHashMap<>();
    public static Map<String, List<RigaDaInviare>> ordiniCassaInAttesa = new ConcurrentHashMap<>();
    public static Set<java.util.UUID> ordiniMobileApplicatiInMemoria = ConcurrentHashMap.newKeySet();
    public static Map<String, Button> mappaBottoniTavoli = new ConcurrentHashMap<>();

    // Riferimenti alla schermata dell'ordine aperto, usati per aggiornamenti in tempo reale.
    public static VBox vBoxDestinazioneprod;
     public static VBox vBoxDestinazioneprezzo;
      public static Label labelTotaleDest;       // La label del totale
       public static String tavoloApertoAttualmente = "";

     // Elenco dei tavoli mostrati dalla pagina mobile.
     public static Map<String, VBox> mappaVBoxTavoli = new ConcurrentHashMap<>();
      public static List<String> elencoTavoli = new ArrayList<>();
}