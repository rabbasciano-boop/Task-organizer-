package com.example.taskorganizer

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class QuickTaskParserTest {
    private val now = LocalDateTime.of(2026, 11, 28, 14, 15)

    @Test fun parsesItalianDatesAndTimes() {
        data class Case(val text: String, val title: String, val date: String, val time: String? = null)
        val cases = listOf(
            Case("Dentista domani alle 18", "Dentista", "2026-11-29", "18:00"),
            Case("Spesa oggi", "Spesa", "2026-11-28"),
            Case("Lezione dopodomani ore 09:30", "Lezione", "2026-11-30", "09:30"),
            Case("Visita tra 3 giorni alle 18.30", "Visita", "2026-12-01", "18:30"),
            Case("Riunione lunedì h 9", "Riunione", "2026-11-30", "09:00"),
            Case("Riunione lun 9:30", "Riunione", "2026-11-30", "09:30"),
            Case("Compito mar", "Compito", "2026-12-01"),
            Case("Compito mercoledì", "Compito", "2026-12-02"),
            Case("Compito gio", "Compito", "2026-12-03"),
            Case("Compito ven", "Compito", "2026-12-04"),
            Case("Compito sabato", "Compito", "2026-12-05"),
            Case("Compito dom", "Compito", "2026-11-29"),
            Case("Palestra il 15", "Palestra", "2026-12-15"),
            Case("Visita 15/12", "Visita", "2026-12-15"),
            Case("Visita 15/12/2026", "Visita", "2026-12-15"),
            Case("Festa 15/01", "Festa", "2027-01-15"),
            Case("Richiamo 29/02", "Richiamo", "2028-02-29"),
            Case("Pratica 29/02/2024", "Pratica", "2024-02-29"),
            Case("Cinema stasera", "Cinema", "2026-11-28", "20:00"),
            Case("Visita alle 10", "Visita", "2026-11-29", "10:00"),
            Case("Visita 23:59", "Visita", "2026-11-28", "23:59"),
            Case("Visita alle 00:00", "Visita", "2026-11-29", "00:00"),
            Case("Visita domani ore 18", "Visita", "2026-11-29", "18:00"),
            Case("Comprare latte", "Comprare latte", "2026-11-28"),
            Case("Compleanno 31/02", "Compleanno 31/02", "2026-11-28"),
            Case("Prova alle 25:99", "Prova alle 25:99", "2026-11-28"),
            Case("Dose 18", "Dose 18", "2026-11-28"),
            Case("Importo 18.30", "Importo 18.30", "2026-11-28"),
            Case("Appuntamento stamattina", "Appuntamento stamattina", "2026-11-28"),
            Case("domani", "domani", "2026-11-29"),
            Case("Chiama domani Rossi", "Chiama Rossi", "2026-11-29")
        )
        for (case in cases) {
            val actual = parseQuickTask(case.text, now)
            assertEquals(case.text, case.title, actual.title)
            assertEquals(case.text, LocalDate.parse(case.date), actual.date)
            assertEquals(case.text, case.time?.let(LocalTime::parse), actual.time)
        }
    }

    @Test fun recognizesMorningBeforeNineAndHandlesYearEnd() {
        val morning = parseQuickTask("Visita stamattina", LocalDateTime.of(2026, 11, 28, 8, 0))
        assertEquals(LocalTime.of(9, 0), morning.time)
        val next = parseQuickTask("Festa domani alle 00:00", LocalDateTime.of(2026, 12, 31, 23, 59))
        assertEquals(LocalDate.of(2027, 1, 1), next.date)
        assertEquals(LocalTime.MIDNIGHT, next.time)
    }
}
