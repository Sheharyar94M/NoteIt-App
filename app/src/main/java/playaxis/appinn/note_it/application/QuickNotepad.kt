package playaxis.appinn.note_it.application

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
import playaxis.appinn.note_it.R
import playaxis.appinn.note_it.preferences.PrefsManager
import playaxis.appinn.note_it.repository.di.DaggerAppComponent
import playaxis.appinn.note_it.repository.room.NotesDatabase
import javax.inject.Inject

class QuickNotepad: Application(), CameraXConfig.Provider {

    val appComponent by lazy {
        DaggerAppComponent.factory().create(applicationContext)
    }
    @Inject
    lateinit var prefs: PrefsManager
    //
//    // for UI tests, should be injected in test ideally
//    // but this works for a temporary solution.
    @Inject
    lateinit var database: NotesDatabase

    override fun onCreate() {
        super.onCreate()

        appContext = this

        appComponent.inject(this)
        // Initialize shared preferences
        prefs.migratePreferences()
//        prefs.setDefaults(this)

//        EmojiManager.install(GoogleEmojiProvider())
        createNotificationChannel()
    }

    override fun getCameraXConfig(): CameraXConfig {
        return CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
            .setMinimumLoggingLevel(Log.ERROR).build()
    }
    private fun createNotificationChannel() {
        // https://developer.android.com/training/notify-user/build-notification#Priority
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, getString(R.string.reminder_notif_channel_title), NotificationManager.IMPORTANCE_HIGH)
            channel.description = getString(R.string.reminder_notif_channel_descr)
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object{
        lateinit var appContext: Context
        const val NOTIFICATION_CHANNEL_ID = "reminders"
    }
}