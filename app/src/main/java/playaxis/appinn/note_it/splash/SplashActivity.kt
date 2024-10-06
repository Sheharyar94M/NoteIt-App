package playaxis.appinn.note_it.splash

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import playaxis.appinn.note_it.R
import playaxis.appinn.note_it.main.QuickNotepadMain
import playaxis.appinn.note_it.utils.MainUtils

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen().setKeepOnScreenCondition{ true }
        setContentView(R.layout.activity_splash)

        //disabling night mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        //handling the status bar color
        MainUtils.makeStatusBarTransparent(this@SplashActivity)

        //currency activity
        startActivity(Intent(this@SplashActivity, QuickNotepadMain::class.java))
        finish()
    }
}