package playaxis.appinn.note_it.main.search

import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import playaxis.appinn.note_it.R
import playaxis.appinn.note_it.main.fragment_home.fragments.note.NoteViewModel
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.NoteAdapter
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.HeaderItem
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.MessageItem
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.NoteItem
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.NoteItemFactory
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.NoteListItem
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.PlaceholderData
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.SwipeAction
import playaxis.appinn.note_it.main.viewModels.AssistedSavedStateViewModelFactory
import playaxis.appinn.note_it.preferences.PrefsManager
import playaxis.appinn.note_it.repository.folder.FoldersRepository
import playaxis.appinn.note_it.repository.label.LabelsRepository
import playaxis.appinn.note_it.repository.model.entities.NoteStatus
import playaxis.appinn.note_it.repository.model.entities.NoteWithLabels
import playaxis.appinn.note_it.repository.model.utils.ReminderAlarmManager
import playaxis.appinn.note_it.repository.note.NotesRepository

class SearchViewModel @AssistedInject constructor(
    @Assisted savedStateHandle: SavedStateHandle,
    notesRepository: NotesRepository,
    labelsRepository: LabelsRepository,
    foldersRepository: FoldersRepository,
    prefs: PrefsManager,
    reminderAlarmManager: ReminderAlarmManager,
    noteItemFactory: NoteItemFactory,
) : NoteViewModel(savedStateHandle, notesRepository, labelsRepository, foldersRepository, prefs, noteItemFactory, reminderAlarmManager),
    NoteAdapter.Callback {

    // No need to save this is a saved state handle, SearchView will
    // call query changed listener after it's been recreated.
    var lastQuery = ""

    private lateinit var listNotes: List<NoteListItem>

    var listNotesLiveData = MutableLiveData<List<NoteListItem>>()

    init {
        viewModelScope.launch {
            restoreState()
        }
    }

    fun searchNotes(query: String) {
        lastQuery = query
        noteItemFactory.query = query

        // Cancel previous flow collection / debounce
        noteListJob?.cancel()

        // Update note items live data when database flow emits a list.
        val cleanedQuery = SearchQueryCleaner.clean(query)
        noteListJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_DELAY)
            try {
                notesRepository.searchNotes(cleanedQuery).collect { notes ->
                    Log.i("searchNotes: ",notes.toString())
                    createListItems(notes)
                }
            } catch (e: SQLiteException) {
                // SearchQueryCleaner may not be perfect, user might have entered
                // something that produces erroneous FTS match syntax. Just ignore it.
                createListItems(emptyList())
            }
        }
    }

    override val selectedNoteStatus: NoteStatus?
        // If a single note is active in selection, treat all as active.
        // Otherwise all notes are archived. Deleted notes are never shown in search.
        get() = when {
            selectedNotes.isEmpty() -> null
            selectedNotes.any { it.status == NoteStatus.ACTIVE } -> NoteStatus.ACTIVE
            else -> NoteStatus.ARCHIVED
        }

    private fun createListItems(notes: List<NoteWithLabels>) {
        listNotes = buildList {
            var addedArchivedHeader = false
            for (noteWithLabels in notes) {
                val note = noteWithLabels.note

                // If this is the first archived note, add a header before it.
                if (!addedArchivedHeader && note.status == NoteStatus.ARCHIVED) {
                    this += ARCHIVED_HEADER_ITEM
                    addedArchivedHeader = true
                }

                val checked = isNoteSelected(note)
                this += noteItemFactory.createItem(note, noteWithLabels.labels, checked)
            }
        }

        listNotesLiveData.value = listNotes
    }

    override fun updatePlaceholder() = PlaceholderData(
        R.drawable.search, R.string.search_empty_placeholder)

    override fun onMessageItemDismissed(item: MessageItem, pos: Int) {
        TODO("Not yet implemented")
    }

    override fun onNoteActionButtonClicked(item: NoteItem, pos: Int) {
    }

    override fun getNoteSwipeAction(direction: NoteAdapter.SwipeDirection): SwipeAction {
        return SwipeAction.NONE
    }

    override fun onNoteSwiped(pos: Int, direction: NoteAdapter.SwipeDirection) {
    }

    @AssistedFactory
    interface Factory : AssistedSavedStateViewModelFactory<SearchViewModel> {
        override fun create(savedStateHandle: SavedStateHandle): SearchViewModel
    }

    companion object {
        val ARCHIVED_HEADER_ITEM = HeaderItem(-1, R.string.note_location_archived)

        private const val SEARCH_DEBOUNCE_DELAY = 100L
    }
}
