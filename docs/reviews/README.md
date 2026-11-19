# Cronologia delle revisioni tecniche

Questo documento conserva lo storico delle revisioni del progetto Task Organizer. Le revisioni originali erano analisi statiche: lo stato qui sotto viene aggiornato confrontandole con il codice effettivamente compilato e con i commit del repository.

## Revisione 1 — base di sicurezza, archivio e promemoria

| Area verificata | Stato attuale | Interventi principali |
|---|---|---|
| Archivio illeggibile all'avvio | Risolto | Schermata di recupero, copia di sicurezza e avvio senza sovrascrivere il file problematico. |
| Conflitto fra widget e app | Risolto | Archivio aggiornato con lock condiviso e ricarica quando l'app torna attiva. |
| Crash del widget | Risolto | Lavoro in background con protezione dagli errori. |
| Promemoria e sveglie | Risolto | Identificativi univoci, testo corretto, riprogrammazione dopo riavvio/cambio ora, limite avvisi e permessi. |
| Privacy notifiche | Risolto | Contenuto privato nella schermata bloccata. |
| Backup | Risolto | Backup manuale `.tob` cifrato con password, importazione con conferma. |
| Pubblicazione | Aperto | Richiede identificativo definitivo, firma e configurazione esterna. |

Commit principali: `2610e97`, `9154f0a`, `6bf6e5c`, `1696f22`, `e9b4081`.

## Revisione 2 — compatibilità e affidabilità

| Area verificata | Stato attuale | Interventi principali |
|---|---|---|
| Android 7.0 e 7.1 | Risolto | Supporto alle API data/ora tramite desugaring. |
| Salvataggi falliti o lenti | Risolto | Operazioni cifrate su thread separato, messaggio chiaro in caso di errore e coda degli aggiornamenti. |
| Cancellazione dati | Risolto | Pulizia archivio, file di recupero, preferenze, widget, PIN e chiave locale. |
| Widget a mezzanotte | Risolto | Receiver data/ora/fuso e controllo aggiuntivo programmato. |
| PIN, biometria e schermate recenti | Risolto | Ritardi progressivi, impronta controllata, blocco screenshot e anteprime. |
| Backup/export | Risolto | Implementato nella revisione successiva. |

Commit principali: `19ebeba`, `6bf6e5c`, `1696f22`, `4be486e`, `ace7570`, `9eb3ac2`.

## Revisione 3 — privacy, widget e manutenzione

| Area verificata | Stato attuale | Interventi principali |
|---|---|---|
| Titoli privati nei widget | Risolto | Interruttore per sostituire il titolo con “Attività pianificata”. Categoria e orario restano visibili. |
| Foto profilo | Risolto | Solo HTTPS, limite 2 MB, nessun redirect, ridimensionamento e nessuna cache locale. |
| Pulizia Cestino | Risolto | Rimozione automatica dopo 30 giorni. |
| Icona notifiche e Android meno recenti | Risolto | Icona personale e suono sveglia corretto. |
| Widget ridimensionabili | Risolto | Riepilogo e Agenda ridimensionabili. |
| Accessibilità principale | Risolto in parte | Icone e descrizioni per barra di navigazione, avatar e completamento attività; resta una verifica TalkBack completa. |
| Stringhe e separazione schermate | Aperto | Miglioria di manutenzione, non blocca l'uso dell'app. |

Commit principali: `1fdc7e1`, `9eb3ac2`, `e9b4081`.

## Revisione 4 — ricorrenze e sicurezza dei test

| Area verificata | Stato attuale | Interventi principali |
|---|---|---|
| Ricorrenze dal widget | Risolto | App e widget usano la stessa logica. |
| Duplicati e attività scadute | Risolto | Una serie conserva il proprio identificativo e crea soltanto la prima occorrenza futura. |
| Giorni 29/30/31 mensili | Risolto | La serie mantiene il giorno originale: 31 gennaio → 28/29 febbraio → 31 marzo. |
| Test su dispositivo | Risolto | I test usano una chiave Keystore dedicata e non possono cancellare quella reale. |
| Cancellazione dati con errore Keystore | Risolto | L'eventuale errore della chiave non blocca la pulizia delle altre informazioni. |
| Biometria | Risolto | Controllo e finestra usano la stessa libreria AndroidX. |
| Cronologia tasto Indietro | Risolto dopo la revisione | Il tasto Android torna da editor, liste, backup, privacy e sezioni visitate. |

