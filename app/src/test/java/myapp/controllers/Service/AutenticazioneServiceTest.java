package myapp.controllers.Service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import myapp.controllers.config.RuoloUtente;
import myapp.controllers.config.SessioneUtente;

class AutenticazioneServiceTest {
    private final CameriereService cameriereService = new CameriereService(null) {
        @Override
        public boolean autenticaCameriere(String username, String password) {
            return "mario".equalsIgnoreCase(username) && "password-sicura".equals(password);
        }
    };
    private final AutenticazioneService autenticazioneService = new AutenticazioneService(cameriereService);

    @Test
    void autenticaAmministratoreECassiere() {
        assertEquals(RuoloUtente.AMMINISTRATORE,
                autenticazioneService.autenticaDesktop("admin", "1234"));
        assertEquals(RuoloUtente.CASSIERE,
                autenticazioneService.autenticaDesktop("cassiere", "1234"));
    }

    @Test
    void rifiutaCredenzialiDesktopNonValide() {
        assertNull(autenticazioneService.autenticaDesktop("admin", "errata"));
        assertNull(autenticazioneService.autenticaDesktop("utente", "1234"));
    }

    @Test
    void autenticaAccountCameriereSulMobile() {
        assertTrue(autenticazioneService.autenticaCameriere("mario", "password-sicura"));
        assertFalse(autenticazioneService.autenticaCameriere("mario", "errata"));
        assertFalse(autenticazioneService.autenticaCameriere("admin", "1234"));
    }

    @Test
    void abilitaIlLoginDesktopPerICamerieri() {
        assertEquals(RuoloUtente.CAMERIERE,
                autenticazioneService.autenticaDesktop("mario", "password-sicura"));
    }

    @Test
    void proteggeLePasswordConHashVerificabili() {
        CameriereService servizio = new CameriereService(null);
        String hash = servizio.creaHashPassword("password-sicura");

        assertTrue(CameriereService.verificaPassword("password-sicura", hash));
        assertFalse(CameriereService.verificaPassword("password-errata", hash));
    }

    @Test
    void soloAmministratorePuoEseguireOperazioniDatabase() {
        SessioneUtente.avvia("cassiere", RuoloUtente.CASSIERE);
        assertThrows(SecurityException.class, SessioneUtente::richiediAmministratore);

        SessioneUtente.avvia("admin", RuoloUtente.AMMINISTRATORE);
        assertDoesNotThrow(SessioneUtente::richiediAmministratore);
    }

    @Test
    void logoutCancellaUtenteERuolo() {
        SessioneUtente.avvia("admin", RuoloUtente.AMMINISTRATORE);

        SessioneUtente.termina();

        assertNull(SessioneUtente.getUsername());
        assertFalse(SessioneUtente.isAmministratore());
    }
}