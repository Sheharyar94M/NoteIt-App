package playaxis.appinn.note_it.main.utils

import android.content.Intent
import androidx.fragment.app.Fragment

data class ShareData(val title: String, val content: String)

/**
 * Use an intent to share some [data] from this fragment.
 */
fun Fragment.startSharingData(data: ShareData) {
    val intent = Intent(Intent.ACTION_SEND)
    intent.type = "text/plain"
    intent.putExtra(Intent.EXTRA_TITLE, data.title)
    intent.putExtra(Intent.EXTRA_SUBJECT, data.title)
    intent.putExtra(Intent.EXTRA_TEXT, data.content)
    startActivity(Intent.createChooser(intent, null))
}
