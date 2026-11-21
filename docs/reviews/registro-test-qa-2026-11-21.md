# Registro test QA manuali — 21 novembre 2026

- Pacchetto: `com.example.taskorganizer`
- Dispositivo: Motorola Edge 30 Neo
- Metodo: verifica manuale sul telefono e Logcat

## Risultati misurati

| Area | Risultato | Esito |
|---|---:|---|
| Avvio freddo | `+1s253ms` (1,25 s) | Superato: sotto la soglia di 2,0 s, senza lampi chiari o scuri. |
| Kanban con tastiera | Funzionale, ma con spazio vuoto eccessivo sopra la tastiera | Bug minore da correggere. |

## Verifiche manuali successive

| Area | Stato |
|---|---|
| Persistenza di attività e spesa dopo chiusura dai recenti | Da eseguire |
| Tema chiaro/scuro e contrasto | Da eseguire |
| Promemoria e sveglia a schermo bloccato | Da eseguire |
| Lista spesa: aggiornamento contatori con più spunte | Da eseguire |
| Passaggio rapido fra le cinque sezioni | Da eseguire |

## Nota tecnica

Il requisito di avvio sotto due secondi è soddisfatto sul dispositivo di prova. Il valore va rieseguito dopo interventi che toccano avvio, archivio o schermata iniziale.
