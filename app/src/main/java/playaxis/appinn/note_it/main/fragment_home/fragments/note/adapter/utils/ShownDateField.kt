package playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils

import playaxis.appinn.note_it.repository.model.utils.ValueEnum
import playaxis.appinn.note_it.repository.model.utils.findValueEnum

/**
 * Enum for different date fields shown for notes.
 * [value] is from [R.array.pref_shown_date_values].
 */
enum class ShownDateField(override val value: String) : ValueEnum<String> {
    ADDED("added"),
    MODIFIED("modified"),
    NONE("none");

    companion object {
        fun fromValue(value: String): ShownDateField = findValueEnum(value)
    }
}
