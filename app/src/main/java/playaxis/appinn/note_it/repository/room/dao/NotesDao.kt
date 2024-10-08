package playaxis.appinn.note_it.repository.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow
import playaxis.appinn.note_it.repository.model.converter.NoteStatusConverter
import playaxis.appinn.note_it.repository.model.entities.Label
import playaxis.appinn.note_it.repository.model.entities.Note
import playaxis.appinn.note_it.repository.model.entities.NoteStatus
import playaxis.appinn.note_it.repository.model.entities.NoteWithLabels
import playaxis.appinn.note_it.repository.model.utils.SortDirection
import playaxis.appinn.note_it.repository.model.utils.SortField
import playaxis.appinn.note_it.repository.model.utils.SortSettings

@Dao
interface NotesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<Note>)

    @Update
    suspend fun update(note: Note)

    @Update
    suspend fun updateAll(notes: List<Note>)

    @Delete
    suspend fun delete(note: Note)

    @Delete
    suspend fun deleteAll(notes: List<Note>)

    /**
     * Used for clearing all data.
     */
    @Query("DELETE FROM notes")
    suspend fun clear()

    /**
     * Get all notes in database, with their labels.
     * Used for exporting data.
     */
    @Transaction
    @Query("SELECT * FROM notes WHERE folderId NOT IN (SELECT id FROM folders)")
    suspend fun getAll(): List<NoteWithLabels>

    /**
     * Get a note by its ID. Returns `null` if note doesn't exist.
     */
    @Query("SELECT * FROM notes WHERE id == :id")
    suspend fun getById(id: Long): Note?

    /**
     * Get a note by its ID with its labels. Returns `null` if note doesn't exist.
     */
    @Transaction
    @Query("SELECT * FROM notes WHERE id == :id")
    suspend fun getByIdWithLabels(id: Long): NoteWithLabels?

    /**
     * Get the last created note, as indicated by creation date.
     * Returns `null` if there's no notes in database.
     */
    @Query("SELECT * FROM notes ORDER BY added_date DESC LIMIT 1")
    suspend fun getLastCreatedNote(): Note?

    /**
     * Get all notes with a [status], sorted by last modified date, with pinned notes first.
     * Exclude notes with a label marked as hidden, except if the note is deleted.
     * This is used to display notes for each status destination.
     */
    fun getByStatus(status: NoteStatus, sort: SortSettings) = sortedQuery("""
            SELECT * FROM notes WHERE status == :status AND (:status == 2 OR id NOT IN 
            (SELECT DISTINCT notes.id FROM notes JOIN label_refs ON noteId == notes.id 
            JOIN labels ON labelId == labels.id WHERE labels.hidden == 1))
            ORDER BY pinned DESC, :sort, id ASC
        """, sort, NoteStatusConverter.toInt(status))

    /**
     * Get all notes tagged with a label ([labelId]), except deleted notes.
     * The notes are sorted by status then by pinned status (pinned first), then by last modified date.
     * This is used to display notes by label.
     */
    fun getByLabel(labelId: Long, sort: SortSettings) = sortedQuery("""
            SELECT notes.* FROM notes JOIN 
            (SELECT noteId FROM label_refs WHERE labelId == :labelId) ON noteId == id
            WHERE status != 2 ORDER BY status ASC, pinned DESC, :sort, id ASC
        """, sort, labelId)

    /**
     * Get all notes with a reminder set and reminder not done, sorted by ascending date.
     * Used for reminders screen and for adding alarms back on boot.
     */
    @Transaction
    @Query("SELECT * FROM notes WHERE reminder_start IS NOT NULL AND NOT reminder_done ORDER BY reminder_next ASC")
    fun getAllWithReminder(): Flow<List<NoteWithLabels>>

    /**
     * Search active and archived notes for a [query] using full-text search,
     * sorted by status first then by last modified date.
     */
    fun search(query: String, sort: SortSettings) = sortedQuery("""
            SELECT * FROM notes JOIN notes_fts ON notes_fts.rowid == notes.id
            WHERE notes_fts MATCH :query AND status != 2
            ORDER BY status ASC, :sort
        """, sort, query)

    /**
     * For internal DAO use, to support query with variable sort field and direction.
     */
    @Transaction
    @RawQuery(observedEntities = [Note::class, Label::class])
    fun runtimeQuery(query: SupportSQLiteQuery): Flow<List<NoteWithLabels>>

    /**
     * Append [sort] settings at the end of a note with labels observable query, with a list of bindable [args]
     * (only primitive values, not converted automatically). [query] should contain a `ORDER BY :sort` substring.
     */
    private fun sortedQuery(query: String, sort: SortSettings, vararg args: Any): Flow<List<NoteWithLabels>> {
        val queryWithSort = query.replaceFirst(":sort", buildString {
            append(when (sort.field) {
                SortField.MODIFIED_DATE -> "notes.modified_date"
                SortField.ADDED_DATE -> "notes.added_date"
                SortField.TITLE -> "LOWER(notes.title)"
            })
            append(" ")
            append(when (sort.direction) {
                SortDirection.ASCENDING -> "ASC"
                SortDirection.DESCENDING -> "DESC"
            })

        })
        return runtimeQuery(SimpleSQLiteQuery(queryWithSort, args))
    }

    /**
     * Delete notes with a [status] and older than a [date][minDate].
     * Used for deleting notes in trash.
     */
    @Query("DELETE FROM notes WHERE status == :status AND modified_date < :minDate")
    suspend fun deleteNotesByStatusAndDate(status: NoteStatus, minDate: Long)
}
