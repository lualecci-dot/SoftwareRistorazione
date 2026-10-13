MYAPP - GESTIONALE PER RISTORAZIONE
===================================

1. PANORAMICA
-------------
MyApp e un gestionale desktop per la cassa, con server web integrato per gli
ordini da telefono e per le postazioni Cucina e Bar. Il progetto usa JavaFX per
l'interfaccia desktop, Spring Boot per i servizi web e PostgreSQL per i dati.

Funzioni principali:
- Login desktop per amministratore, cassiere e camerieri.
- Gestione degli archivi prodotto e degli account camerieri.
- Inserimento degli articoli sui tavoli dalla cassa.
- Invio degli ordini mobile a cucina o bar in base alla categoria dell'archivio.
- Ritentativo dell'invio mobile in caso di connessione interrotta, senza duplicare
  gli articoli gia ricevuti.
- Stato occupato dei tavoli sulla pagina mobile.
- Coda web Cucina e coda web Bar, con selezione delle comande, stato prodotto
  uscito ed evasione della comanda.
- Annullamento del conto, registrazione del pagamento e stampa/anteprima dello
  scontrino.

Il flusso cassa e intenzionalmente differito: gli articoli aggiunti dalla cassa
sono accodati al tavolo e vengono inviati ai reparti quando si esce dal dettaglio
del tavolo o si usa l'azione di chiusura/liberazione. Il pulsante Paga registra
il pagamento ma non invia gli articoli pendenti a Cucina o Bar.


2. REQUISITI
------------
- Windows con JDK 25 installato e JAVA_HOME configurato. La versione Java
  richiesta e definita in pom.xml.
- PostgreSQL installato e in esecuzione. Il progetto e stato verificato con
  PostgreSQL 16.
- La porta TCP 8080 deve essere libera.
- Per usare il telefono o altre postazioni senza Internet, i dispositivi devono
  essere collegati alla stessa rete Wi-Fi/LAN del computer che esegue MyApp.

Il Maven Wrapper e incluso nel progetto: non e necessario installare Maven
separatamente.


3. PREPARAZIONE DEL DATABASE
----------------------------
Il database predefinito e:

    SpringDatabaseProdotti

Deve esistere prima di avviare il programma. Puoi crearlo da pgAdmin oppure
eseguire questo comando collegandoti a PostgreSQL con un utente autorizzato:

    CREATE DATABASE SpringDatabaseProdotti;

L'utente PostgreSQL configurato deve poter creare e modificare tabelle e indici
nel database. All'avvio MyApp crea o aggiorna automaticamente le proprie tabelle
di servizio, tra cui:

- app_camerieri: account dei camerieri e hash delle password.
- ordini_ricevuti: prodotti, comande, reparto e stato di uscita/evasione.
- pagamenti_conti: importo, metodo, operatore e data/ora del pagamento.

Le tabelle di servizio non compaiono negli archivi prodotto dell'interfaccia.

Le tabelle prodotto devono avere le colonne "nome" e "prezzo" per essere
utilizzabili dalla cassa e dalla pagina amministrativa Database. Il menu mobile
legge le colonne testuali e numeriche degli archivi prodotto. Il nome della
tabella/categoria viene usato per instradare le bevande al Bar; usa nomi chiari
come BEVANDE, BAR, BIBITE o DRINK per gli archivi delle bevande.


4. CONFIGURAZIONE
-----------------
Impostazioni predefinite in src/main/resources/application.properties:

- DB_URL: jdbc:postgresql://localhost:5432/SpringDatabaseProdotti
- DB_USERNAME: postgres
- DB_PASSWORD: 1234
- server.port: 8080
- server.address: 0.0.0.0, necessario per ricevere connessioni dalla rete locale.

Le variabili d'ambiente DB_URL, DB_USERNAME e DB_PASSWORD sovrascrivono i
valori predefiniti. In PowerShell, prima dell'avvio, puoi impostarle cosi:

    $env:DB_URL = "jdbc:postgresql://localhost:5432/SpringDatabaseProdotti"
    $env:DB_USERNAME = "postgres"
    $env:DB_PASSWORD = "la-password-del-tuo-database"

La password PostgreSQL non e la stessa password di accesso all'applicazione.


5. AVVIO E ARRESTO
------------------
Apri PowerShell nella cartella app, quella che contiene pom.xml e mvnw.cmd:

    cd "C:\Users\lucaa\Desktop\Programmi Java\JAVA SPRINGBOOT TEST\app"
    .\mvnw.cmd spring-boot:run

Si apre la finestra desktop e viene avviato anche il server web sulla porta
8080. Mantieni aperto il programma mentre telefoni, Cucina o Bar devono
collegarsi. Chiudendo l'applicazione desktop si arresta anche il server web.

Per fermare il programma, chiudi la finestra MyApp. Se lo avvii da una
PowerShell interattiva puoi anche usare Ctrl+C.

Comandi utili dalla cartella app:

    .\mvnw.cmd test
    .\mvnw.cmd clean package

Il primo esegue i test. Il secondo compila e crea il pacchetto Maven.


6. PRIMO ACCESSO
----------------
Credenziali desktop iniziali:

- Amministratore: username admin, password 1234.
- Cassiere: username cassiere, password 1234.

Dopo il login come amministratore, apri Camerieri nella barra laterale per
creare gli account individuali. Le password dei camerieri devono avere almeno
8 caratteri e sono memorizzate come hash. Un cameriere puo accedere sia dal
login desktop sia dal login mobile.

