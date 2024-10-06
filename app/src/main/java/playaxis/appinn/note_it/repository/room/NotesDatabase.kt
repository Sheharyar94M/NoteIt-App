package playaxis.appinn.note_it.repository.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import playaxis.appinn.note_it.repository.model.converter.ColorNoteConverter
import playaxis.appinn.note_it.repository.model.converter.DateTimeConverter
import playaxis.appinn.note_it.repository.model.converter.DraftStatusConverter
import playaxis.appinn.note_it.repository.model.converter.NoteMetadataConverter
import playaxis.appinn.note_it.repository.model.converter.NoteStatusConverter
import playaxis.appinn.note_it.repository.model.converter.NoteTextFormatConverter
import playaxis.appinn.note_it.repository.model.converter.NoteTypeConverter
import playaxis.appinn.note_it.repository.model.converter.PinnedStatusConverter
import playaxis.appinn.note_it.repository.model.converter.RecurrenceConverter
import playaxis.appinn.note_it.repository.model.converter.StringListConverter
import playaxis.appinn.note_it.repository.model.entities.FolderNote
import playaxis.appinn.note_it.repository.model.entities.Label
import playaxis.appinn.note_it.repository.model.entities.LabelRef
import playaxis.appinn.note_it.repository.model.entities.Note
import playaxis.appinn.note_it.repository.model.entities.NoteFts
import playaxis.appinn.note_it.repository.room.dao.FoldersDao
import playaxis.appinn.note_it.repository.room.dao.LabelsDao
import playaxis.appinn.note_it.repository.room.dao.NotesDao

@Database(entities = [Note::class, NoteFts::class, Label::class, LabelRef::class, FolderNote::class], version = NotesDatabase.VERSION, exportSchema = true)
@TypeConverters(
    DateTimeConverter::class, NoteTypeConverter::class, NoteStatusConverter::class, NoteMetadataConverter::class,
    PinnedStatusConverter::class, RecurrenceConverter::class, ColorNoteConverter::class, DraftStatusConverter::class, NoteTextFormatConverter::class,
    StringListConverter::class)
abstract class NotesDatabase : RoomDatabase() {

    abstract fun notesDao(): NotesDao
    abstract fun labelsDao(): LabelsDao
    abstract fun foldersDao(): FoldersDao

