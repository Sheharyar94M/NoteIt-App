package playaxis.appinn.note_it.repository.model.entities

import playaxis.appinn.note_it.repository.model.utils.ValueEnum
import playaxis.appinn.note_it.repository.model.utils.findValueEnum

enum class NoteType(override val value: Int) : ValueEnum<Int> {
    TEXT(0),
    LIST(1);

    companion object {
        fun fromValue(value: Int): NoteType = findValueEnum(value)
    }
}
