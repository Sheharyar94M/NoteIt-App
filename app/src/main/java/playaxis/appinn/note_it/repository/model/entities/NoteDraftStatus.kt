package playaxis.appinn.note_it.repository.model.entities

import playaxis.appinn.note_it.repository.model.utils.ValueEnum
import playaxis.appinn.note_it.repository.model.utils.findValueEnum

enum class NoteDraftStatus(override val value: Int) : ValueEnum<Int> {
    NON_DRAFTED(0),
    DRAFTED(1),
    EDIT_DRAFTED(2);

    companion object {
        fun fromValue(value: Int): NoteDraftStatus = findValueEnum(value)
    }
}