ATTENZIONE: le password iniziali admin/cassiere sono definite nel codice e non
sono modificabili dalla schermata. 1234 e una credenziale di sviluppo debole:
non esporre il server su Internet e non usare questa configurazione in un
ambiente pubblico o produttivo senza prima sostituire le credenziali e
proteggere gli endpoint web.


7. USO DELLA CASSA
------------------
1. Accedi come cassiere o amministratore.
2. Apri un tavolo dalla scheda Tavoli per associare gli articoli al tavolo.
3. Seleziona un archivio prodotto e tocca gli articoli per aggiungerli al conto.
4. Esci dal dettaglio del tavolo per inviare la comanda ai reparti. Le bevande
   vanno al Bar; gli altri prodotti vanno in Cucina.
5. Usa Annulla conto per annullare il conto e ritirare dalle code le righe non
   ancora evase.
6. Usa Scontrino per aprire la finestra di stampa. Se non e disponibile una
   stampante, il programma mostra un'anteprima testuale.
7. Usa Paga per registrare l'importo e selezionare Contanti o Carta. Il pagamento
   svuota il conto, ma non spedisce una comanda cassa che era ancora pendente.

Per vendite senza un tavolo selezionato, gli articoli vengono accodati sotto il
conto Cassa e possono essere pagati dalla schermata principale.

Le azioni Annulla conto, Scontrino e Paga sono disponibili anche dal menu
contestuale del singolo tavolo. L'azione di pagamento e una registrazione
interna: non comunica con terminali POS, circuiti bancari o registratori fiscali.
Lo scontrino generato non e un documento fiscale.


8. ORDINI DA TELEFONO
---------------------
Dal computer della cassa scansiona il QR "Ordini mobile" oppure apri:

    http://localhost:8080/mobile/login

Accedi con lo username e la password di un cameriere creato dall'amministratore.
Seleziona il tavolo, aggiungi gli articoli e premi Conferma ordine. Gli ordini
mobile vengono inviati subito alle code Cucina/Bar.

In caso di errore di rete, la pagina mostra un avviso e conserva la comanda nel
browser. Premi Riprova invio dopo aver ripristinato la connessione. Ogni riga ha
un identificativo idempotente: se una parte dell'invio era gia arrivata, il
retry non inserisce duplicati.

Nella selezione tavoli, quelli con un conto aperto sono indicati come Occupato.


9. POSTAZIONI CUCINA E BAR
--------------------------
Sul computer che esegue MyApp:

    http://localhost:8080/cucina
    http://localhost:8080/bar

La pagina Cucina mostra gli articoli di cucina; la pagina Bar mostra le bevande.
Le comande si aggiornano automaticamente. Seleziona una comanda, clicca un
prodotto quando e uscito e premi Evadi comanda quando il reparto ha terminato.
Lo stato del prodotto e della comanda viene salvato nel database.

Da un altro dispositivo della rete locale, sostituisci localhost con l'indirizzo
IP locale del computer che esegue MyApp, per esempio:

    http://192.168.1.25:8080/bar

L'indirizzo effettivo dipende dalla rete. Il QR mobile usa l'indirizzo locale
rilevato dal computer all'avvio. Per raggiungere MyApp da altri dispositivi,
Windows Firewall deve consentire le connessioni TCP in ingresso sulla porta
8080 e il router/access point non deve avere l'isolamento dei client attivo.

Le pagine web Cucina e Bar non richiedono credenziali applicative. Mantieni
l'accesso limitato alla rete locale e non inoltrare la porta 8080 su Internet.


10. RISOLUZIONE DEI PROBLEMI
----------------------------
Il programma non si avvia o mostra errori PostgreSQL:
- Verifica che il servizio PostgreSQL sia avviato.
- Verifica che SpringDatabaseProdotti esista.
- Verifica DB_URL, DB_USERNAME, DB_PASSWORD e i permessi dell'utente.

Il telefono non apre la pagina:
- Verifica che computer e telefono siano sulla stessa Wi-Fi/LAN.
- Prova dal browser del telefono l'indirizzo http://IP_DEL_PC:8080/mobile/login.
- Verifica che Windows Firewall consenta TCP 8080 e che l'access point non
  isoli i dispositivi.
- localhost indica sempre il dispositivo sul quale viene digitato; dal telefono
  usa l'IP del PC, non localhost.

La pagina web non si apre:
- Verifica che MyApp sia ancora avviato e che la porta 8080 non sia occupata.
- Le pagine web vengono chiuse quando si chiude la finestra desktop.

Una comanda mobile non risulta confermata:
- Ripristina la connessione e premi Riprova invio nella pagina del tavolo.
- Non cancellare i dati del browser prima che il retry sia completato: la comanda
  pendente e conservata nel localStorage del browser su quel dispositivo.

Lo scontrino non esce:
- Verifica stampante e driver. Se JavaFX non trova una stampante, viene mostrata
  l'anteprima testuale.


11. TABELLE PRINCIPALI
----------------------
- Archivi prodotto: tabelle create dall'amministratore e mostrate nei menu.
- app_camerieri: account e password hashate.
- ordini_ricevuti: righe degli ordini, categoria, cameriere, tavolo e stato.
- pagamenti_conti: registrazioni dei pagamenti locali.

Le tabelle tecniche sono escluse dagli archivi prodotto. Le modifiche allo schema
applicativo vengono applicate automaticamente all'avvio; non eliminare le
colonne o gli indici creati dal programma.
