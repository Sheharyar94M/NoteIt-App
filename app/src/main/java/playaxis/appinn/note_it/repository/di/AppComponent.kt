package playaxis.appinn.note_it.repository.di

import android.content.Context
import dagger.BindsInstance
import dagger.Component
import playaxis.appinn.note_it.application.QuickNotepad
import playaxis.appinn.note_it.main.QuickNotepadMain
import playaxis.appinn.note_it.main.drawer_fragments.archives.ArchiveFragment
import playaxis.appinn.note_it.main.drawer_fragments.label.CreateEditLabelFragment
import playaxis.appinn.note_it.main.drawer_fragments.reminder.ReminderFragment
import playaxis.appinn.note_it.main.drawer_fragments.trash.TrashFragment
import playaxis.appinn.note_it.main.fragment_home.HomeFragment
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.dialogs.ColorPaletteDialog
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.note.AddEditFragment
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.speech.SpeechFragment
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.todo.TodoFragment
import playaxis.appinn.note_it.main.fragment_home.fragments.folder.FolderListFragment
import playaxis.appinn.note_it.main.fragment_home.fragments.note.NoteListFragment
import playaxis.appinn.note_it.main.search.SearchFragment
import playaxis.appinn.note_it.receiver.AlarmReceiver
import playaxis.appinn.note_it.reminder.ReminderDialog
import playaxis.appinn.note_it.repository.di.modules.AppModule
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class])
interface AppComponent {

    fun inject(app: QuickNotepad)
    fun inject(activity: QuickNotepadMain)
    fun inject(fragment: HomeFragment)
    fun inject(fragment: NoteListFragment)
    fun inject(fragment: AddEditFragment)
    fun inject(alarmReceiver: AlarmReceiver)
    fun inject(reminderDialog: ReminderDialog)
    fun inject(dialog: ColorPaletteDialog)
    fun inject(fragment: FolderListFragment)
    fun inject(fragment: SpeechFragment)
    fun inject(fragment: TodoFragment)
    fun inject(fragment: CreateEditLabelFragment)
    fun inject(fragment: ArchiveFragment)
    fun inject(fragment: ReminderFragment)
    fun inject(fragment: TrashFragment)
    fun inject(fragment: SearchFragment)

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance appContext: Context): AppComponent
    }
}
