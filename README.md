# Task Organizer

Task Organizer è un'app Android locale per attività, calendario, promemoria, liste e widget.

## Principi di privacy

- Le attività sono salvate nella memoria privata dell'app e cifrate con Android Keystore.
- L'accesso Google è facoltativo: non carica automaticamente le attività.
- Il backup manuale crea un file `.tob` cifrato con una password scelta dall'utente.
- Il PIN, l'account collegato, la foto profilo e le credenziali non entrano mai nel backup.
- `google-services.json`, chiavi di rilascio e file di backup non devono essere caricati nel repository.

## Requisiti

- Android Studio aggiornato.
- Android SDK installato; l'app supporta Android 7.0 (API 24) e successivi.
- Java 11 configurato da Android Studio.
- Un progetto Firebase configurato per l'accesso Google, se si vuole usare il login opzionale.

## Configurazione Firebase

1. Crea o seleziona il progetto Firebase.
2. Registra l'app Android con l'identificativo del progetto.
3. Aggiungi SHA-1 e SHA-256 della chiave di debug per lo sviluppo e della chiave di rilascio prima della pubblicazione.
4. Scarica `google-services.json` in `app/google-services.json`.

Il file è ignorato da Git per evitare che dati di configurazione vengano pubblicati.

## Compilazione locale

Da Android Studio usa **Build > Make Project**, oppure dalla cartella del progetto:

```powershell
.\gradlew.bat :app:assembleDebug
```

Per eseguire i test JVM:

```powershell
.\gradlew.bat testDebugUnitTest
```

I test su dispositivo sono intenzionalmente disattivati: vanno avviati solo su un emulatore o su un dispositivo dedicato, mai sul telefono personale con dati reali.

## Backup manuale

In **Opzioni > Backup cifrato** puoi creare un file `.tob`. Conserva la password separatamente: non viene salvata e non può essere recuperata. Importare un backup sostituisce le attività, liste e categorie presenti sul telefono.

## Preparazione alla pubblicazione

Prima di Google Play occorrono un identificativo definitivo, una chiave di rilascio conservata fuori dal repository, una build release verificata, gli SHA Firebase della chiave finale e le pagine web per privacy e cancellazione account.
