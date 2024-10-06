package playaxis.appinn.note_it.main.utils

import playaxis.appinn.note_it.repository.model.entities.Note
import playaxis.appinn.note_it.repository.model.entities.NoteStatus

data class StatusChange(
    val oldNotes: List<Note>,
    val oldStatus: NoteStatus,
    val newStatus: NoteStatus
)
