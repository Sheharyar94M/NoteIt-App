package playaxis.appinn.note_it.repository.folder

import playaxis.appinn.note_it.repository.model.entities.FolderNote
import playaxis.appinn.note_it.repository.model.entities.FolderWithNotes

interface FoldersRepository {
    suspend fun insertFolder(folder: FolderNote)
    suspend fun updateFolder(folder: FolderNote)
    suspend fun deleteFolder(folderNote: FolderNote)
    suspend fun deleteAll(folders: List<FolderNote>)
    suspend fun getAllFolders(): List<FolderWithNotes>
    suspend fun moveNotesToFolder(newFolderId: Long, noteIds: List<Long>)
}