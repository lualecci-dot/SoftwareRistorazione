package myapp.controllers.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import myapp.controllers.config.SessioneUtente;
@Service
public class DatabaseService {
    private static final Set<String> TABELLE_DI_SERVIZIO = Set.of(
            "ordini", "tavoli", "app_camerieri", "ordini_ricevuti", "pagamenti_conti");

    // Operazioni di schema eseguite con query native tramite l'EntityManager.
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void creaNuovaTabella(String nomeTabella, String colonnaNome, String colonnaPrezzo) {
        SessioneUtente.richiediAmministratore();
        // Il formato dei nomi ammessi impedisce di inserire frammenti SQL nell'identificatore.
        String tabella = sanitizeIdentifier(nomeTabella);
        String sql = String.format(
            "CREATE TABLE IF NOT EXISTS \"%s\" (nome VARCHAR(255), prezzo FLOAT)",
            tabella
        );

        entityManager.createNativeQuery(sql).executeUpdate();
    }

    private String sanitizeIdentifier(String value) {
        // Gli identificatori SQL non possono essere passati come parametri: accetta solo lettere, cifre e '_'.
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Il nome della tabella è obbligatorio");
        }

        String cleaned = value.trim();
        if (!cleaned.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException(
                "Il nome della tabella può contenere solo lettere, numeri e underscore"
            );
        }
        return cleaned;
    }

    @Transactional
    public static List<String> recuperaNomiTabelle(EntityManager entityManager) {
    String sql = "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'";
    List<String> nomiTabelle = entityManager.createNativeQuery(sql).getResultList();
    return nomiTabelle.stream().filter(DatabaseService::isTabellaProdotto).toList();
    }

    public static boolean isTabellaProdotto(String nomeTabella) {
        return nomeTabella != null
                && !TABELLE_DI_SERVIZIO.contains(nomeTabella.toLowerCase(Locale.ROOT));
}
}