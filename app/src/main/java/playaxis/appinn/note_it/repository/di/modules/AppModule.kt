package playaxis.appinn.note_it.repository.di.modules

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import kotlinx.serialization.json.Json
import playaxis.appinn.note_it.receiver.ReceiverAlarmCallback
import playaxis.appinn.note_it.repository.DefaultAppDataRepository
import playaxis.appinn.note_it.repository.DefaultFoldersRepository
import playaxis.appinn.note_it.repository.DefaultLabelsRepository
import playaxis.appinn.note_it.repository.DefaultNotesRepository
import playaxis.appinn.note_it.repository.appData.AppDataRepository
import playaxis.appinn.note_it.repository.folder.FoldersRepository
import playaxis.appinn.note_it.repository.label.LabelsRepository
import playaxis.appinn.note_it.repository.model.DefaultJsonManager
import playaxis.appinn.note_it.repository.model.JsonManager
import playaxis.appinn.note_it.repository.model.utils.ReminderAlarmCallback
import playaxis.appinn.note_it.repository.note.NotesRepository

@Module(includes = [DatabaseModule::class])
abstract class AppModule {

    @get:Binds
    abstract val DefaultNotesRepository.bindNotesRepository: NotesRepository

    @get:Binds
    abstract val DefaultLabelsRepository.bindLabelsRepository: LabelsRepository

    @get:Binds
    abstract val DefaultAppDataRepository.bindAppDataRepository: AppDataRepository

    @get:Binds
    abstract val DefaultFoldersRepository.bindFoldersRepository: FoldersRepository

    @get:Binds
    abstract val DefaultJsonManager.bindJsonManager: JsonManager

    @get:Binds
    abstract val ReceiverAlarmCallback.bindAlarmCallback: ReminderAlarmCallback

    companion object {
        @Provides
        fun providesSharedPreferences(context: Context): SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context)

        @get:Provides
        val json
            get() = Json {
                encodeDefaults = false
                ignoreUnknownKeys = true
            }
    }
}
