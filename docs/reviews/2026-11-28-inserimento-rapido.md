# Inserimento rapido — 28 novembre 2026

- La Home offre un campo per creare attività con testo libero e mostra un'anteprima di data e orario. La creazione usa l'archivio cifrato esistente e la stessa programmazione dei promemoria dell'editor.
- Il riconoscimento avviene localmente. Supporta date italiane, giorni della settimana, oggi/domani/dopodomani, intervalli in giorni, orari e le espressioni mattina/sera previste dalla scheda. Le espressioni non riconosciute restano nel titolo.
- La bozza sopravvive alla ricreazione della schermata e viene cancellata solo dopo un salvataggio riuscito. Una richiesta ripetuta con lo stesso identificativo non crea un doppione.
- I test verificano più di 25 frasi, casi limite di date e orari, conservazione della bozza, errore di salvataggio, apertura dell'editor e importazione di un backup precedente in un archivio isolato.
- Verifiche manuali: provare frasi con e senza orario, anteprima e salvataggio; aprire Modifica dal messaggio; verificare il promemoria; ruotare lo schermo con una bozza; provare caratteri grandi, TalkBack e tastiera.
