package myapp.controllers.Service;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import myapp.controllers.config.RuoloUtente;

@Service
public class AutenticazioneService {
    public static final String SESSIONE_CAMERIERE = "utenteCameriereAutenticato";
    private static final String PASSWORD_LOCALE = "1234";
    private final CameriereService cameriereService;

    @Autowired
    public AutenticazioneService(CameriereService cameriereService) {
        this.cameriereService = cameriereService;
    }

    public RuoloUtente autenticaDesktop(String username, String password) {
        if (username == null || password == null) {
            return null;
        }

        String nomeUtente = username.trim().toLowerCase(Locale.ROOT);
        if (PASSWORD_LOCALE.equals(password)) {
            RuoloUtente ruoloFisso = switch (nomeUtente) {
                case "admin" -> RuoloUtente.AMMINISTRATORE;
                case "cassiere" -> RuoloUtente.CASSIERE;
                default -> null;
            };
            if (ruoloFisso != null) {
                return ruoloFisso;
            }
        }

        return cameriereService.autenticaCameriere(nomeUtente, password)
                ? RuoloUtente.CAMERIERE
                : null;
    }

    public boolean autenticaCameriere(String username, String password) {
        return cameriereService.autenticaCameriere(username, password);
    }
}