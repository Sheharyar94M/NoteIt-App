package playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils

import playaxis.appinn.note_it.repository.model.utils.ValueEnum
import playaxis.appinn.note_it.repository.model.utils.findValueEnum

/**
 * A note list layout mode.
 */
enum class NoteListLayoutMode(override val value: Int) : ValueEnum<Int> {
    LIST(0),
    GRID(1);

    companion object {
        fun fromValue(value: Int): NoteListLayoutMode = findValueEnum(value)
    }
}
