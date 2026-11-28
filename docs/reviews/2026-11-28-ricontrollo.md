# Ricontrollo del 28 novembre 2026

- N1: il passaggio da profilo vuoto a profilo con dati avvia un nuovo intervallo di 14 giorni; i successivi salvataggi non lo azzerano. Un backup esportato con successo continua a riavviare il conteggio.
- N2: l'inserimento rapido riconosce abbreviazioni come “Gio” e “Ven” all'inizio della frase. L'anteprima mostra il titolo interpretato, utile anche nei casi ambigui con nomi di persona.
- N3: se l'avviso backup incontra un errore di lettura temporaneo, riprova fino a tre volte a distanza di circa un'ora; dopo errori ripetuti l'app riprende la programmazione alla successiva apertura. Nessun archivio viene cancellato o sostituito.
- Test automatici: transizioni vuoto/dati, intervalli, limite dei tentativi e frasi abbreviate. Restano da provare sul telefono i casi elencati nel riepilogo di collaudo.
