package myapp.controllers.WebController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import myapp.controllers.Holder.DataHolder;
import myapp.controllers.Service.AutenticazioneService;
import myapp.controllers.Service.DatabaseService;
import myapp.controllers.Service.OrdineRicevutoService;

@Controller
public class MobileOrderController {

    // Espone il menu mobile e riceve gli ordini inviati dal browser del cliente.

    @Autowired
    private AutenticazioneService autenticazioneService;

    @GetMapping("/mobile/login")
    public String paginaLogin(HttpSession session) {
        return cameriereAutenticato(session) ? "redirect:/mobile/tavoli" : "accesso-mobile";
    }

    @PostMapping("/mobile/login")
    public String autenticaCameriere(
            @RequestParam(name = "username", required = false) String username,
            @RequestParam(name = "password", required = false) String password,
            HttpServletRequest request,
            Model model) {
        if (!autenticazioneService.autenticaCameriere(username, password)) {
            model.addAttribute("messaggioErrore", "Username o password non corretti.");
            return "accesso-mobile";
        }

        HttpSession sessionePrecedente = request.getSession(false);
        if (sessionePrecedente != null) {
            sessionePrecedente.invalidate();
        }
        request.getSession(true).setAttribute(
                AutenticazioneService.SESSIONE_CAMERIERE,
                username.trim());
        return "redirect:/mobile/tavoli";
    }

    @PostMapping("/mobile/logout")
    public String esci(HttpSession session) {
        session.invalidate();
        return "redirect:/mobile/login";
    }

    // 1. Pagina di selezione iniziale dei tavoli
    @GetMapping("/mobile/tavoli")
public String selezioneTavolo(Model model, HttpSession session) {
    if (!cameriereAutenticato(session)) {
        return "redirect:/mobile/login";
    }
    // Crea 25 tavoli dimostrativi e li conserva in memoria per le richieste successive.
    List<String> tavoliAutomatici = new ArrayList<>();
    for (int i = 1; i <= 25; i++) {
        tavoliAutomatici.add("Tavolo " + i);
    }

    // Se il DataHolder è vuoto, lo popoliamo per la prima volta
    if (DataHolder.elencoTavoli.isEmpty()) {
        DataHolder.elencoTavoli.addAll(tavoliAutomatici);
    }

    List<String> tavoliOccupati = DataHolder.memoriaProdotti.entrySet().stream()
            .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
            .map(Map.Entry::getKey)
            .toList();
    model.addAttribute("tavoli", DataHolder.elencoTavoli); 
    model.addAttribute("tavoliOccupati", tavoliOccupati);
    return "scelta-tavolo";
}
    


 @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OrdineRicevutoService ordineRicevutoService;

    //  Carica il menu per lo specifico tavolo selezionato
    @GetMapping("/mobile/ordine/{nomeTavolo}")
    public ModelAndView mostraMenu(@PathVariable String nomeTavolo, HttpSession session) {
        if (!cameriereAutenticato(session)) {
            return new ModelAndView("redirect:/mobile/login");
        }
        // Costruisce il menu leggendo le tabelle prodotto presenti nello schema pubblico.
        ModelAndView model = new ModelAndView("ordine-mobile");
        Map<String, List<Map<String, Object>>> menuCompleto = new LinkedHashMap<>();

        // 1. Recupera i nomi di TUTTE le tabelle dal database
        List<String> nomiTabelle = jdbcTemplate.queryForList(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'", 
            String.class
        );

        for (String tabella : nomiTabelle) {
            // Escludi tabelle di sistema o tecniche
            if (!DatabaseService.isTabellaProdotto(tabella)) {
                continue;
            }

            List<Map<String, Object>> righe = caricaProdottiDaTabella(tabella);
            if (righe != null) {
                menuCompleto.put(tabella.toUpperCase(), righe);
            }
        }

        model.addObject("menuDinamico", menuCompleto);
        model.addObject("tavoloSelezionato", nomeTavolo);
        return model;
    }

    private List<Map<String, Object>> caricaProdottiDaTabella(String nomeTabella) {
            // Individua una colonna testuale e una numerica senza dipendere dai loro nomi.
        if (!nomeTabella.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            return null;
        }

        String colonneSql = "SELECT column_name, data_type FROM information_schema.columns "
                + "WHERE table_schema = 'public' AND table_name = ? ORDER BY ordinal_position";
        List<Map<String, Object>> colonne = jdbcTemplate.queryForList(colonneSql, nomeTabella);

        String colonnaNome = null;
        String colonnaPrezzo = null;
        for (Map<String, Object> colonna : colonne) {
            String nome = (String) colonna.get("column_name");
            String tipo = (String) colonna.get("data_type");

            if (colonnaNome == null && ("character varying".equals(tipo) || "text".equals(tipo))) {
                colonnaNome = nome;
            }
            if (colonnaPrezzo == null && ("real".equals(tipo)
                    || "double precision".equals(tipo)
                    || "numeric".equals(tipo))) {
                colonnaPrezzo = nome;
            }
        }

        if (colonnaNome == null || colonnaPrezzo == null) {
            System.err.println("Tabella ignorata: " + nomeTabella
                    + " non contiene una colonna testo e una numerica");
            return null;
        }

        String sql = "SELECT " + quoteIdentifier(colonnaNome) + " AS nome, "
                + quoteIdentifier(colonnaPrezzo) + " AS prezzo FROM "
                + quoteIdentifier(nomeTabella);
        return jdbcTemplate.queryForList(sql);
    }

