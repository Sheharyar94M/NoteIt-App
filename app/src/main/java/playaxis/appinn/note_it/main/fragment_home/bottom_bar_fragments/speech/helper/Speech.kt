package playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.speech.helper

import playaxis.appinn.note_it.repository.model.entities.Note

data class Speech(
    val id: Long = Note.NO_ID,
    var audio: String
)
