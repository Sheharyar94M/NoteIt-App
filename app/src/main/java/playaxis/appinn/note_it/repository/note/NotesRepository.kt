package playaxis.appinn.note_it.repository.note

import kotlinx.coroutines.flow.Flow
import playaxis.appinn.note_it.repository.model.entities.Note
import playaxis.appinn.note_it.repository.model.entities.NoteStatus
import playaxis.appinn.note_it.repository.model.entities.NoteWithLabels

interface NotesRepository {

    suspend fun insertNote(note: Note): Long
    suspend fun updateNote(note: Note)
    suspend fun updateNotes(notes: List<Note>)
    suspend fun deleteNote(note: Note)
    suspend fun deleteNotes(notes: List<Note>)
    suspend fun getNoteById(id: Long): Note?
    suspend fun getNoteByIdWithLabels(id: Long): NoteWithLabels?
    suspend fun getLastCreatedNote(): Note?
    suspend fun getAllNotes(): List<NoteWithLabels>
    fun getNotesByStatus(status: NoteStatus): Flow<List<NoteWithLabels>>
    fun getNotesByLabel(labelId: Long): Flow<List<NoteWithLabels>>
    fun getNotesWithReminder(): Flow<List<NoteWithLabels>>
    fun searchNotes(query: String): Flow<List<NoteWithLabels>>
    suspend fun emptyTrash()
    suspend fun deleteOldNotesInTrash()
    suspend fun clearAllData()
}