    private String quoteIdentifier(String value) {
            // I nomi di tabelle e colonne non sono parametrizzabili: validarli prima di inserirli nell'SQL.
        if (!value.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Identificatore SQL non valido");
        }
        return "\"" + value + "\"";
    }

    private boolean cameriereAutenticato(HttpSession session) {
        return session.getAttribute(AutenticazioneService.SESSIONE_CAMERIERE) instanceof String;
    }




/* 
//FUNZIONA  
@GetMapping("/mobile/ordine/{nomeTavolo}")
public String mostraMenu(@PathVariable String nomeTavolo, Model model) {
    System.out.println("1. Entrato nel metodo per: " + nomeTavolo);
    
    if (prodottoService == null) {
        System.err.println("ERRORE: prodottoService è NULL! Controlla @Autowired");
        return "error"; 
    }

    try {
        List<Prodotto> listaReale = prodottoService.findAll();
        System.out.println("2. Dati recuperati dal DB: " + listaReale.size() + " prodotti");

        model.addAttribute("prodotti", listaReale);
        model.addAttribute("tavoloSelezionato", nomeTavolo);
        
        return "ordine-mobile";
    } catch (Exception e) {
        System.err.println("3. ERRORE durante findAll(): " + e.getMessage());
        e.printStackTrace(); // Questo stamperà l'errore esatto in rosso nella console
        return "error";
    }
}*/




/*@GetMapping("/mobile/ordine/{nomeTavolo}")
public String mostraMenu(@PathVariable String nomeTavolo, Model model) {
    System.out.println("Richiesto ordine per: " + nomeTavolo);
    
    // Test: passiamo una lista finta per vedere se la pagina si apre
    List<Prodotto> testLista = new ArrayList<>();
    testLista.add(new Prodotto("Pizza Test", 5.0));
    
    model.addAttribute("prodotti", testLista); // Prova con testLista invece di prodottoService.findAll()
    model.addAttribute("tavoloSelezionato", nomeTavolo);
    
    return "ordine-mobile";
}*/

// 2. Carica il menu per lo specifico tavolo selezionato
   /*  @GetMapping("/mobile/ordine/{nomeTavolo}")
    public String mostraMenu(@PathVariable String nomeTavolo, Model model) {
        model.addAttribute("tavoloSelezionato", nomeTavolo);
        model.addAttribute("prodotti", prodottoService.findAll());
        return "ordine-mobile"; // Il tuo HTML
    }*/

