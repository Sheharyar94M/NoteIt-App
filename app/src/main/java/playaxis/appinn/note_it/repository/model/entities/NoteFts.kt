package playaxis.appinn.note_it.repository.model.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions
@Fts4(
    contentEntity = Note::class,
    tokenizer = FtsOptions.TOKENIZER_UNICODE61
)
@Entity(tableName = "notes_fts")
data class NoteFts(
    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "content")
    val content: String
)
