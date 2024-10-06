package playaxis.appinn.note_it.receiver

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import playaxis.appinn.note_it.R
import playaxis.appinn.note_it.repository.model.utils.ReminderAlarmCallback
import javax.inject.Inject

/**
 * Implementation of the alarm callback for [ReminderAlarmManager].
 * Uses the app context to set alarms broadcasted to [AlarmReceiver].
 */
class ReceiverAlarmCallback @Inject constructor(
    private val context: Context
) : ReminderAlarmCallback {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private var requestPermissionLauncher: ActivityResultLauncher<String>? = null
    private var TAG = "CrashAlarmPermission"

    @RequiresApi(Build.VERSION_CODES.S)
    override fun addAlarm(noteId: Long, time: Long) {
        val alarmIntent = getAlarmPendingIndent(noteId)
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, time, alarmIntent)
        }catch (se: SecurityException){
            Log.d(TAG,"Crash: the user removed the permission SCHEDULE_EXACT_ALARM at runtime " +
                    "or the android setting 'Pause app activity if unused' has been triggered")
            Toast.makeText(context, R.string.toast_alarm_permission_denied, Toast.LENGTH_LONG).show();
        }
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED
        ) {
            Toast.makeText(context, R.string.toast_notification_permission_denied, Toast.LENGTH_LONG).show();
        }
    }

    override fun removeAlarm(noteId: Long) {
        getAlarmPendingIndent(noteId).cancel()
    }

    private fun getAlarmPendingIndent(noteId: Long): PendingIntent {
        // Make alarm intent
        val receiverIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM
            putExtra(AlarmReceiver.EXTRA_NOTE_ID, noteId)
        }
        var flags = 0
        flags = flags or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, noteId.toInt(), receiverIntent, flags)
    }
}
