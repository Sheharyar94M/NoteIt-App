package playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.note.helper_model

import android.graphics.Typeface

data class FontItem(
    var typeface: Typeface?,
    var isSelected:Boolean = false
){
    constructor() : this(null, false)
}