    @Suppress("MagicNumber")
    companion object {
        const val VERSION = 7

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // By removing the sync feature, some data is now useless.
                // - Deleted notes table
                // - Synced flag on notes
                // - UUID flag on notes (unique ID across devices)
                db.apply {
                    // By removing the sync feature, some data is now useless.
                    // - Deleted notes table
                    // - Synced flag on notes
                    // - UUID flag on notes (unique ID across devices)
                    execSQL("DROP TABLE deleted_notes")
                    execSQL("""CREATE TABLE notes_temp (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, folderId INTEGER,
                        type INTEGER NOT NULL, title TEXT NOT NULL, content TEXT NOT NULL, 
                        metadata TEXT NOT NULL, 
                        added_date INTEGER NOT NULL, modified_date INTEGER NOT NULL, status INTEGER NOT NULL, 
                        FOREIGN KEY(folderId) REFERENCES folders(id) ON DELETE CASCADE ON UPDATE CASCADE)""")
                    execSQL("""INSERT INTO notes_temp (id, folderId, type, title, content, metadata, added_date, 
                        modified_date, status) SELECT id, folderId, type, title, content, metadata, added_date, modified_date, status FROM notes""")
                    execSQL("DROP TABLE notes")
                    execSQL("ALTER TABLE notes_temp RENAME TO notes")
                }
            }
        }
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.apply {
                    // Add pinned column to notes table. 'unpinned' for active notes, 'can't pin' for others.
                    execSQL("ALTER TABLE notes ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
                    execSQL("UPDATE notes SET pinned = 1 WHERE status == 0")

                    // Add reminder columns, all set to `null` by default.
                    execSQL("ALTER TABLE notes ADD COLUMN reminder_start INTEGER")
                    execSQL("ALTER TABLE notes ADD COLUMN reminder_recurrence TEXT")
                    execSQL("ALTER TABLE notes ADD COLUMN reminder_next INTEGER")
                    execSQL("ALTER TABLE notes ADD COLUMN reminder_count INTEGER")
                    execSQL("ALTER TABLE notes ADD COLUMN reminder_done INTEGER")

                    // Add label tables
                    execSQL("CREATE TABLE labels (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL)")
                    execSQL("""CREATE TABLE label_refs (noteId INTEGER NOT NULL, labelId INTEGER NOT NULL,
                               PRIMARY KEY(noteId, labelId),
                               FOREIGN KEY(noteId) REFERENCES notes(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                               FOREIGN KEY(labelId) REFERENCES labels(id) ON UPDATE NO ACTION ON DELETE CASCADE)""")
                    execSQL("CREATE INDEX index_labels_name ON labels (name)")
                    execSQL("CREATE INDEX IF NOT EXISTS index_label_refs_noteId ON label_refs (noteId)")
                    execSQL("CREATE INDEX IF NOT EXISTS index_label_refs_labelId ON label_refs (labelId)")
                }
            }
        }
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.apply {
                    // Add hidden attribute for labels
                    execSQL("ALTER TABLE labels ADD COLUMN hidden INTEGER NOT NULL DEFAULT 0")
                }
            }
        }
        private val MIGRATION_4_5 = object : Migration(4, 5){
            override fun migrate(db: SupportSQLiteDatabase) {
                db.apply {
                    // Add folder attributes
                    // create table
                    execSQL("""CREATE TABLE folders (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                name TEXT NOT NULL)""")

                    // Add folderId column to notes table
                    execSQL("ALTER TABLE notes ADD COLUMN folderId INTEGER")

                    // Copy existing notes to the default folder (folderId = 0)
                    execSQL("UPDATE notes SET folderId = 0")

                    // Create a new table to store the relationships between folders and notes
                    execSQL("""CREATE TABLE folder_notes (
                folderId INTEGER NOT NULL,
                noteId INTEGER NOT NULL,
                PRIMARY KEY(folderId, noteId),
                FOREIGN KEY(folderId) REFERENCES folders(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(noteId) REFERENCES notes(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )""")

                    // Copy existing folder information from the notes table to the new folder_notes table
                    execSQL("""INSERT INTO folder_notes (folderId, noteId)
                SELECT folderId, id FROM notes WHERE folderId != 0
            """)

                    // Update notes_temp table to reflect the nullable folderId
                    execSQL("""CREATE TABLE notes_temp (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, folderId INTEGER,
                type INTEGER NOT NULL, title TEXT NOT NULL, content TEXT NOT NULL, colorNote TEXT NOT NULL, metadata TEXT NOT NULL, 
                added_date INTEGER NOT NULL, modified_date INTEGER NOT NULL, status INTEGER NOT NULL, 
                FOREIGN KEY(folderId) REFERENCES folders(id) ON DELETE CASCADE ON UPDATE CASCADE)""")

                    // Copy data from the current notes table to notes_temp
                    execSQL("""INSERT INTO notes_temp (id, folderId, type, title, content, colorNote, metadata, added_date, 
                modified_date, status) SELECT id, folderId, type, title, content, colorNote, metadata, added_date, modified_date, status FROM notes""")

                    // Drop the current notes table
                    execSQL("DROP TABLE notes")

                    // Rename notes_temp to notes
                    execSQL("ALTER TABLE notes_temp RENAME TO notes")
                }
            }
        }
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {

                db.apply {

                    // Add the new column to the 'notes' table
                    execSQL("ALTER TABLE notes ADD COLUMN colorNote TEXT")
                    execSQL("ALTER TABLE notes ADD COLUMN isDrafted INTEGER NOT NULL DEFAULT 0")
                    execSQL("ALTER TABLE notes ADD COLUMN locked TEXT")
                }

            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {

                db.apply {

                    execSQL("ALTER TABLE notes ADD COLUMN noteAudios TEXT")
                    execSQL("ALTER TABLE notes ADD COLUMN noteImages TEXT")
                    execSQL("ALTER TABLE notes ADD COLUMN noteTextFormat TEXT")
                }
            }
        }

        val ALL_MIGRATIONS = arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7
        )
    }
}