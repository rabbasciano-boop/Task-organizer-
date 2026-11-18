from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.style import WD_STYLE_TYPE
from docx.enum.table import WD_ALIGN_VERTICAL
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Inches, Pt, RGBColor
from pathlib import Path

OUTPUT = Path(__file__).with_name("Task Organizer Documento Unico del Progetto.docx")

DARK_BLUE = "14243B"
MID_BLUE = "284563"
LIGHT_BLUE = "EEF4F9"
GOLD = "A47528"
GRID = "D9D9D9"
BLACK = RGBColor(0, 0, 0)


def shade(cell, color):
    props = cell._tc.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:fill"), color)
    props.append(shd)


def border_cell(cell, color=GRID):
    tc_pr = cell._tc.get_or_add_tcPr()
    borders = tc_pr.first_child_found_in("w:tcBorders")
    if borders is None:
        borders = OxmlElement("w:tcBorders")
        tc_pr.append(borders)
    for edge in ("top", "left", "bottom", "right", "insideH", "insideV"):
        tag = "w:" + edge
        element = borders.find(qn(tag))
        if element is None:
            element = OxmlElement(tag)
            borders.append(element)
        element.set(qn("w:val"), "single")
        element.set(qn("w:sz"), "4")
        element.set(qn("w:color"), color)


def set_cell_text(cell, text, bold=False, color=BLACK, size=9.5, alignment=WD_ALIGN_PARAGRAPH.LEFT):
    cell.text = ""
    cell.vertical_alignment = WD_ALIGN_VERTICAL.CENTER
    p = cell.paragraphs[0]
    p.alignment = alignment
    p.paragraph_format.space_after = Pt(2)
    p.paragraph_format.space_before = Pt(2)
    run = p.add_run(text)
    run.bold = bold
    run.font.name = "Aptos"
    run._element.rPr.rFonts.set(qn("w:ascii"), "Aptos")
    run._element.rPr.rFonts.set(qn("w:hAnsi"), "Aptos")
    run.font.size = Pt(size)
    run.font.color.rgb = color
    border_cell(cell)


def add_table(doc, headers, rows, widths=None):
    table = doc.add_table(rows=1, cols=len(headers))
    table.autofit = False
    table.style = "Table Grid"
    for idx, header in enumerate(headers):
        cell = table.rows[0].cells[idx]
        shade(cell, DARK_BLUE)
        set_cell_text(cell, header, bold=True, color=RGBColor(255, 255, 255), size=9.5, alignment=WD_ALIGN_PARAGRAPH.CENTER)
        if widths:
            cell.width = Inches(widths[idx])
    for row_index, row in enumerate(rows):
        cells = table.add_row().cells
        for idx, value in enumerate(row):
            if row_index % 2 == 1:
                shade(cells[idx], LIGHT_BLUE)
            set_cell_text(cells[idx], value, size=9.2)
            if widths:
                cells[idx].width = Inches(widths[idx])
    doc.add_paragraph().paragraph_format.space_after = Pt(3)
    return table


def add_para(doc, text, bold_lead=None):
    p = doc.add_paragraph()
    p.paragraph_format.space_after = Pt(7)
    p.paragraph_format.line_spacing = 1.13
    if bold_lead and text.startswith(bold_lead):
        lead = p.add_run(bold_lead)
        lead.bold = True
        lead.font.color.rgb = BLACK
        rest = p.add_run(text[len(bold_lead):])
        rest.font.color.rgb = BLACK
    else:
        p.add_run(text).font.color.rgb = BLACK
    for run in p.runs:
        run.font.name = "Aptos"
        run._element.rPr.rFonts.set(qn("w:ascii"), "Aptos")
        run._element.rPr.rFonts.set(qn("w:hAnsi"), "Aptos")
        run.font.size = Pt(10.5)
    return p


