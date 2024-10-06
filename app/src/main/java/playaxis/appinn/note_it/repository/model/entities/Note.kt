package playaxis.appinn.note_it.repository.model.entities

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.TypeConverters
import playaxis.appinn.note_it.repository.model.converter.StringListConverter
import java.util.Date

@Entity(tableName = "notes", foreignKeys = [
    ForeignKey(
        entity = FolderNote::class,
        parentColumns = ["id"],
        childColumns = ["folderId"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE
    )
])
data class Note(
    /**
     * Note ID in the database.
     * ID is transient during serialization since notes are mapped by ID in JSON,
     * so repeating this field would be superfluous.
     */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    @Transient
    val id: Long = NO_ID,

    @ColumnInfo(name = "folderId", index = true)
    var folderId: Long?,

    @ColumnInfo(name = "type")
    val type: NoteType,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "colorNote")
    val colorNote: ColorNote?,

    @ColumnInfo(name = "isDrafted")
    val isDrafted: NoteDraftStatus,

    @ColumnInfo(name = "locked")
    var lock: String,

    @ColumnInfo(name = "noteAudio")
    val noteAudios: String,

    @ColumnInfo(name = "noteImages")
    @TypeConverters(StringListConverter::class)
    val noteImages: List<String>,

    @ColumnInfo(name = "noteTextFormat")
    val noteTextFormat: NoteTextFormat,

    @ColumnInfo(name = "metadata")
    val metadata: NoteMetadata,

    @ColumnInfo(name = "added_date")
    val addedDate: Date,

    @ColumnInfo(name = "modified_date")
    val lastModifiedDate: Date,

    @ColumnInfo(name = "status")
    val status: NoteStatus,

    @ColumnInfo(name = "pinned")
    val pinned: PinnedStatus,

    @Embedded(prefix = "reminder_")
    val reminder: Reminder?,
) {

    init {
        // Validate the type of metadata.
        require(when (type) {
            NoteType.TEXT -> metadata is BlankNoteMetadata
            NoteType.LIST -> metadata is ListNoteMetadata
        })
    }

    val isBlank: Boolean get() = title.isBlank() && content.isBlank() && reminder == null && noteImages.isEmpty() && noteAudios.isBlank()
    val listItems: MutableList<ListNoteItem> get() {
            check(type == NoteType.LIST) { "Cannot get list items for non-list note." }

            val checked = (metadata as ListNoteMetadata).checked
            val items = content.split('\n')
            if (items.size == 1 && checked.isEmpty()) {
                // No items
                return mutableListOf()
            }

            return items.mapIndexedTo(mutableListOf()) { i, text ->
                ListNoteItem(text.trim(), checked.getOrElse(i) { false })
            }
        }

    /**
     * Returns conversion of this note to a text note if it's not already one.
     * If all items were blank, resulting list note is empty. Otherwise, each item
     * because a text line with a bullet point at the start. Checked state is always lost.
     *
     * @param keepCheckedItems Whether to keep checked items or delete them.
     */
    fun asTextNote(keepCheckedItems: Boolean): Note = when (type) {
        NoteType.TEXT -> this
        NoteType.LIST -> {
            val items = listItems
            val content = if (items.all { it.content.isBlank() }) {
                // All list items are blank, so no content.
                ""
            } else {
                // Append a bullet point to each line of content.
                buildString {
                    for (item in items) {
                        if (keepCheckedItems || !item.checked) {
                            append(DEFAULT_BULLET_CHAR)
                            append(' ')
                            appendLine(item.content)
                        }
                    }
                    if (length > 0) {
                        deleteCharAt(lastIndex)
                    }
                }
            }
            copy(type = NoteType.TEXT, content = content, metadata = BlankNoteMetadata)
        }
    }

    /**
     * Returns a conversion of this note to a list note if it's not already one.
     * Each text line becomes an unchecked list item.
     * If all lines started with a bullet point, the bullet point is removed.
     */
    fun asListNote(): Note = when (type) {
        NoteType.LIST -> this
        NoteType.TEXT -> {
            // Convert each list item to a text line.
            val text = content.trim()
            val lines = text.split('\n')
            val content = if (lines.all { it.isNotEmpty() && it.first() in BULLET_CHARS }) {
                // All lines start with a bullet point, remove them.
                buildString {
                    for (line in lines) {
                        appendLine(line.substring(1).trim())
                    }
                    deleteCharAt(lastIndex)
                }
            } else {
                // List note items content are separated by line breaks, and this is already the case.
                text
            }
            val metadata = ListNoteMetadata(List(lines.size) { false })
            copy(type = NoteType.LIST, content = content, metadata = metadata)
        }
    }

    /**
     * Convert this note to text, including both the title and the content.
     */
    fun asText(includeTitle: Boolean = true): String {
        val textNote = asTextNote(true)
        return buildString {
            if (includeTitle && title.isNotBlank()) {
                appendLine(textNote.title)
            }
            append(textNote.content)
        }
    }

    fun withFolderId(newFolderId: Long): Note {
        return copy(folderId = newFolderId)
    }

    companion object {
        const val NO_ID = 0L
        const val BULLET_CHARS = "-+*•–"
        const val DEFAULT_BULLET_CHAR = '-'

        /**
         * Get the title of a copy of a note with [currentTitle].
         * Localized strings [untitledName] and [copySuffix] must be provided.
         * Returns "- Copy", "- Copy 2", "- Copy 3", etc, and sets a title if current is blank.
         */
        fun getCopiedNoteTitle(currentTitle: String, untitledName: String, copySuffix: String): String {
            val match = "^(.*) - $copySuffix(?:\\s+([1-9]\\d*))?$".toRegex().find(currentTitle)
            return when {
                match != null -> {
                    val name = match.groupValues[1]
                    val number = (match.groupValues[2].toIntOrNull() ?: 1) + 1
                    "$name - $copySuffix $number"
                }
                currentTitle.isBlank() -> "$untitledName - $copySuffix"
                else -> "$currentTitle - $copySuffix"
            }
        }
    }
}

data class NoteWithLabels(
    @Embedded
    val note: Note,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            LabelRef::class,
            parentColumn = "noteId",
            entityColumn = "labelId",
        ))
    val labels: List<Label>
)

/**
 * Representation of a list note item for [Note.listItems].
 */
data class ListNoteItem(val content: String, val checked: Boolean) {

    init {
        require('\n' !in content) { "List item content cannot contain line breaks." }
    }
}
