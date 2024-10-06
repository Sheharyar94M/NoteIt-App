package playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils

import playaxis.appinn.note_it.repository.model.utils.ValueEnum
import playaxis.appinn.note_it.repository.model.utils.findValueEnum

/**
 * Enum for different swipe action.
 */
enum class SwipeAction(override val value: String) : ValueEnum<String> {
    ARCHIVE("archive"),
    DELETE("delete"),
    NONE("none");

    companion object {
        fun fromValue(value: String): SwipeAction = findValueEnum(value)
    }
}