def heading(doc, text, level=1):
    p = doc.add_paragraph(style=f"Heading {level}")
    p.paragraph_format.space_before = Pt(14 if level == 1 else 9)
    p.paragraph_format.space_after = Pt(6)
    run = p.add_run(text)
    run.font.color.rgb = BLACK
    return p


def bullet(doc, text):
    p = doc.add_paragraph(style="List Bullet")
    p.paragraph_format.space_after = Pt(3)
    run = p.add_run(text)
    run.font.name = "Aptos"
    run.font.size = Pt(10.5)
    run.font.color.rgb = BLACK


def configure_styles(doc):
    styles = doc.styles
    normal = styles["Normal"]
    normal.font.name = "Aptos"
    normal._element.rPr.rFonts.set(qn("w:ascii"), "Aptos")
    normal._element.rPr.rFonts.set(qn("w:hAnsi"), "Aptos")
    normal.font.size = Pt(10.5)
    for name, size in (("Title", 25), ("Subtitle", 12), ("Heading 1", 16), ("Heading 2", 12.5)):
        style = styles[name]
        style.font.name = "Aptos Display" if name != "Normal" else "Aptos"
        style._element.rPr.rFonts.set(qn("w:ascii"), style.font.name)
        style._element.rPr.rFonts.set(qn("w:hAnsi"), style.font.name)
        style.font.size = Pt(size)
        style.font.color.rgb = BLACK
    styles["Heading 1"].font.bold = True
    styles["Heading 2"].font.bold = True


def add_footer(section):
    footer = section.footer
    p = footer.paragraphs[0]
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = p.add_run("Task Organizer  |  Documento unico del progetto  |  Versione 0.1 - 18 novembre 2026")
    run.font.name = "Aptos"
    run.font.size = Pt(8)
    run.font.color.rgb = RGBColor(90, 90, 90)


