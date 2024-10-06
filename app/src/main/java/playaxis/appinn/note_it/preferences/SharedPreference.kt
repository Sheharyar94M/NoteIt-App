package playaxis.appinn.note_it.preferences

import android.preference.PreferenceManager
import playaxis.appinn.note_it.application.QuickNotepad

object SharedPreference {

    private val sharedPreference = PreferenceManager.getDefaultSharedPreferences(QuickNotepad.appContext)

    @JvmStatic
    var isLoggedIn : Boolean
        get() = sharedPreference.getBoolean("LoggedIn", false)
        set(state){
        sharedPreference.edit().putBoolean("LoggedIn",state).apply()
    }

    fun isFirstRun(): Boolean {
        return sharedPreference.getBoolean("firstRun", true)
    }

    fun setFirstRunCompleted() {
        sharedPreference.edit().putBoolean("firstRun", false).apply()
    }

    @JvmStatic
    var selectedBackground : String?
        get() = sharedPreference.getString("selectedBackground", null)
        set(selectedBackground){
            sharedPreference.edit().putString("selectedBackground",selectedBackground).apply()
        }

    @JvmStatic
    var editFontsList : String?
        get() = sharedPreference.getString("editFontsList",null)
        set(editFontsList){
            sharedPreference.edit().putString("editFontsList",editFontsList).apply()
        }

    @JvmStatic
    var noteLock : Boolean
        get() = sharedPreference.getBoolean("noteLock",false)
        set(noteLock){
            sharedPreference.edit().putBoolean("noteLock",noteLock).apply()
        }

    @JvmStatic
    var email : String?
        get() = sharedPreference.getString("email",null)
        set(email){
            sharedPreference.edit().putString("email",email).apply()
        }

    @JvmStatic
    var username : String?
        get() = sharedPreference.getString("username",null)
        set(username){
            sharedPreference.edit().putString("username",username).apply()
        }
}