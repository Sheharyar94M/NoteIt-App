package playaxis.appinn.note_it.repository.model.entities

import playaxis.appinn.note_it.repository.model.utils.ValueEnum
import playaxis.appinn.note_it.repository.model.utils.findValueEnum

enum class PinnedStatus(override val value: Int) : ValueEnum<Int> {
    CANT_PIN(0),
    UNPINNED(1),
    PINNED(2);

    companion object {
        fun fromValue(value: Int): PinnedStatus = findValueEnum(value)
    }
}
