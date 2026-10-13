package myapp.controllers.Service;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import myapp.controllers.config.SessioneUtente;

@Service
public class CameriereService {
    private static final int ITERAZIONI_HASH = 210_000;
    private static final int DIMENSIONE_HASH = 256;
    private static final int DIMENSIONE_SALT = 16;

    private final JdbcTemplate jdbcTemplate;
    private final SecureRandom random = new SecureRandom();

    @Autowired
    public CameriereService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void inizializzaSchema() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS app_camerieri ("
                + "username VARCHAR(80) PRIMARY KEY, password_hash VARCHAR(255) NOT NULL)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS ordini_ricevuti ("
            + "id BIGSERIAL PRIMARY KEY, riga_id UUID NOT NULL DEFAULT gen_random_uuid(), "
            + "tavolo VARCHAR(120) NOT NULL, prodotto TEXT NOT NULL, "
                + "prezzo NUMERIC(10, 2) NOT NULL, cameriere VARCHAR(80) NOT NULL, "
            + "inviato_il TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + "comanda_id UUID NOT NULL DEFAULT gen_random_uuid(), categoria VARCHAR(120) NOT NULL DEFAULT '', "
                + "in_cucina BOOLEAN NOT NULL DEFAULT FALSE, in_bar BOOLEAN NOT NULL DEFAULT FALSE, "
                + "uscito BOOLEAN NOT NULL DEFAULT FALSE, annullata BOOLEAN NOT NULL DEFAULT FALSE, "
                + "pagata BOOLEAN NOT NULL DEFAULT FALSE, "
            + "evasa BOOLEAN NOT NULL DEFAULT FALSE)");
        jdbcTemplate.execute("ALTER TABLE ordini_ricevuti ADD COLUMN IF NOT EXISTS comanda_id UUID");
        jdbcTemplate.execute("ALTER TABLE ordini_ricevuti ADD COLUMN IF NOT EXISTS riga_id UUID");
        jdbcTemplate.execute("ALTER TABLE ordini_ricevuti ADD COLUMN IF NOT EXISTS categoria VARCHAR(120) DEFAULT ''");
        jdbcTemplate.execute("ALTER TABLE ordini_ricevuti ADD COLUMN IF NOT EXISTS in_cucina BOOLEAN NOT NULL DEFAULT FALSE");
        jdbcTemplate.execute("ALTER TABLE ordini_ricevuti ADD COLUMN IF NOT EXISTS in_bar BOOLEAN NOT NULL DEFAULT FALSE");
        jdbcTemplate.execute("ALTER TABLE ordini_ricevuti ADD COLUMN IF NOT EXISTS uscito BOOLEAN NOT NULL DEFAULT FALSE");
        jdbcTemplate.execute("ALTER TABLE ordini_ricevuti ADD COLUMN IF NOT EXISTS annullata BOOLEAN NOT NULL DEFAULT FALSE");
        jdbcTemplate.execute("ALTER TABLE ordini_ricevuti ADD COLUMN IF NOT EXISTS pagata BOOLEAN NOT NULL DEFAULT FALSE");
        jdbcTemplate.execute("ALTER TABLE ordini_ricevuti ADD COLUMN IF NOT EXISTS evasa BOOLEAN NOT NULL DEFAULT FALSE");
        jdbcTemplate.execute("UPDATE ordini_ricevuti SET comanda_id = gen_random_uuid() WHERE comanda_id IS NULL");
        jdbcTemplate.execute("ALTER TABLE ordini_ricevuti ALTER COLUMN comanda_id SET DEFAULT gen_random_uuid()");
        jdbcTemplate.execute("ALTER TABLE ordini_ricevuti ALTER COLUMN comanda_id SET NOT NULL");
        jdbcTemplate.execute("UPDATE ordini_ricevuti SET riga_id = gen_random_uuid() WHERE riga_id IS NULL");
        jdbcTemplate.execute("ALTER TABLE ordini_ricevuti ALTER COLUMN riga_id SET DEFAULT gen_random_uuid()");
        jdbcTemplate.execute("ALTER TABLE ordini_ricevuti ALTER COLUMN riga_id SET NOT NULL");
        jdbcTemplate.execute("ALTER TABLE ordini_ricevuti ALTER COLUMN in_cucina SET DEFAULT FALSE");
        jdbcTemplate.execute("UPDATE ordini_ricevuti SET in_cucina = FALSE WHERE categoria IS NULL OR btrim(categoria) = ''");
        jdbcTemplate.execute("UPDATE ordini_ricevuti SET in_bar = TRUE "
            + "WHERE in_bar = FALSE AND in_cucina = FALSE AND btrim(categoria) <> ''");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_ordini_cucina_aperti "
            + "ON ordini_ricevuti (in_cucina, evasa, inviato_il)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_ordini_bar_aperti "
            + "ON ordini_ricevuti (in_bar, evasa, inviato_il)");
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_ordini_riga_id "
            + "ON ordini_ricevuti (riga_id)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS pagamenti_conti ("
            + "id BIGSERIAL PRIMARY KEY, tavolo VARCHAR(120) NOT NULL, importo NUMERIC(10, 2) NOT NULL, "
            + "metodo VARCHAR(30) NOT NULL, operatore VARCHAR(80), pagato_il TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP)");
    }

    public List<String> elencoCamerieri() {
        SessioneUtente.richiediAmministratore();
        return jdbcTemplate.queryForList(
                "SELECT username FROM app_camerieri ORDER BY username",
                String.class);
    }

    public boolean aggiungiCameriere(String username, String password) {
        SessioneUtente.richiediAmministratore();
        String nomeUtente = normalizzaUsername(username);
        if (nomeUtente.isEmpty() || nomeUtente.length() > 80) {
            throw new IllegalArgumentException("Inserisci uno username valido (massimo 80 caratteri).");
        }
        if ("admin".equals(nomeUtente) || "cassiere".equals(nomeUtente)) {
            throw new IllegalArgumentException("Questo username è riservato.");
        }
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("La password deve contenere almeno 8 caratteri.");
        }

        try {
            return jdbcTemplate.update(
                    "INSERT INTO app_camerieri (username, password_hash) VALUES (?, ?)",
                    nomeUtente,
                    creaHashPassword(password)) == 1;
        } catch (DuplicateKeyException errore) {
            return false;
        }
    }

    public boolean eliminaCameriere(String username) {
        SessioneUtente.richiediAmministratore();
        return jdbcTemplate.update(
                "DELETE FROM app_camerieri WHERE username = ?",
                normalizzaUsername(username)) == 1;
    }

    public boolean autenticaCameriere(String username, String password) {
        if (username == null || password == null) {
            return false;
        }

        String hash;
        try {
            hash = jdbcTemplate.queryForObject(
                    "SELECT password_hash FROM app_camerieri WHERE username = ?",
                    String.class,
                    normalizzaUsername(username));
        } catch (EmptyResultDataAccessException errore) {
            return false;
        }
        return verificaPassword(password, hash);
    }

    private String normalizzaUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    String creaHashPassword(String password) {
        byte[] salt = new byte[DIMENSIONE_SALT];
        random.nextBytes(salt);
        byte[] hash = calcolaHash(password, salt, ITERAZIONI_HASH);
        return "pbkdf2$" + ITERAZIONI_HASH + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    static boolean verificaPassword(String password, String valoreSalvato) {
        try {
            String[] parti = valoreSalvato.split("\\$", -1);
            if (parti.length != 4 || !"pbkdf2".equals(parti[0])) {
                return false;
            }
            int iterazioni = Integer.parseInt(parti[1]);
            byte[] salt = Base64.getDecoder().decode(parti[2]);
            byte[] hashSalvato = Base64.getDecoder().decode(parti[3]);
            byte[] hashInserito = calcolaHash(password, salt, iterazioni);
            return java.security.MessageDigest.isEqual(hashSalvato, hashInserito);
        } catch (IllegalArgumentException errore) {
            return false;
        }
    }

    private static byte[] calcolaHash(String password, byte[] salt, int iterazioni) {
        PBEKeySpec specifica = new PBEKeySpec(password.toCharArray(), salt, iterazioni, DIMENSIONE_HASH);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(specifica)
                    .getEncoded();
        } catch (GeneralSecurityException errore) {
            throw new IllegalStateException("Impossibile proteggere la password del cameriere.", errore);
        } finally {
            specifica.clearPassword();
        }
    }
}