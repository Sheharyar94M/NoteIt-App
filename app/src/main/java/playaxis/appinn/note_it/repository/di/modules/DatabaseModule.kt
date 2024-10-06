package playaxis.appinn.note_it.repository.di.modules

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import playaxis.appinn.note_it.repository.room.NotesDatabase
import javax.inject.Singleton

@Module
object DatabaseModule {

    @Provides
    @Singleton
    fun providesDatabase(context: Context) =
        Room.databaseBuilder(context,
            NotesDatabase::class.java, "notes_db.db")
            .addMigrations(*NotesDatabase.ALL_MIGRATIONS)
            .build()

    @Provides
    fun providesNotesDao(database: NotesDatabase) = database.notesDao()
    @Provides
    fun providesLabelsDao(database: NotesDatabase) = database.labelsDao()
    @Provides
    fun providesFoldersDao(database: NotesDatabase) = database.foldersDao()
}
