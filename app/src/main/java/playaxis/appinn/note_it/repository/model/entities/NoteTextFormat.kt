package playaxis.appinn.note_it.repository.model.entities

import kotlinx.serialization.Serializable

@Serializable
data class NoteTextFormat(
    var spannable: Boolean,
    var fontPosition: Int
)