Commit principali: `c9a9c85`, `92ccc79`.

## Revisione 5 — rifiniture di ricorrenze e navigazione

| Area verificata | Stato attuale | Interventi principali |
|---|---|---|
| Data modificata in una serie ricorrente | Risolto | Modificando data o tipo di ricorrenza, il nuovo giorno diventa il riferimento per le occorrenze successive. |
| Cronologia del tasto Indietro | Risolto | Tornare alla scheda appena visitata rimuove il passaggio duplicato invece di aggiungerne uno nuovo. |
| Modifiche non salvate | Risolto | Indietro e Annulla chiedono conferma prima di scartare una modifica nell'editor. |
| Biometria e FragmentActivity | Verificato | La libreria biometrica porta già la dipendenza necessaria; la configurazione viene mantenuta senza nuove dipendenze ridondanti. |

Il relativo commit è registrato nella cronologia GitHub insieme alle altre revisioni.

## Revisione front-end — usabilità, contrasto e stabilità visiva

| Area verificata | Stato attuale | Interventi principali |
|---|---|---|
| Bozza mentre si modifica un'attività | Risolto | I campi dell'editor e la schermata aperta resistono alla ricreazione dell'interfaccia, ad esempio dopo una rotazione. |
| Pulsanti di completamento | Risolto | Area toccabile portata a 48 dp nelle card delle attività e nelle liste. |
| Contrasto nei temi chiari | Risolto | Accenti, conferme e azioni distruttive usano colori più scuri e leggibili in Carta Dorata e Aurora Nordic. |
| Barra di stato del telefono | Risolto | Icone chiare o scure in base al tema scelto, anche dopo il cambio tema. |
| Selezione data | Risolto | Il campo conserva l'inserimento manuale italiano e offre un calendario nativo. |
| Elenchi con molte attività | Risolto | Attività e Calendario usano liste a caricamento progressivo con chiavi stabili. |
| Schermi larghi | Risolto | Le pagine restano centrate e non superano una larghezza comoda su tablet e finestre grandi. |

Restano volutamente aperti lo spostamento delle stringhe in `strings.xml`, la divisione di `MainActivity.kt` e una verifica TalkBack completa: sono lavori di manutenzione più ampi e non modificano la sicurezza o il comportamento attuale.

## Revisione front-end 2 — correzioni di affidabilità

| Area verificata | Stato attuale | Interventi principali |
|---|---|---|
| Data scelta dal calendario | Risolto | La conversione usa sempre mezzanotte UTC, quindi il giorno non cambia in base al fuso orario. Sono presenti test automatici di conversione. |
| Completamento dal Calendario | Risolto | La spunta aggiorna ora lo stato dell'attività, come nelle altre schermate. |
| Spunte su card colorate | Risolto | I simboli usano tonalità chiare specifiche sulle card colorate, indipendenti dal tema scelto. |
| PIN dopo rotazione | Risolto | Lo stato della sessione resta vivo durante una ricreazione dell'interfaccia, ma non viene salvato dopo una vera chiusura dell'app. |
| Archivio con identificativi duplicati | Risolto | In lettura e importazione vengono mantenuti gli elementi più recenti, evitando un crash delle liste. |
| Accessibilità calendario e liste | Risolto | Frecce e spunte hanno descrizioni vocali; il calendario del campo Data è ora un'icona con descrizione. |
| Tema e avvio | Risolto | La barra adotta da subito il tema salvato, la schermata iniziale usa il tema scelto ed è disponibile l'opzione “Segui il tema del telefono”. |
| Prestazioni delle attività | Risolto | I filtri e gli ordinamenti vengono memorizzati finché attività o filtri non cambiano. |

