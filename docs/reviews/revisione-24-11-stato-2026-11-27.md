# Revisione commit del 24/11 — stato e collaudo

Riferimento: `Task Organizer — Revisione commit del 24 11.md`, confrontato con il progetto il 27 novembre 2026. I punti di seguito descrivono il codice e non sostituiscono il collaudo su dispositivo.

## Carta Dorata

- Font Hanken Grotesk per i testi e Cormorant Garamond per i titoli; licenza SIL OFL 1.1 nei file di progetto e nell'app.
- Foglia oro per avanzamento, simboli di completamento e selezione nella navigazione; separatore con rombo nella Home.
- Calendario in stile Carta Dorata, con mesi dinamici, data selezionabile e attività reali modificabili e completabili.
- Verifica visiva iniziale della Home, del calendario e della leggibilità Sunset approvata dall'utente; verificare ancora formati, font grandi e altre schermate.

## Punti della revisione

1. La cancellazione di una voce ore viene richiesta subito all'archivio, mentre una copia consente di annullarla; riepilogo e CSV escludono le eliminazioni in corso. L'utente ha confermato che la voce resta eliminata dopo essere uscita dal Registro ore ed essere rientrata. Restano da provare rotazione, chiusura dell'app e ripristino.
2. Se il salvataggio del timer fallisce, lo stato di attesa si sblocca e l'errore viene mostrato; il timer salvato nell'archivio resta attivo. Da provare con errore di scrittura controllato.
3. Il timer viene suddiviso automaticamente in registrazioni compatibili con l'archivio, con pausa e note, salvate insieme alla chiusura del timer. Prima di un minuto il timer rimane attivo. Da verificare con un timer di prova oltre 24 ore; il cambio di fuso orario o dell'ora legale durante il timer richiede ulteriori verifiche.
4. Gradienti Sunset scuriti per migliorare il contrasto del testo bianco, inclusi i colori delle card personalizzate. Controllo numerico aggiunto; l'utente ha approvato la prima verifica visiva. Ricontrollare schermate aggiuntive e contrasto reale sul dispositivo.
5. Interruttore suoni in Aspetto; vibrazione e Non disturbare non producono suoni. Carta Dorata usa le stesse caratteristiche audio degli altri effetti. Da verificare fisicamente; la licenza del file audio fornito deve essere confermata prima della pubblicazione.
6. Il suono viene deciso sulla transizione effettiva nell'archivio, non sullo stato precedentemente mostrato. Verificare tocchi rapidi.
7. Riproduzione audio su un singolo esecutore con coda limitata, fuori dal percorso di salvataggio; gestione degli errori audio. Verificare ripetizioni e prestazioni su telefono.
8. Traslazione della card applicata anche a cornice e alone; durante lo swipe il bagliore viene disattivato. Verifica manuale ancora necessaria.
9. In una prova sul telefono l'utente ha riscontrato che, dopo la timbratura di uscita, pausa e nota restavano soltanto nella bozza e la voce non compariva subito nella Dashboard. Correzione successiva: pausa e nota inserite vengono incluse nella registrazione salvata insieme alla chiusura del timer, la lista riceve una copia aggiornata e, tornando alla Dashboard, mostra la data e la registrazione appena salvata. Aggiunto un test sui dettagli della registrazione; da confermare sul telefono e verificare anche riavvio e modifica successiva.
10. Gli avvisi di annullamento sono identificati dalla voce, mentre la cancellazione avviene subito; ripristino solo dopo salvataggio confermato. Dopo una segnalazione di sovrapposizione al pulsante + e alla card vuota, gli avvisi hanno ora uno spazio dedicato sopra la barra inferiore: l'utente ha confermato la correzione. Da testare anche eliminazioni multiple e rotazione.
11. Calendario Registro ore: descrizioni delle date e frecce per TalkBack; simboli decorativi principali sostituiti da icone o testo. Serve una verifica TalkBack completa su dispositivo.
12. Sfumatura di Agenda Reale già corretta nel commit precedente; ritaglio del feedback al tocco nel pulsante arrotondato aggiornato. Verifica visiva ancora necessaria.
13. Resta da misurare lo scorrimento delle liste lunghe su un telefono meno potente: i risultati non sono deducibili dalla sola compilazione.

## Prove consigliate

Usare dati di prova e un backup aggiornato. Verificare eliminazione e annullamento delle ore, uscita rapida e rotazione, errore di salvataggio, timer oltre 24 ore, modifica della bozza del timer, suoni attivi/disattivi con vibrazione e Non disturbare, swipe, TalkBack e prestazioni. Controllare anche la leggibilità del calendario e dei font grandi in Carta Dorata.

## Interventi aggiuntivi del 27 novembre

- Migliorato il contrasto delle card Agenda Reale e dei segni di completamento Sunset; ridotto il costo del bagliore Sunset, da verificare visivamente e durante lo scorrimento.
- Agenda Reale riduce la frequenza delle scintille ambientali e rispetta l'impostazione di sistema che disabilita le animazioni. Verificare sul telefono, anche riattivando le animazioni.
- Font Carta Dorata forniti con pesi espliciti, calendario con celle espandibili e data corrente aggiornata periodicamente.
- Gli avvisi Annulla del Registro ore sopravvivono alla rotazione durante il periodo disponibile; l'eliminazione è comunque salvata subito.
- Il thread audio si arresta dopo l'inattività; in caso di filtro Non disturbare sconosciuto resta applicata la politica audio Android. La provenienza e la licenza del suono di Carta Dorata restano da verificare prima di pubblicare.
- Aggiornata la bozza dell'informativa con Registro ore e CSV; contatto e pubblicazione restano da completare.
- Compilazione debug e test unitari riusciti; le prove manuali su dispositivo restano necessarie.

## Riscontri del collaudo sul telefono

- L'utente conferma la leggibilità delle card Agenda Reale, TalkBack, l'avviso del timer breve, i suoni in modalità normale/silenzioso/vibrazione e la fluidità dello scorrimento.
- Da verificare dopo la correzione: rispetto di Rimuovi animazioni e immediatezza delle scintille; leggibilità delle registrazioni con caratteri molto grandi; messaggi e salvataggio dal pulsante superiore in Sunset; Annulla dopo rotazione; intensità neon di Sunset scuro.
- L'app aggiornata è stata installata sul telefono; compilazione e test unitari riusciti. Un test Compose isolato sul telefono ha verificato che il pulsante superiore inoltra la modifica valida in Sunset; rimane la prova del caso concreto dell'utente.
- Timer oltre 24 ore rimandato su richiesta dell'utente. Il test di Non disturbare non è ancora confermato.
