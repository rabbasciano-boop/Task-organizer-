# Cronologia delle revisioni tecniche

Questo documento conserva lo storico delle quattro revisioni del progetto Task Organizer. Le revisioni originali erano analisi statiche: lo stato qui sotto viene aggiornato confrontandole con il codice effettivamente compilato e con i commit del repository.

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
