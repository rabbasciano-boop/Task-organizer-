package com.example.taskorganizer

/**
 * Punto di ingresso Android dell'app.
 * L'implementazione dell'interfaccia e del ciclo di vita è separata in [TaskOrganizerActivity].
 */
class MainActivity : TaskOrganizerActivity() {
    companion object {
        const val EXTRA_OPEN_NEW_TASK = "open_new_task"
    }
}
