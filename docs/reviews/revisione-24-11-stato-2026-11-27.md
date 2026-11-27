# Revisione commit del 24/11 — stato e collaudo

Riferimento: `Task Organizer — Revisione commit del 24 11.md`, confrontato con il progetto il 27 novembre 2026. I punti di seguito descrivono il codice e non sostituiscono il collaudo su dispositivo.

## Carta Dorata

- Font Hanken Grotesk per i testi e Cormorant Garamond per i titoli; licenza SIL OFL 1.1 nei file di progetto e nell'app.
- Foglia oro per avanzamento, simboli di completamento e selezione nella navigazione; separatore con rombo nella Home.
- Calendario in stile Carta Dorata, con mesi dinamici, data selezionabile e attività reali modificabili e completabili.
- Verifica visiva iniziale della Home, del calendario e della leggibilità Sunset approvata dall'utente; verificare ancora formati, font grandi e altre schermate.

## Punti della revisione

1. La cancellazione di una voce ore viene richiesta subito all'archivio, mentre una copia consente di annullarla; riepilogo e CSV escludono le eliminazioni in corso. Da provare uscita immediata, rotazione e chiusura dell'app, incluso ripristino.
2. Se il salvataggio del timer fallisce, lo stato di attesa si sblocca e l'errore viene mostrato; il timer salvato nell'archivio resta attivo. Da provare con errore di scrittura controllato.
3. A partire da 24 ore, il timer chiede l'orario di fine del primo turno e avverte esplicitamente che le ore rimanenti vanno aggiunte separatamente. Annullare lascia il timer attivo. Da verificare con un timer di prova.
4. Gradienti Sunset scuriti per migliorare il contrasto del testo bianco, inclusi i colori delle card personalizzate. Controllo numerico aggiunto; l'utente ha approvato la prima verifica visiva. Ricontrollare schermate aggiuntive e contrasto reale sul dispositivo.
5. Interruttore suoni in Aspetto; vibrazione e Non disturbare non producono suoni. Carta Dorata usa le stesse caratteristiche audio degli altri effetti. Da verificare fisicamente; la licenza del file audio fornito deve essere confermata prima della pubblicazione.
6. Il suono viene deciso sulla transizione effettiva nell'archivio, non sullo stato precedentemente mostrato. Verificare tocchi rapidi.
7. Riproduzione audio su un singolo esecutore con coda limitata, fuori dal percorso di salvataggio; gestione degli errori audio. Verificare ripetizioni e prestazioni su telefono.
8. Traslazione della card applicata anche a cornice e alone; durante lo swipe il bagliore viene disattivato. Verifica manuale ancora necessaria.
9. La bozza conserva note e pausa quando si ferma il timer; il messaggio viene azzerato aprendo una voce diversa. Da testare sul telefono.
10. Gli avvisi di annullamento sono identificati dalla voce, mentre la cancellazione avviene subito; ripristino solo dopo salvataggio confermato. L'utente ha segnalato che «Annulla» era coperto dal pulsante +: l'avviso è stato spostato sopra di esso, da verificare sul telefono. Da testare anche eliminazioni multiple e rotazione.
11. Calendario Registro ore: descrizioni delle date e frecce per TalkBack; simboli decorativi principali sostituiti da icone o testo. Serve una verifica TalkBack completa su dispositivo.
12. Sfumatura di Agenda Reale già corretta nel commit precedente; ritaglio del feedback al tocco nel pulsante arrotondato aggiornato. Verifica visiva ancora necessaria.
13. Resta da misurare lo scorrimento delle liste lunghe su un telefono meno potente: i risultati non sono deducibili dalla sola compilazione.

## Prove consigliate

Usare dati di prova e un backup aggiornato. Verificare eliminazione e annullamento delle ore, uscita rapida e rotazione, errore di salvataggio, timer oltre 24 ore, modifica della bozza del timer, suoni attivi/disattivi con vibrazione e Non disturbare, swipe, TalkBack e prestazioni. Controllare anche la leggibilità del calendario e dei font grandi in Carta Dorata.
