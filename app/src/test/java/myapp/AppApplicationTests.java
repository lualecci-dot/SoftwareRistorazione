package myapp;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import myapp.controllers.Holder.DataHolder;
import myapp.controllers.Service.AutenticazioneService;
import myapp.controllers.Service.CameriereService;
import myapp.controllers.Service.DatabaseService;
import myapp.controllers.Service.OrdineRicevutoService;
import myapp.controllers.Service.OrdineRicevutoService.RigaDaInviare;
import myapp.controllers.config.RuoloUtente;
import myapp.controllers.config.SessioneUtente;

@SpringBootTest
@AutoConfigureMockMvc
class AppApplicationTests {
	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private CameriereService cameriereService;
	@Autowired
	private AutenticazioneService autenticazioneService;
	@Autowired
	private OrdineRicevutoService ordineRicevutoService;

	// Verifica che il contesto Spring dell'applicazione riesca ad avviarsi nel test.
	@Test
	void contextLoads() {
	}

	@Test
	void escludeLaTabellaPagamentiDagliArchiviProdotto() {
		assertFalse(DatabaseService.isTabellaProdotto("pagamenti_conti"));
		assertFalse(DatabaseService.isTabellaProdotto("PAGAMENTI_CONTI"));
		assertTrue(DatabaseService.isTabellaProdotto("bibite"));
	}

