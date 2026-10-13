package myapp.controllers.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import myapp.controllers.config.SessioneUtente;


@Service
@Transactional
public class ProdottoService {

    // Coordina accesso ai prodotti e query sulle tabelle create dagli utenti.
    @Autowired
    private ProdottoRepository repository;

     @PersistenceContext
    private EntityManager entityManager;

    public void deletePerNome(String nome) {
        SessioneUtente.richiediAmministratore();
        repository.deleteByNome(nome);
    }
    
     public List<Prodotto> trovaDaTabella(String nomeTabella) {
        // Mappa il risultato della query dinamica sul modello usato dalla TableView JavaFX.
        ColumnNames columns = findProductColumns(nomeTabella);
        String sql = "SELECT " + quoteIdentifier(columns.nameColumn()) + " AS nome, "
            + quoteIdentifier(columns.priceColumn()) + " AS prezzo FROM " + quoteIdentifier(nomeTabella);
        
        List<Object[]> results = entityManager.createNativeQuery(sql).getResultList();
        
        return results.stream()
            .map(row -> new Prodotto((String) row[0], ((Number) row[1]).doubleValue()))
                .toList();
    }
  @Transactional
      public void inserisciProdotto(String nomeTabella, String nome, Float prezzo) {
        SessioneUtente.richiediAmministratore();
        // Parametrizza i valori; solo gli identificatori validati vengono composti nella query SQL.
        ColumnNames columns = findProductColumns(nomeTabella);
        String sql = "INSERT INTO " + quoteIdentifier(nomeTabella)
            + " (" + quoteIdentifier(columns.nameColumn()) + ", "
            + quoteIdentifier(columns.priceColumn()) + ") VALUES (:nome, :prezzo)";
        
        entityManager.createNativeQuery(sql)
                .setParameter("nome", nome)
                .setParameter("prezzo", prezzo)
                .executeUpdate();
    }

    @Transactional
    public void eliminaTabella(String nomeTabella) {
        SessioneUtente.richiediAmministratore();
        // Elimina lo schema selezionato dopo aver validato il suo identificatore.
        String sql = "DROP TABLE IF EXISTS " + quoteIdentifier(nomeTabella);
        entityManager.createNativeQuery(sql).executeUpdate();
    }

    private String quoteIdentifier(String value) {
        if (value == null || !value.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Nome tabella non valido");
        }
        return "\"" + value + "\"";
    }

    private ColumnNames findProductColumns(String tableName) {
        // Cerca nello schema pubblico la prima colonna testuale e quella numerica compatibile col menu.
        String validTableName = quoteIdentifier(tableName).substring(1, tableName.length() + 1);
        String sql = "SELECT column_name, data_type FROM information_schema.columns "
                + "WHERE table_schema = 'public' AND table_name = :tableName "
                + "ORDER BY ordinal_position";

        List<Object[]> columns = entityManager.createNativeQuery(sql)
                .setParameter("tableName", validTableName)
                .getResultList();

        String nameColumn = null;
        String priceColumn = null;
        for (Object[] column : columns) {
            String columnName = (String) column[0];
            String dataType = (String) column[1];

            if (nameColumn == null && ("character varying".equals(dataType) || "text".equals(dataType))) {
                nameColumn = columnName;
            }
            if (priceColumn == null && ("real".equals(dataType)
                    || "double precision".equals(dataType)
                    || "numeric".equals(dataType))) {
                priceColumn = columnName;
            }
        }

        if (nameColumn == null || priceColumn == null) {
            throw new IllegalArgumentException(
                    "La tabella " + tableName + " deve avere una colonna testuale e una colonna numerica");
        }
        return new ColumnNames(nameColumn, priceColumn);
    }

    private record ColumnNames(String nameColumn, String priceColumn) {
    }

    @Transactional
    public List<Prodotto> findAll() {
        return repository.findAll();
    }       
}