    // 3. Ricezione Ordine
    @PostMapping("/api/ordini/passa")
    @ResponseBody
    public ResponseEntity<String> riceviOrdine(
            @RequestBody Map<String, String> payload,
            HttpSession session) {
        if (!cameriereAutenticato(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Accesso cameriere richiesto");
        }

        // Memorizza ogni prodotto separatamente e aggiorna JavaFX sul suo thread grafico.
        String nomeTavolo = payload.get("tavolo");
        String prodotto = payload.get("prodotto");
        String prezzo = payload.get("prezzo");
        String categoria = payload.getOrDefault("categoria", "");
        String idComandaPayload = payload.get("comandaId");
        String idRigaPayload = payload.get("rigaId");
        String cameriere = (String) session.getAttribute(AutenticazioneService.SESSIONE_CAMERIERE);

        if (nomeTavolo == null || prodotto == null) {
            return ResponseEntity.badRequest().body("Dati dell'ordine mancanti");
        }

        UUID comandaId;
        UUID rigaId;
        try {
            comandaId = idComandaPayload == null || idComandaPayload.isBlank()
                    ? UUID.randomUUID()
                    : UUID.fromString(idComandaPayload);
            rigaId = idRigaPayload == null || idRigaPayload.isBlank()
                    ? UUID.randomUUID()
                    : UUID.fromString(idRigaPayload);
        } catch (IllegalArgumentException errore) {
            return ResponseEntity.badRequest().body("Identificativo ordine non valido");
        }
        if (categoria.length() > 120) {
            return ResponseEntity.badRequest().body("Categoria non valida");
        }

        BigDecimal prezzoOrdine;
        try {
            prezzoOrdine = new BigDecimal(prezzo == null ? "0.00" : prezzo);
        } catch (NumberFormatException errore) {
            return ResponseEntity.badRequest().body("Prezzo non valido");
        }

        ordineRicevutoService.registra(
                rigaId, comandaId, nomeTavolo, prodotto, prezzoOrdine, cameriere, categoria);

        if (DataHolder.ordiniMobileApplicatiInMemoria.add(rigaId)) {
            String prodottoConAutore = prodotto + "  ·  " + cameriere;

            DataHolder.memoriaProdotti
                    .computeIfAbsent(nomeTavolo, k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(prodottoConAutore);

            DataHolder.memoriaPrezzi
                    .computeIfAbsent(nomeTavolo, k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(prezzoOrdine.toPlainString());

            // Aggiornamento UI JavaFX (Eseguito sul thread grafico)
            try {
                Platform.runLater(() -> {
                    Button bottone = DataHolder.mappaBottoniTavoli.get(nomeTavolo);
                    if (bottone != null) {
                        bottone.setStyle("-fx-background-color: #ff4444; -fx-text-fill: white; -fx-font-weight: bold;");
                    }

                    if (DataHolder.vBoxDestinazioneprod != null
                            && nomeTavolo.equals(DataHolder.tavoloApertoAttualmente)) {
                        DataHolder.vBoxDestinazioneprod.getChildren().add(new Label(prodottoConAutore));
                        DataHolder.vBoxDestinazioneprezzo.getChildren().add(new Label(prezzoOrdine.toPlainString()));

                        if (DataHolder.labelTotaleDest != null) {
                            double attuale = Double.parseDouble(DataHolder.labelTotaleDest.getText());
                            DataHolder.labelTotaleDest.setText(String.valueOf(attuale + prezzoOrdine.doubleValue()));
                        }
                    }
                });
            } catch (IllegalStateException errore) {
                if (!"Toolkit not initialized".equals(errore.getMessage())) {
                    throw errore;
                }
            }
        }

        return ResponseEntity.ok("OK");
    }
}

/*@Controller
public class MobileOrderController {

    @Autowired
    private ProdottoRepository prodottoRepository;
    private Object tavoloCorrente;

    // Carica la pagina HTML con la lista prodotti
    @GetMapping("/mobile/ordine")
    public String mostraMenu(Model model) {
        model.addAttribute("prodotti", prodottoRepository.findAll());
        return "ordine-mobile";
    }

    // Salva l'ordine inviato tramite JavaScript (JSON)
    @PostMapping("/api/ordini/passa")
    @ResponseBody
   public String riceviOrdine(@RequestBody Map<String, String> payload) {

    
    String nomeTavolo = payload.get("tavolo");
    String prodotto = payload.get("prodotto");
    String prezzo = payload.get("prezzo");
    System.out.println("Payload ricevuto: " + payload);

    // Validazione base
    if (nomeTavolo == null || prodotto == null) return "ERRORE: Dati mancanti";

    // 1. Salva Prodotto (Sincronizzato)
    DataHolder.memoriaProdotti
        .computeIfAbsent(nomeTavolo, k -> Collections.synchronizedList(new ArrayList<>()))
        .add(prodotto);

    // 2. Salva Prezzo (Sincronizzato - IMPORTANTE: usa lo stesso pattern del prodotto)
    DataHolder.memoriaPrezzi
        .computeIfAbsent(nomeTavolo, k -> Collections.synchronizedList(new ArrayList<>()))
        .add(prezzo != null ? prezzo : "0.00");

    // Log di verifica
    System.out.println("Aggiunto " + prodotto + " a " + nomeTavolo);
    System.out.println("Lista Totale " + nomeTavolo + ": " + DataHolder.memoriaProdotti.get(nomeTavolo));

    // 3. Aggiornamento UI JavaFX
    Platform.runLater(() -> {
        Button bottone = DataHolder.mappaBottoniTavoli.get(nomeTavolo);
        if (bottone != null) {
            bottone.setStyle("-fx-background-color: #ff4444; -fx-text-fill: white; -fx-font-weight: bold;");
        }

        // Se hai un'area che mostra i prodotti in tempo reale
    /*     if (DataHolder.vBoxDestinazioneprod != null) {
            Label labelNuovoProdotto = new Label(prodotto );
            DataHolder.vBoxDestinazioneprod.getChildren().add(labelNuovoProdotto);
            Label labelNuovoPrezzo = new Label(prezzo );
            DataHolder.vBoxDestinazioneprezzo.getChildren().add(labelNuovoPrezzo);
        }
    });

    return "OK";
}
}*/