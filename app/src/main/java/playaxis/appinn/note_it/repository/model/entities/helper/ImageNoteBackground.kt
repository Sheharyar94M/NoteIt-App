package playaxis.appinn.note_it.repository.model.entities.helper

import android.graphics.drawable.Drawable

data class ImageNoteBackground(
    var image: Drawable,
    var isSelected:Boolean = false
)