### Verifiche manuali ancora consigliate

1. Inserire `20/11/2026`, aprire il calendario e confermare senza toccare nulla: la data deve restare il 20.
2. Nella scheda Calendario, completare e riaprire un'attività con la spunta.
3. Provare una card colorata in Carta Dorata e Aurora Nordic.
4. Con un PIN attivo, ruotare il telefono nell'editor: nessuna nuova richiesta PIN e bozza presente.
5. Attivare “Segui il tema del telefono” e cambiare tema Android.

## Revisione front-end 3 — sessione PIN e continuità dell'interfaccia

| Area verificata | Stato attuale | Interventi principali |
|---|---|---|
| PIN dopo background e cambio configurazione | Risolto | Quando l'app passa in background con PIN attivo, la sessione viene bloccata direttamente nel ViewModel. Una semplice rotazione mentre l'app è visibile non richiede invece un nuovo PIN. |
| Inizializzazione della sessione | Risolto | Il ViewModel riceve lo stato del PIN tramite una factory: non vengono più modificati stati dell'interfaccia durante il disegno della schermata. |
| Widget e tema di sistema | Risolto | Con “Segui il tema del telefono”, widget e app scelgono insieme Originale Dark o Carta Dorata e vengono aggiornati al cambio configurazione. |
| Filtri e calendario dopo rotazione | Risolto | Filtri, finestre delle attività, mese e giorno selezionati vengono conservati durante una ricreazione dell'interfaccia. |
| Calcoli delle liste | Risolto | Attività e Calendario usano stato derivato, evitando copie della lista a ogni ridisegno. |
| Flash all'avvio | Migliorato | La finestra iniziale usa una risorsa chiara/scura e viene immediatamente allineata al tema salvato dall'activity. |
| Test sessione e ID duplicati | Risolto | Aggiunti test automatici per blocco/sblocco della sessione e normalizzazione degli archivi con ID duplicati. |

### Prove manuali consigliate

1. Con PIN: sblocca, premi Home, cambia tema o ruota il telefono e rientra. Deve chiedere di nuovo il PIN.
2. Con PIN: ruota il telefono senza uscire dall'app. Non deve chiedere il PIN e la bozza aperta deve restare.
3. Attiva “Segui il tema del telefono”, cambia il tema Android e verifica che app, barra di stato e widget si aggiornino.
4. Imposta filtri o una data nel Calendario, ruota il telefono e verifica che la selezione resti identica.

## Verifiche manuali ancora consigliate

1. Ricorrenza giornaliera, settimanale e mensile, anche completata dal widget e dopo una modifica della data.
2. Creazione e ripristino di un backup su un emulatore o un secondo dispositivo senza dati importanti.
3. Widget con titoli nascosti, cambio di data e riavvio del telefono.
4. Notifiche consentite, negate e riattivate dalle impostazioni Android.
5. PIN, impronta e comportamento dopo più tentativi errati.

## Lavoro ancora aperto

### Qualità del progetto

- Spostare gradualmente le stringhe fisse in `strings.xml`, per rendere possibile la traduzione.
- Dividere `MainActivity.kt` in file per schermata e componente.
- Ampliare i test, soprattutto per PIN, widget e file di backup danneggiati.
- Eseguire una verifica completa con TalkBack.
- Controllare gli aggiornamenti delle dipendenze in Android Studio prima della versione pubblica.

### Pubblicazione Google Play

- Scegliere l'identificativo definitivo dell'applicazione.
- Creare e custodire la chiave di firma di rilascio.
- Aggiungere gli SHA-1/SHA-256 della versione release in Firebase.
- Compilare e provare una release ottimizzata.
- Pubblicare online la privacy policy e la procedura di eliminazione account già disponibili in `docs/`.
- Completare le dichiarazioni nella Google Play Console, compreso l'uso delle sveglie esatte.

## Regole di sicurezza per i test

- Non eseguire test strumentali sul telefono personale con dati reali.
- Usare un emulatore oppure un dispositivo dedicato.
- Prima di importare un backup, crearne sempre uno nuovo del contenuto attuale.
