package playaxis.appinn.note_it.repository.model.entities

import kotlinx.serialization.Serializable

@Serializable
data class ColorNote(
    var color: String,
    var isSelected:Boolean = false
)