	@Test
	void paginaCucinaERestituisceLaCodaJson() throws Exception {
		mockMvc.perform(get("/cucina"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Evadi comanda")));
		mockMvc.perform(get("/bar"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Bar")));

		mockMvc.perform(get("/api/cucina/comande"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());
		mockMvc.perform(get("/api/bar/comande"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());
	}

	@Test
	void paginaOrdineMostraIlRetryEIlReinvioNonDuplicaLeRighe() throws Exception {
		String tavolo = "Retry " + UUID.randomUUID();
		UUID comandaId = UUID.randomUUID();
		UUID rigaId = UUID.randomUUID();
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(AutenticazioneService.SESSIONE_CAMERIERE, "test-cameriere");
		String payload = "{\"tavolo\":\"" + tavolo + "\",\"prodotto\":\"Pizza retry\","
				+ "\"prezzo\":\"8.50\",\"categoria\":\"cucina\","
				+ "\"comandaId\":\"" + comandaId + "\",\"rigaId\":\"" + rigaId + "\"}";

		try {
			mockMvc.perform(get("/mobile/ordine/{tavolo}", "Tavolo 1").session(session))
					.andExpect(status().isOk())
					.andExpect(content().string(containsString("riprova-ordine")));

			for (int tentativo = 0; tentativo < 2; tentativo++) {
				mockMvc.perform(post("/api/ordini/passa")
						.session(session)
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
						.andExpect(status().isOk());
			}

			Integer righeSalvate = jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM ordini_ricevuti WHERE riga_id = ?",
					Integer.class,
					rigaId);
			assertEquals(1, righeSalvate);
			assertEquals(1, DataHolder.memoriaProdotti.get(tavolo).size());
		} finally {
			jdbcTemplate.update("DELETE FROM ordini_ricevuti WHERE riga_id = ?", rigaId);
			DataHolder.memoriaProdotti.remove(tavolo);
			DataHolder.memoriaPrezzi.remove(tavolo);
			DataHolder.ordiniMobileApplicatiInMemoria.remove(rigaId);
		}
	}

	@Test
	void listaMobileSegnalaTavoliOccupati() throws Exception {
		String tavolo = "Tavolo 1";
		var prodottiPrecedenti = DataHolder.memoriaProdotti.put(tavolo, java.util.List.of("Pizza test"));
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(AutenticazioneService.SESSIONE_CAMERIERE, "test-cameriere");

		try {
			mockMvc.perform(get("/mobile/tavoli").session(session))
					.andExpect(status().isOk())
					.andExpect(content().string(containsString("Occupato")));
		} finally {
			if (prodottiPrecedenti == null) {
				DataHolder.memoriaProdotti.remove(tavolo);
			} else {
				DataHolder.memoriaProdotti.put(tavolo, prodottiPrecedenti);
			}
		}
	}

	@Test
	void annullareIlContoRimuoveGliOrdiniAncoraAperti() {
		String tavolo = "Test annulla " + UUID.randomUUID();
		String operatore = "test-annulla";
		UUID comandaId = ordineRicevutoService.registraComanda(tavolo, operatore, List.of(
				new RigaDaInviare("Pizza annullata", new BigDecimal("8.00"), "cucina"),
				new RigaDaInviare("Acqua annullata", new BigDecimal("2.00"), "bere7bar")));

		try {
			assertTrue(ordineRicevutoService.comandeAperte().stream()
					.anyMatch(comanda -> comanda.id().equals(comandaId)));
			assertTrue(ordineRicevutoService.comandeBarAperte().stream()
					.anyMatch(comanda -> comanda.id().equals(comandaId)));

			assertEquals(2, ordineRicevutoService.annullaConto(tavolo));
			assertFalse(ordineRicevutoService.comandeAperte().stream()
					.anyMatch(comanda -> comanda.id().equals(comandaId)));
			assertFalse(ordineRicevutoService.comandeBarAperte().stream()
					.anyMatch(comanda -> comanda.id().equals(comandaId)));
		} finally {
			jdbcTemplate.update("DELETE FROM ordini_ricevuti WHERE tavolo = ?", tavolo);
		}
	}

	@Test
	void pagamentoRegistraImportoEMetodoSenzaFermareLaPreparazione() {
		String tavolo = "Test pagamento " + UUID.randomUUID();
		String operatore = "test-pagamento";
		UUID comandaId = ordineRicevutoService.registraComanda(tavolo, operatore, List.of(
				new RigaDaInviare("Pasta pagata", new BigDecimal("12.50"), "cucina")));

		try {
			ordineRicevutoService.registraPagamento(tavolo, new BigDecimal("12.50"), "Contanti", operatore);

			Integer pagamenti = jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM pagamenti_conti WHERE tavolo = ? AND importo = ? AND metodo = ?",
					Integer.class,
					tavolo,
					new BigDecimal("12.50"),
					"Contanti");
			assertEquals(1, pagamenti);
			assertTrue(ordineRicevutoService.comandeAperte().stream()
					.anyMatch(comanda -> comanda.id().equals(comandaId)));
		} finally {
			jdbcTemplate.update("DELETE FROM pagamenti_conti WHERE tavolo = ?", tavolo);
			jdbcTemplate.update("DELETE FROM ordini_ricevuti WHERE tavolo = ?", tavolo);
		}
	}

	@Test
	void creaAutenticaERegistraOrdiniConIlCameriereCorretto() {
		String username = "test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
		String password = "password-di-test";
		SessioneUtente.avvia("admin", RuoloUtente.AMMINISTRATORE);

		try {
			assertTrue(cameriereService.aggiungiCameriere(username, password));
			assertTrue(autenticazioneService.autenticaCameriere(username, password));
			assertEquals(RuoloUtente.CAMERIERE, autenticazioneService.autenticaDesktop(username, password));
			assertTrue(cameriereService.elencoCamerieri().contains(username));

			UUID comandaId = ordineRicevutoService.registraComanda("Tavolo test", username, List.of(
					new RigaDaInviare("Prodotto test", new BigDecimal("3.50"), "cucina"),
					new RigaDaInviare("Acqua test", new BigDecimal("1.50"), "bere7bar")));
			Integer ordiniRegistrati = jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM ordini_ricevuti WHERE tavolo = ? AND prodotto = ? AND cameriere = ?",
					Integer.class,
					"Tavolo test",
					"Prodotto test",
					username);
			assertEquals(1, ordiniRegistrati);
			var comandaCucina = ordineRicevutoService.comandeAperte().stream()
					.filter(comanda -> comanda.id().equals(comandaId))
					.findFirst()
					.orElseThrow();
			assertEquals(1, comandaCucina.prodotti().size());
			assertEquals("Prodotto test", comandaCucina.prodotti().get(0).nome());
			var comandaBar = ordineRicevutoService.comandeBarAperte().stream()
					.filter(comanda -> comanda.id().equals(comandaId))
					.findFirst()
					.orElseThrow();
			assertEquals(1, comandaBar.prodotti().size());
			assertEquals("Acqua test", comandaBar.prodotti().get(0).nome());

			long prodottoId = comandaCucina.prodotti().get(0).id();
			assertTrue(ordineRicevutoService.segnaProdottoUscito(prodottoId));
			assertTrue(ordineRicevutoService.comandeAperte().stream()
					.filter(comanda -> comanda.id().equals(comandaId))
					.findFirst()
					.orElseThrow()
					.prodotti().get(0).uscito());
			assertTrue(ordineRicevutoService.evadiComanda(comandaId));
			assertFalse(ordineRicevutoService.comandeAperte().stream()
					.anyMatch(comanda -> comanda.id().equals(comandaId)));
			long bevandaId = comandaBar.prodotti().get(0).id();
			assertTrue(ordineRicevutoService.segnaBevandaUscita(bevandaId));
			assertTrue(ordineRicevutoService.evadiComandaBar(comandaId));
			assertFalse(ordineRicevutoService.comandeBarAperte().stream()
					.anyMatch(comanda -> comanda.id().equals(comandaId)));

			assertTrue(cameriereService.eliminaCameriere(username));
			assertFalse(autenticazioneService.autenticaCameriere(username, password));
		} finally {
			jdbcTemplate.update("DELETE FROM ordini_ricevuti WHERE cameriere = ?", username);
			jdbcTemplate.update("DELETE FROM app_camerieri WHERE username = ?", username);
			SessioneUtente.termina();
		}
	}

}
