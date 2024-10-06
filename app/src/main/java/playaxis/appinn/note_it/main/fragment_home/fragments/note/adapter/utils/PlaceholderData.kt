package playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/**
 * Icon and message shown in the placeholder view when note list is empty.
 */
data class PlaceholderData(
    @DrawableRes val iconId: Int,
    @StringRes val messageId: Int
)
