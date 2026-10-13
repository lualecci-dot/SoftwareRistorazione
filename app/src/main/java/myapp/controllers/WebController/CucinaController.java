package myapp.controllers.WebController;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import myapp.controllers.Service.OrdineRicevutoService;
import myapp.controllers.Service.OrdineRicevutoService.ComandaCucina;

@Controller
public class CucinaController {
    private final OrdineRicevutoService ordineRicevutoService;

    @Autowired
    public CucinaController(OrdineRicevutoService ordineRicevutoService) {
        this.ordineRicevutoService = ordineRicevutoService;
    }

    @GetMapping("/cucina")
    public String paginaCucina(Model model) {
        configuraPagina(model, "Cucina", "/api/cucina");
        return "cucina";
    }

    @GetMapping("/bar")
    public String paginaBar(Model model) {
        configuraPagina(model, "Bar", "/api/bar");
        return "cucina";
    }

    private void configuraPagina(Model model, String reparto, String apiBase) {
        model.addAttribute("reparto", reparto);
        model.addAttribute("apiBase", apiBase);
    }

    @GetMapping("/api/cucina/comande")
    @ResponseBody
    public List<ComandaCucina> comandeAperte() {
        return ordineRicevutoService.comandeAperte();
    }

    @GetMapping("/api/bar/comande")
    @ResponseBody
    public List<ComandaCucina> comandeBarAperte() {
        return ordineRicevutoService.comandeBarAperte();
    }

    @PostMapping("/api/cucina/prodotti/{id}/uscito")
    @ResponseBody
    public ResponseEntity<Void> segnaProdottoUscito(@PathVariable long id) {
        return ordineRicevutoService.segnaProdottoUscito(id)
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }

    @PostMapping("/api/bar/prodotti/{id}/uscito")
    @ResponseBody
    public ResponseEntity<Void> segnaBevandaUscita(@PathVariable long id) {
        return ordineRicevutoService.segnaBevandaUscita(id)
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }

    @PostMapping("/api/cucina/comande/{id}/evadi")
    @ResponseBody
    public ResponseEntity<Void> evadiComanda(@PathVariable UUID id) {
        return ordineRicevutoService.evadiComanda(id)
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }

    @PostMapping("/api/bar/comande/{id}/evadi")
    @ResponseBody
    public ResponseEntity<Void> evadiComandaBar(@PathVariable UUID id) {
        return ordineRicevutoService.evadiComandaBar(id)
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }
}