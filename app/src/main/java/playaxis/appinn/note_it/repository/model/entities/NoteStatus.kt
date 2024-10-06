package playaxis.appinn.note_it.repository.model.entities

import playaxis.appinn.note_it.repository.model.utils.ValueEnum
import playaxis.appinn.note_it.repository.model.utils.findValueEnum

enum class NoteStatus(override val value: Int) : ValueEnum<Int> {
    ACTIVE(0),
    ARCHIVED(1),
    DELETED(2);

    companion object {
        fun fromValue(value: Int): NoteStatus = findValueEnum(value)
    }
}
