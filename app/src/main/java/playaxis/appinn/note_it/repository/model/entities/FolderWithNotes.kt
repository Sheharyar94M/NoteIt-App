package playaxis.appinn.note_it.repository.model.entities

import androidx.room.Embedded
import androidx.room.Relation

class FolderWithNotes(
    @Embedded
    val folder: FolderNote,

    @Relation(parentColumn = "id", entityColumn = "folderId")
    var notes: List<Note>
)