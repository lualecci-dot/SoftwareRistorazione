package myapp.controllers.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrdineRicevutoService {
    private static final Pattern SEGNI_DIACRITICI = Pattern.compile("\\p{M}+");
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public OrdineRicevutoService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean registra(String tavolo, String prodotto, BigDecimal prezzo, String cameriere) {
        return registra(UUID.randomUUID(), UUID.randomUUID(), tavolo, prodotto, prezzo, cameriere, "");
        }

        public boolean registra(
            UUID comandaId,
            String tavolo,
            String prodotto,
            BigDecimal prezzo,
            String cameriere,
            String categoria) {
            return registra(UUID.randomUUID(), comandaId, tavolo, prodotto, prezzo, cameriere, categoria);
        }

        public boolean registra(
            UUID rigaId,
            UUID comandaId,
            String tavolo,
            String prodotto,
            BigDecimal prezzo,
            String cameriere,
            String categoria) {
        String categoriaOrdine = categoria == null ? "" : categoria.trim();
            boolean bevanda = !categoriaOrdine.isBlank() && categoriaEBevanda(categoriaOrdine);
        return jdbcTemplate.update(
            "INSERT INTO ordini_ricevuti "
                    + "(riga_id, comanda_id, tavolo, prodotto, prezzo, cameriere, categoria, in_cucina, in_bar) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (riga_id) DO NOTHING",
                rigaId,
            comandaId,
                tavolo,
                prodotto,
                prezzo,
            cameriere,
                categoriaOrdine,
                !categoriaOrdine.isBlank() && !bevanda,
                bevanda) == 1;
        }

        @Transactional
        public UUID registraComanda(String tavolo, String cameriere, List<RigaDaInviare> righe) {
            if (righe == null || righe.isEmpty()) {
                return null;
            }
            UUID comandaId = UUID.randomUUID();
            for (RigaDaInviare riga : righe) {
                registra(comandaId, tavolo, riga.nome(), riga.prezzo(), cameriere, riga.categoria());
            }
            return comandaId;
        }

        public List<ComandaCucina> comandeAperte() {
            return caricaComande(true);
            }

            public List<ComandaCucina> comandeBarAperte() {
            return caricaComande(false);
            }

            private List<ComandaCucina> caricaComande(boolean cucina) {
            String colonnaReparto = cucina ? "in_cucina" : "in_bar";
        List<RigaOrdine> righe = jdbcTemplate.query(
            "SELECT id, comanda_id, tavolo, prodotto, prezzo, categoria, cameriere, inviato_il, uscito "
                + "FROM ordini_ricevuti WHERE " + colonnaReparto + " = TRUE "
                + "AND evasa = FALSE AND annullata = FALSE "
                + "ORDER BY inviato_il, id",
            (resultSet, rowNumber) -> new RigaOrdine(
                resultSet.getLong("id"),
                resultSet.getObject("comanda_id", UUID.class),
                resultSet.getString("tavolo"),
                resultSet.getString("prodotto"),
                resultSet.getBigDecimal("prezzo"),
                resultSet.getString("categoria"),
                resultSet.getString("cameriere"),
                resultSet.getTimestamp("inviato_il").toLocalDateTime(),
                resultSet.getBoolean("uscito")));

        Map<UUID, ComandaInCostruzione> comande = new LinkedHashMap<>();
        for (RigaOrdine riga : righe) {
            ComandaInCostruzione comanda = comande.computeIfAbsent(
                riga.comandaId(),
                id -> new ComandaInCostruzione(id, riga.tavolo(), riga.cameriere(), riga.inviatoIl()));
            comanda.prodotti().add(new ProdottoCucina(
                riga.id(), riga.prodotto(), riga.prezzo(), riga.categoria(), riga.uscito()));
        }

        return comande.values().stream()
            .map(comanda -> new ComandaCucina(
                comanda.id(),
                comanda.tavolo(),
                comanda.cameriere(),
                comanda.ricevutaIl(),
                List.copyOf(comanda.prodotti())))
            .toList();
        }

        public boolean segnaProdottoUscito(long prodottoId) {
            return aggiornaProdottoUscito(prodottoId, "in_cucina");
        }

        public boolean segnaBevandaUscita(long prodottoId) {
            return aggiornaProdottoUscito(prodottoId, "in_bar");
        }

        private boolean aggiornaProdottoUscito(long prodottoId, String colonnaReparto) {
        return jdbcTemplate.update(
            "UPDATE ordini_ricevuti SET uscito = TRUE "
                + "WHERE id = ? AND " + colonnaReparto + " = TRUE "
                + "AND evasa = FALSE AND annullata = FALSE",
            prodottoId) == 1;
        }

        public boolean evadiComanda(UUID comandaId) {
            return evadiComanda(comandaId, "in_cucina");
        }

        public boolean evadiComandaBar(UUID comandaId) {
            return evadiComanda(comandaId, "in_bar");
        }

        private boolean evadiComanda(UUID comandaId, String colonnaReparto) {
        return jdbcTemplate.update(
            "UPDATE ordini_ricevuti SET uscito = TRUE, evasa = TRUE "
                + "WHERE comanda_id = ? AND " + colonnaReparto + " = TRUE "
                + "AND evasa = FALSE AND annullata = FALSE",
            comandaId) > 0;
        }

        @Transactional
        public int annullaConto(String tavolo) {
        return jdbcTemplate.update(
            "UPDATE ordini_ricevuti SET annullata = TRUE "
                + "WHERE tavolo = ? AND pagata = FALSE AND annullata = FALSE",
            tavolo);
        }

        @Transactional
        public void registraPagamento(String tavolo, BigDecimal importo, String metodo, String operatore) {
        jdbcTemplate.update(
            "INSERT INTO pagamenti_conti (tavolo, importo, metodo, operatore) VALUES (?, ?, ?, ?)",
            tavolo,
            importo,
            metodo,
            operatore);
        jdbcTemplate.update(
            "UPDATE ordini_ricevuti SET pagata = TRUE "
                + "WHERE tavolo = ? AND pagata = FALSE AND annullata = FALSE",
            tavolo);
        }

        static boolean categoriaEBevanda(String categoria) {
        String normalizzata = SEGNI_DIACRITICI.matcher(Normalizer.normalize(
            categoria == null ? "" : categoria, Normalizer.Form.NFD)).replaceAll("")
            .toLowerCase(Locale.ROOT);
        String compatta = normalizzata.replaceAll("[^a-z0-9]+", "");
        boolean categoriaTe = List.of(normalizzata.split("[^a-z0-9]+"))
            .stream()
            .anyMatch("te"::equals);
        return List.of("bar", "bere", "bev", "drink", "bibit", "birr", "vin", "acqu", "cocktail",
            "caffe", "succh", "softdrink").stream().anyMatch(compatta::contains) || categoriaTe;
        }

        private record RigaOrdine(
            long id,
            UUID comandaId,
            String tavolo,
            String prodotto,
            BigDecimal prezzo,
            String categoria,
            String cameriere,
            LocalDateTime inviatoIl,
            boolean uscito) {
        }

        private record ComandaInCostruzione(
            UUID id,
            String tavolo,
            String cameriere,
            LocalDateTime ricevutaIl,
            List<ProdottoCucina> prodotti) {
        private ComandaInCostruzione(UUID id, String tavolo, String cameriere, LocalDateTime ricevutaIl) {
            this(id, tavolo, cameriere, ricevutaIl, new ArrayList<>());
        }
        }

        public record ProdottoCucina(long id, String nome, BigDecimal prezzo, String categoria, boolean uscito) {
        }

        public record RigaDaInviare(String nome, BigDecimal prezzo, String categoria) {
        }

        public record ComandaCucina(
            UUID id,
            String tavolo,
            String cameriere,
            LocalDateTime ricevutaIl,
            List<ProdottoCucina> prodotti) {
    }
}