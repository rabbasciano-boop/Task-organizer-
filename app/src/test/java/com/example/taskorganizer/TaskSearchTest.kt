package com.example.taskorganizer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskSearchTest {
    private val task = OrganizerTask(title = "Visita in Città", date = "2026-11-28", note = "Portare gli esami", category = "Salute")

    @Test fun ignoresAccentsAndCapitalization() {
        assertTrue(taskMatchesQuery(task, "CITTA"))
        assertTrue(taskMatchesQuery(task, "visiTa"))
    }

    @Test fun matchesNoteAndCategory() {
        assertTrue(taskMatchesQuery(task, "esami"))
        assertTrue(taskMatchesQuery(task, "salute"))
    }

    @Test fun allWordsMustMatchAcrossFields() {
        assertTrue(taskMatchesQuery(task, "  città   ESAMI  salute "))
        assertFalse(taskMatchesQuery(task, "citta dentista"))
    }

    @Test fun blankQueryMatchesAll() {
        assertTrue(taskMatchesQuery(task, ""))
        assertTrue(taskMatchesQuery(task, "  \t "))
    }

    @Test fun missingTextDoesNotMatch() {
        assertFalse(taskMatchesQuery(task, "inesistente"))
    }
}
