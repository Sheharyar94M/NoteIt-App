package playaxis.appinn.note_it.repository

import playaxis.appinn.note_it.repository.folder.FoldersRepository
import playaxis.appinn.note_it.repository.model.entities.FolderNote
import playaxis.appinn.note_it.repository.model.entities.FolderWithNotes
import playaxis.appinn.note_it.repository.room.dao.FoldersDao
import javax.inject.Inject

class DefaultFoldersRepository @Inject constructor(private val foldersDao: FoldersDao):
    FoldersRepository {
    override suspend fun insertFolder(folder: FolderNote) {
        foldersDao.insert(folder)
    }
    override suspend fun updateFolder(folder: FolderNote) {
        foldersDao.update(folder)
    }
    override suspend fun deleteFolder(folderNote: FolderNote) {
        foldersDao.delete(folderNote)
    }
    override suspend fun deleteAll(folders: List<FolderNote>) {
        foldersDao.deleteAll(folders)
    }
    override suspend fun getAllFolders(): List<FolderWithNotes> {
        return foldersDao.getAll()
    }

    override suspend fun moveNotesToFolder(newFolderId: Long, noteIds: List<Long>) {
        return foldersDao.moveNotesToFolder(newFolderId,noteIds)
    }
}