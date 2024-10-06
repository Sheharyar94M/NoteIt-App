package playaxis.appinn.note_it.receiver

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import playaxis.appinn.note_it.R
import playaxis.appinn.note_it.application.QuickNotepad
import playaxis.appinn.note_it.main.QuickNotepadMain
import playaxis.appinn.note_it.notification.NotificationActivity
import playaxis.appinn.note_it.repository.model.entities.Note
import playaxis.appinn.note_it.repository.model.utils.ReminderAlarmManager
import playaxis.appinn.note_it.repository.note.NotesRepository
import javax.inject.Inject

class AlarmReceiver : BroadcastReceiver() {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    @Inject
    lateinit var reminderAlarmManager: ReminderAlarmManager

    @Inject
    lateinit var notesRepository: NotesRepository

    override fun onReceive(context: Context?, intent: Intent) {
        if (context == null) return

        (context.applicationContext as QuickNotepad).appComponent.inject(this)

        coroutineScope.launch {
            val noteId = intent.getLongExtra(EXTRA_NOTE_ID, Note.NO_ID)
            when (intent.action) {
                Intent.ACTION_BOOT_COMPLETED -> reminderAlarmManager.updateAllAlarms()
                ACTION_ALARM -> showNotificationForReminder(context, noteId)
                ACTION_MARK_DONE -> markReminderAsDone(context, noteId)
            }
        }
    }

    /**
     * Receiver was called for reminder alarm, show a notification with the note title and content.
     * Clicking the notification opens the app to edit/view it.
     * Two action buttons can be clicked: mark as done and postpone.
     */
    private suspend fun showNotificationForReminder(context: Context, noteId: Long) {
        val note = notesRepository.getNoteById(noteId) ?: return

        reminderAlarmManager.setNextNoteReminderAlarm(note)

        var pendingIntentBaseFlags = 0
        if (Build.VERSION.SDK_INT >= 23) {
            pendingIntentBaseFlags = pendingIntentBaseFlags or PendingIntent.FLAG_IMMUTABLE
        }

        val noteText = note.asText(includeTitle = false).ifBlank { null }
        val builder = NotificationCompat.Builder(context, QuickNotepad.NOTIFICATION_CHANNEL_ID)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setSmallIcon(R.drawable.app_icon)
            .setGroup(NOTIFICATION_GROUP)
            .setContentTitle(note.title.ifBlank { null })
            .setContentText(noteText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(noteText))
            .setAutoCancel(true)

        // Edit/view main action
        val notifIntent = Intent(context, QuickNotepadMain::class.java).apply {
            action = QuickNotepadMain.INTENT_ACTION_EDIT
            putExtra(EXTRA_NOTE_ID, noteId)
        }
        builder.setContentIntent(PendingIntent.getActivity(context,
            noteId.toInt(), notifIntent, pendingIntentBaseFlags))

        // Add actions for non-recurring reminders
        if (note.reminder?.recurrence == null) {
            val pendingIntentFlags = pendingIntentBaseFlags or PendingIntent.FLAG_UPDATE_CURRENT

            // Mark done action
            val markDoneIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_MARK_DONE
                putExtra(EXTRA_NOTE_ID, noteId)
            }
            builder.addAction(R.drawable.tick_button_white, context.getString(R.string.action_mark_as_done),
                PendingIntent.getBroadcast(context, noteId.toInt(), markDoneIntent, pendingIntentFlags))

            // Postpone action only if not recurring.
            val postponeIntent = Intent(context, NotificationActivity::class.java).apply {
                action = NotificationActivity.INTENT_ACTION_POSTPONE
                putExtra(EXTRA_NOTE_ID, noteId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            builder.addAction(
                R.drawable.ic_calendar,
                context.getString(R.string.action_postpone),
                PendingIntent.getActivity(context, noteId.toInt(), postponeIntent, pendingIntentFlags))
        }

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        NotificationManagerCompat.from(context).notify(noteId.toInt(), builder.build())
    }

    private suspend fun markReminderAsDone(context: Context, noteId: Long) {
        reminderAlarmManager.markReminderAsDone(noteId)
        withContext(Dispatchers.Main) {
            NotificationManagerCompat.from(context).cancel(noteId.toInt())
        }
    }

    companion object {
        const val ACTION_ALARM = "com.example.quicknotepad.reminder.ALARM"
        const val ACTION_MARK_DONE = "com.example.quicknotepad.reminder.MARK_DONE"

        const val EXTRA_NOTE_ID = "com.example.quicknotepad.reminder.NOTE_ID"
        const val NOTIFICATION_GROUP = "com.example.quicknotepad.reminder.REMINDERS"
    }
}
