# Correzioni della revisione del 28 novembre 2026

- M1: gli inviti generici usano il canale Android “Suggerimenti”, indipendente da promemoria e sveglie. Su Android precedenti alla versione 8 non sono disponibili le categorie separate. In fase di creazione del nuovo canale, se i promemoria erano già disattivati, anche i suggerimenti partono disattivati.
- M2: rimossi i testi delle vecchie impostazioni non più usati.
- M3: il promemoria di backup non è inviato per un profilo vuoto; sono considerati anche cestino, liste, Registro ore e impostazioni personalizzate. L'aggiunta di dati riprogramma un eventuale avviso già scaduto.
- B1–B4: i nomi con iniziale maiuscola come “Mar” non sono interpretati come abbreviazioni; riconosciuti anche orari come “alle 9,30” e “alle 9.”. L'anteprima mostra il titolo ricavato e segnala gli orari ambigui; un orario passato richiede conferma esplicita. Testo e date restano modificabili.
- B7: il filtro della ricerca viene memorizzato rispetto ai dati e ai filtri correnti; è verificato che le modifiche alla lista aggiornino i risultati.
- B8: una prova automatica con un identificativo fittizio verifica che il salvataggio dell'attività annulli il posticipo precedente. Va comunque provato il caso reale di modifica dell'orario.
- B5 e B6: la tastiera e il saluto sono da controllare anche manualmente con caratteri grandi. Il saluto può andare a capo e, con nomi molto lunghi, scorrere senza nascondere la Home.
- B9: il login supportato rimane Google; altri provider richiedono una revisione della separazione dei profili prima dell'attivazione.
- Il comportamento della prima configurazione relativo alle notifiche non è stato cambiato; rivederlo con la proprietaria prima della pubblicazione.
