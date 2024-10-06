package playaxis.appinn.note_it.utils

import android.content.Context
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.os.Environment
import android.text.TextUtils
import androidx.annotation.RequiresApi
import java.io.File
import java.util.Date
import java.util.Locale


object FileUtil {
    private fun getFolder(name: String?): String {
        val mediaStorageDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), name!!)
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return ""
            }
        }
        return mediaStorageDir.absolutePath
    }

    private val isSDAvailable: Boolean
        /**
         * sd card
         */
        get() = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED

    fun getNewFile(context: Context, folderName: String?): File? {
        val simpleDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val timeStamp: String = simpleDateFormat.format(Date())
        val path: String = if (isSDAvailable) {
            getFolder(folderName) + File.separator + timeStamp + ".txt"
        } else {
            context.filesDir.path + File.separator + timeStamp + ".txt"
        }
        return if (TextUtils.isEmpty(path)) {
            null
        } else File(path)
    }
}
