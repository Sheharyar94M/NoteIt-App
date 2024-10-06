package playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils

/**
 * Text with a list of highlighted ranges.
 */
data class Highlighted(
    val content: String,
    val highlights: List<IntRange> = emptyList()
)
