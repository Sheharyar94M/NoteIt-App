package playaxis.appinn.note_it.repository.model.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "folders")
@Parcelize
data class FolderNote(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    @Transient
    val id: Long = NO_ID,

    @ColumnInfo(name = "name")
    var name: String

): Parcelable{

    companion object {
        const val NO_ID = 0L
    }
}