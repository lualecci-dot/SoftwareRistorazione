package myapp.controllers.config;

public final class SessioneUtente {
    private static volatile String username;
    private static volatile RuoloUtente ruolo;

    private SessioneUtente() {
    }

    public static void avvia(String nomeUtente, RuoloUtente ruoloUtente) {
        username = nomeUtente;
        ruolo = ruoloUtente;
    }

    public static boolean isAmministratore() {
        return ruolo == RuoloUtente.AMMINISTRATORE;
    }

    public static void richiediAmministratore() {
        if (!isAmministratore()) {
            throw new SecurityException("Operazione riservata all'amministratore.");
        }
    }

    public static String getUsername() {
        return username;
    }

    public static void termina() {
        username = null;
        ruolo = null;
    }
}