def build():
    doc = Document()
    section = doc.sections[0]
    section.page_width = Inches(8.5)
    section.page_height = Inches(11)
    section.top_margin = Inches(0.72)
    section.bottom_margin = Inches(0.66)
    section.left_margin = Inches(0.78)
    section.right_margin = Inches(0.78)
    configure_styles(doc)
    add_footer(section)

    title = doc.add_paragraph(style="Title")
    title.alignment = WD_ALIGN_PARAGRAPH.CENTER
    title.add_run("Task Organizer")
    subtitle = doc.add_paragraph(style="Subtitle")
    subtitle.alignment = WD_ALIGN_PARAGRAPH.CENTER
    subtitle.add_run("Documento unico del progetto Android")
    subtitle.runs[0].font.color.rgb = BLACK
    doc.add_paragraph()
    add_para(doc, "Questo documento raccoglie le decisioni funzionali, tecniche e di sicurezza del progetto. Serve come riferimento unico mentre l'app viene sviluppata, testata e preparata per la pubblicazione.")
    add_table(doc, ["Informazione", "Valore"], [
        ("Piattaforma", "Android nativa con Kotlin e Jetpack Compose"),
        ("Stato", "Prototipo funzionante in evoluzione"),
        ("Dati principali", "Attività personali, liste, promemoria, preferenze e impostazioni"),
        ("Principio guida", "Dati locali, cifrati, sotto il controllo dell'utente"),
        ("Ultimo aggiornamento", "18 novembre 2026"),
    ], widths=[1.7, 5.0])

    heading(doc, "1 Obiettivo del prodotto")
    add_para(doc, "Task Organizer è un'organizer personale per pianificare attività, studio, appuntamenti e liste. L'app privilegia semplicità, chiarezza e controllo dei dati: può funzionare senza account e senza sincronizzare i dati con il computer.")
    add_para(doc, "La versione attuale è pensata prima di tutto per Android. In futuro potrà essere distribuita ad altri utenti e pubblicata su Google Play.")

    heading(doc, "2 Funzioni disponibili")
    add_table(doc, ["Area", "Funzione", "Stato"], [
        ("Attività", "Creazione, modifica, categorie, colori, note, priorità e stato esame", "Disponibile"),
        ("Date e orari", "Date italiane, orario di inizio e fine, calendario mensile", "Disponibile"),
        ("Promemoria", "Notifiche, anticipo configurabile, ripetizioni e sveglia", "Disponibile"),
        ("Organizzazione", "Filtri, riepilogo giornaliero e settimanale, cestino e ripristino", "Disponibile"),
        ("Liste", "To do list e lista spesa con importi", "Disponibile in prova"),
        ("Aspetto", "Tema scuro, Carta Dorata, Aurora Midnight e Aurora Nordic", "Disponibile"),
        ("Widget", "Progresso, riepilogo e agenda per la schermata Home", "Disponibile e gratuito"),
        ("Protezione", "Archivio cifrato locale, PIN e impronta se supportata", "Disponibile"),
    ], widths=[1.25, 4.4, 1.05])

    heading(doc, "3 Dati locali e protezione")
    add_para(doc, "Le attività e le liste vengono salvate nell'area privata dell'app sul telefono. L'archivio è cifrato con AES GCM e una chiave custodita dall'Android Keystore del dispositivo. Il salvataggio usa un meccanismo atomico per ridurre il rischio di perdita di dati durante un'interruzione.")
    add_para(doc, "L'app non sincronizza i dati con il computer. La disinstallazione dell'app o la cancellazione dei dati da Android eliminano l'archivio locale; per questo il futuro backup cifrato sarà importante, ma resterà una scelta dell'utente.")
    bullet(doc, "Il PIN e l'impronta proteggono l'accesso sullo stesso telefono.")
    bullet(doc, "Il cestino evita cancellazioni accidentali delle attività.")
    bullet(doc, "Le attività non devono sparire silenziosamente: un archivio non leggibile non viene sostituito con un archivio vuoto.")

    heading(doc, "4 Esperienza utente e interfaccia")
    add_para(doc, "L'interfaccia usa una base blu notte con accenti oro. I temi Aurora aggiungono gradazioni coerenti senza cambiare la struttura dell'app. Ogni attività può avere un colore proprio e le categorie possono usare icone personalizzate.")
    add_para(doc, "La schermata iniziale presenta saluto, progresso del giorno e attività. La schermata Attività offre filtri per mese, categoria e stato. Calendario, Account e Opzioni sono aree distinte per evitare sovrapposizioni di informazioni.")

    heading(doc, "5 Widget Android")
    add_table(doc, ["Formato", "Contenuto", "Interazioni"], [
        ("Progresso", "Percentuale e riepilogo della giornata", "Apre l'editor con il pulsante più"),
        ("Riepilogo", "Anello giornaliero, tre valori settimanali e prossima attività", "Completa la prossima attività o crea una nuova"),
        ("Agenda", "Data odierna, avanzamento, totale attività, filtri e lista", "Filtra, completa attività e apre l'editor"),
    ], widths=[1.2, 3.85, 1.65])
    add_para(doc, "I widget seguono il tema selezionato nell'app. Sono gratuiti e vengono aggiunti dalla schermata Home del telefono tramite la sezione Widget di Android.")

    heading(doc, "6 Account facoltativo")
    add_para(doc, "L'app continuerà a funzionare come ospite. L'account sarà richiesto soltanto per funzioni online opzionali: backup cifrato, collegamento al calendario personale, gestione delle funzioni Plus e recupero dei servizi collegati.")
    add_table(doc, ["Scelta", "Decisione"], [
        ("Provider", "Accesso con Google oppure Microsoft"),
        ("Servizio di identità", "Firebase Authentication, senza usare Supabase"),
        ("Password dell'app", "Non prevista: il recupero è gestito dal provider scelto"),
        ("Modalità ospite", "Disponibile senza registrazione"),
        ("Dati attività", "Restano locali e non vengono caricati con il solo login"),
    ], widths=[2.0, 4.7])
    add_para(doc, "Firebase Authentication gestirà soltanto identità e sessione dell'utente. Google e Microsoft sono responsabili delle rispettive credenziali. Il progetto Firebase dovrà essere creato e configurato prima dell'integrazione effettiva nell'app.")

    heading(doc, "7 Privacy consenso e controllo utente")
    add_para(doc, "Prima di usare l'app verrà mostrata una schermata di trasparenza. Non sarà un testo generico: distinguerà i dati che restano sul telefono dai dati usati soltanto se l'utente accede o abilita un servizio online.")
    add_table(doc, ["Situazione", "Dati trattati", "Destinazione"], [
        ("Uso come ospite", "Attività, note, liste, categorie, preferenze e PIN", "Solo sul telefono, in archivio cifrato"),
        ("Accesso Google o Microsoft", "Identificativo dell'account e dati base forniti dal provider, come nome o email se disponibili", "Firebase Authentication per la sessione"),
        ("Backup futuro attivato", "Archivio cifrato prima dell'invio", "Spazio cloud scelto dall'utente"),
        ("Notion", "Nessun contenuto di attività, note, email o credenziali", "Non collegato ai dati personali dell'app"),
    ], widths=[1.55, 3.15, 2.0])
    add_para(doc, "Non sono previste pubblicità, analisi comportamentale o vendita di dati. Le autorizzazioni Android, come le notifiche, saranno richieste nel momento in cui servono e potranno essere revocate dalle impostazioni del telefono.")

    heading(doc, "8 Uscita cancellazione e massimo controllo")
    add_para(doc, "Quando un utente esce dall'account potrà scegliere se mantenere l'archivio locale sul telefono oppure cancellarlo definitivamente. La scelta sarà mostrata in modo esplicito, senza opzioni preselezionate che cancellino dati.")
    add_para(doc, "L'eliminazione dell'account sarà una funzione distinta dall'uscita. Richiederà una nuova autenticazione con Google o Microsoft, perché è un'operazione sensibile. Quando sarà disponibile il backup, la procedura eliminerà prima il backup cifrato remoto e poi l'identità dell'account.")

    heading(doc, "9 Ruolo di Notion")
    add_para(doc, "Notion potrà essere usato come pannello organizzativo per il progetto: roadmap, test, richieste degli utenti, stato delle funzioni e decisioni di prodotto. Non sarà un sistema di login, non conserverà token nell'app e non deciderà chi può accedere ai dati personali.")

    heading(doc, "10 Roadmap")
    add_table(doc, ["Priorità", "Attività", "Stato"], [
        ("1", "Schermata privacy, consenso informato e modalità ospite", "Da implementare"),
        ("2", "Configurazione Firebase Authentication con Google e Microsoft", "Da configurare"),
        ("3", "Account, uscita, cancellazione dati locali ed eliminazione account", "Da implementare"),
        ("4", "Backup cifrato su Google Drive o OneDrive", "Da progettare"),
        ("5", "Collegamento opzionale Google Calendar e Outlook", "Da progettare"),
        ("6", "Accessibilità, privacy policy, consensi per la pubblicazione", "Da preparare"),
        ("7", "Funzioni Plus, pagamenti, test e pubblicazione Google Play", "Futuro"),
        ("8", "Documentazione tecnica, diagrammi UML e materiale di presentazione", "In aggiornamento"),
    ], widths=[0.7, 4.85, 1.15])

    heading(doc, "11 Riferimenti tecnici")
    add_para(doc, "Firebase Authentication per Google su Android: https://firebase.google.com/docs/auth/android/google-signin")
    add_para(doc, "Firebase Authentication per Microsoft su Android: https://firebase.google.com/docs/auth/android/microsoft-oauth")
    add_para(doc, "Gestione ed eliminazione degli utenti Firebase: https://firebase.google.com/docs/auth/android/manage-users")
    add_para(doc, "Informazioni Google Play sulla sezione Data safety: https://support.google.com/googleplay/android-developer/answer/10787469")

    doc.save(OUTPUT)
    print(OUTPUT)


if __name__ == "__main__":
    build()
