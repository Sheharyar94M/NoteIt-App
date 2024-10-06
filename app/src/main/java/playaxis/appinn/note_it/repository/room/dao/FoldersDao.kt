package playaxis.appinn.note_it.repository.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import playaxis.appinn.note_it.repository.model.entities.FolderNote
import playaxis.appinn.note_it.repository.model.entities.FolderWithNotes

@Dao
interface FoldersDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folderNote: FolderNote)
    @Update
    suspend fun update(folderNote: FolderNote)
    @Delete
    suspend fun delete(folderNote: FolderNote)
    @Delete
    suspend fun deleteAll(folders: List<FolderNote>)
    @Transaction
    @Query("SELECT * FROM folders")
    suspend fun getAll(): List<FolderWithNotes>
    @Query("UPDATE notes SET folderId = :newFolderId WHERE id IN (:noteIds)")
    suspend fun moveNotesToFolder(newFolderId: Long, noteIds: List<Long>)
}