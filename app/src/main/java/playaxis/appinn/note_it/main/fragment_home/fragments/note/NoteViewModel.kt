package playaxis.appinn.note_it.main.fragment_home.fragments.note

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import playaxis.appinn.note_it.R
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.NoteAdapter
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.HeaderItem
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.NoteItem
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.NoteItemFactory
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.NoteListItem
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.NoteListLayoutMode
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.PlaceholderData
import playaxis.appinn.note_it.main.utils.Event
import playaxis.appinn.note_it.main.utils.EventParams
import playaxis.appinn.note_it.main.utils.ShareData
import playaxis.appinn.note_it.main.utils.StatusChange
import playaxis.appinn.note_it.main.utils.send
import playaxis.appinn.note_it.preferences.PrefsManager
import playaxis.appinn.note_it.repository.folder.FoldersRepository
import playaxis.appinn.note_it.repository.label.LabelsRepository
import playaxis.appinn.note_it.repository.model.entities.LabelRef
import playaxis.appinn.note_it.repository.model.entities.Note
import playaxis.appinn.note_it.repository.model.entities.NoteStatus
import playaxis.appinn.note_it.repository.model.entities.PinnedStatus
import playaxis.appinn.note_it.repository.model.utils.ReminderAlarmManager
import playaxis.appinn.note_it.repository.note.NotesRepository
import java.util.Date

/**
 * This view model provides common behavior for home and search view models.
 */
abstract class NoteViewModel(
    protected val savedStateHandle: SavedStateHandle,
    protected val notesRepository: NotesRepository,
    private val labelsRepository: LabelsRepository,
    private val folderRepository: FoldersRepository,
    protected val prefs: PrefsManager,
    protected val noteItemFactory: NoteItemFactory,
    protected val reminderAlarmManager: ReminderAlarmManager,
) : ViewModel(), NoteAdapter.Callback {

    protected var listItems: List<NoteListItem> = emptyList()
        set(value) {
            field = value
            _noteItems.postValue(value)

            _placeholderData.postValue(
                if (value.isEmpty())
                    updatePlaceholder()
                else
                    null
            )

            // Update selected notes.
            val selectedBefore = selectedNotes.size

            //this will only be updated when the home screen will appear
            if (changeSelection) {

                //list of selection
                _selectedNotes.clear()
                selectedNoteIds.clear()
                for (item in value) {
                    if (item is NoteItem && item.checked) {
                        _selectedNotes += item.note
                        selectedNoteIds += item.note.id
                    }
                }

                updateNoteSelection()
            }

            if (selectedNotes.size != selectedBefore) {
                saveNoteSelectionState()
            }
        }

    private var _selectedNotes = mutableSetOf<Note>()
    val selectedNoteIds = mutableSetOf<Long>()
    val selectedNotes: Set<Note> get() = _selectedNotes

    var changeSelection = false
    abstract fun updatePlaceholder(): PlaceholderData

    protected abstract val selectedNoteStatus: NoteStatus?

    private val _noteItems = MutableLiveData<List<NoteListItem>>()
    val noteItems: LiveData<List<NoteListItem>> get() = _noteItems

    private val _listLayoutMode = MutableLiveData<NoteListLayoutMode>()
    val listLayoutMode: LiveData<NoteListLayoutMode> get() = _listLayoutMode

    private val _editItemEvent = MutableLiveData<EventParams<Note, Int>>()
    val editItemEvent: LiveData<EventParams<Note, Int>>
        get() = _editItemEvent

    private val _shareEvent = MutableLiveData<Event<ShareData>>()
    val shareEvent: LiveData<Event<ShareData>>
        get() = _shareEvent

    protected val _statusChangeEvent = MutableLiveData<Event<StatusChange>>()
    val statusChangeEvent: LiveData<Event<StatusChange>>
        get() = _statusChangeEvent

    private val _currentSelection = MutableLiveData<NoteSelection>()
    val currentSelection: LiveData<NoteSelection>
        get() = _currentSelection

    private val _showReminderDialogEvent = MutableLiveData<Event<List<Long>>>()
    val showReminderDialogEvent: LiveData<Event<List<Long>>>
        get() = _showReminderDialogEvent

    private val _showLabelsFragmentEvent = MutableLiveData<Event<List<Long>>>()
    val showLabelsFragmentEvent: LiveData<Event<List<Long>>>
        get() = _showLabelsFragmentEvent

    private val _showDeletedForeverConfirmEvent = MutableLiveData<Event<Unit>>()
    val showDeleteConfirmEvent: LiveData<Event<Unit>> get() = _showDeletedForeverConfirmEvent

    protected val _placeholderData = MutableLiveData<PlaceholderData?>(null)
    val placeholderData: LiveData<PlaceholderData?> get() = _placeholderData

    private val _onItemSelectEvent = MutableLiveData<Event<Boolean>>()
    val onItemSelectEvent: LiveData<Event<Boolean>> get() = _onItemSelectEvent

    private val _noteLockedEvent = MutableLiveData<Event<Int>>()
    val noteLockedEvent: LiveData<Event<Int>> get() = _noteLockedEvent

    private val _noteLockedContentEvent = MutableLiveData<EventParams<Int, NoteItem>>()
    val noteLockedContentEvent: LiveData<EventParams<Int, NoteItem>> get() = _noteLockedContentEvent

    private val _itemClickEvent = MutableLiveData<EventParams<Int, NoteItem>>()
    val itemClickEvent: LiveData<EventParams<Int, NoteItem>> get() = _itemClickEvent

    protected var noteListJob: Job? = null
    private var restoreStateJob: Job? = null

    init {
        // Initialize list layout to saved value.
        _listLayoutMode.value = prefs.listLayoutMode
    }

    fun clear(){
        _selectedNotes.clear()
        selectedNoteIds.clear()
    }
    /**
     * Restore the state of this fragment from [savedStateHandle].
     * Must be called by child to ensure child is fully constructed before restoring state.
     * Notice that state restoration is suspending, so when initializing the child view model,
     * [waitForRestoredState] must be called to wait for state restoration to be complete.
     */
    protected fun restoreState() {
        restoreStateJob = viewModelScope.launch {
            // Restore saved selected notes
            selectedNoteIds += savedStateHandle.get<List<Long>>(KEY_SELECTED_IDS)
                .orEmpty().toMutableSet()
            _selectedNotes += selectedNoteIds.mapNotNull { notesRepository.getNoteById(it) }
            updateNoteSelection()
            restoreStateJob = null
        }
    }

    protected suspend fun waitForRestoredState() {
        restoreStateJob?.join()
    }

    /**
     * Stop updating list. This is called when the fragment view is destroyed to
     * prevent useless updates when the fragment isn't visible but the view model still exists.
     */
    fun stopUpdatingList() {
        noteListJob?.cancel()
        noteListJob = null
    }

    fun addNoteToSelection(note: Note) {
        _selectedNotes.add(note)
        selectedNoteIds.add(note.id)
    }

    /**
     * Called when note list is empty to update the placeholder data.
     */
//    abstract fun updatePlaceholder(): PlaceholderData

    fun clearSelection() {
        setAllSelected(false)
    }

    //for the view when the button of select all will be given
    fun selectAll() = setAllSelected(true)

    fun togglePin() {

        // If one note in selection isn't pinned, pin all. If all are pinned, unpin all.
        viewModelScope.launch {

            val newPinned = if (selectedNotes.any { it.pinned == PinnedStatus.UNPINNED })
                PinnedStatus.PINNED
            else
                PinnedStatus.UNPINNED

            val newNotes = selectedNotes.mapNotNull { note ->
                if (note.pinned != PinnedStatus.CANT_PIN && note.pinned != newPinned)
                    note.copy(pinned = newPinned)
                else
                    null
            }

            //updating the database
            for (note in newNotes)
                notesRepository.updateNote(note)
        }
    }

    fun createReminder() = _showReminderDialogEvent.send(selectedNoteIds.toList())

    fun changeLabels() = _showLabelsFragmentEvent.send(selectedNoteIds.toList())

    protected open fun onListLayoutModeChanged() = Unit

    fun toggleListLayoutMode() {
        val mode = when (_listLayoutMode.value!!) {
            NoteListLayoutMode.LIST -> NoteListLayoutMode.GRID
            NoteListLayoutMode.GRID -> NoteListLayoutMode.LIST
        }
        _listLayoutMode.value = mode
        prefs.listLayoutMode = mode

        onListLayoutModeChanged()
    }

    fun archiveNotes() {
        changeSelectedNotesStatus(
            if (selectedNoteStatus == NoteStatus.ACTIVE) {
                NoteStatus.ARCHIVED
            } else {
                NoteStatus.ACTIVE
            }
        )
    }

    fun unArchiveAll() = changeSelectedNotesStatus(NoteStatus.ACTIVE)

    fun addAllItemsToSelection() {
        for (item in listItems) {
            val note = item as NoteItem
            _selectedNotes.add(note.note)
        }
    }

    fun restoreNoteFromTrash() = changeSelectedNotesStatus(NoteStatus.ACTIVE)

    fun deleteSelectedNotesPre() = changeSelectedNotesStatus(NoteStatus.DELETED)

    fun deleteSelectedNotes() {
        // Delete forever
        viewModelScope.launch {
            notesRepository.deleteNotes(selectedNotes.toList())
            clearSelection()
        }
    }

    fun copySelectedNote(untitledName: String, copySuffix: String) {
        if (selectedNotes.size != 1) {
            return
        }

        viewModelScope.launch {
            val note = selectedNotes.first()
            val date = Date()
            val copy = note.copy(
                id = Note.NO_ID,
                title = Note.getCopiedNoteTitle(note.title, untitledName, copySuffix),
                addedDate = date,
                lastModifiedDate = date,
                reminder = null
            )
            val id = notesRepository.insertNote(copy)

            // Set labels for copy
            val labelIds = labelsRepository.getLabelIdsForNote(note.id)
            if (labelIds.isNotEmpty())
                labelsRepository.insertLabelRefs(labelIds.map { LabelRef(id, it) })

            clearSelection()
        }
    }

    fun shareSelectedNote() {
        val note = selectedNotes.firstOrNull() ?: return
        _shareEvent.send(ShareData(note.title, note.asText()))
    }

    /** Set the selected state of all notes to [selected]. */
    private fun setAllSelected(selected: Boolean) {
        if (!selected && selectedNotes.isEmpty()) {
            // Already all unselected.
            // (No fast path for all selected since there are multiple view types.)
            return
        }

        changeListItems {
            for ((i, item) in it.withIndex()) {
                if (item is NoteItem && item.checked != selected) {
                    it[i] = item.withChecked(selected)
                }
            }
        }
    }

    protected fun isNoteSelected(note: Note): Boolean {
        return note.id in selectedNoteIds
    }

    /** Update current selection live data to reflect current selection. */
    private fun updateNoteSelection() {
        // If no pinnable (active) notes are selected, selection is un-pinnable.
        // If at least one unpinned note is selected, selection is unpinned.
        // Otherwise selection is pinned.
        val pinned = when {
            selectedNotes.none { it.status == NoteStatus.ACTIVE } -> PinnedStatus.UNPINNED
            selectedNotes.any { it.pinned == PinnedStatus.UNPINNED } -> PinnedStatus.UNPINNED
            else -> PinnedStatus.PINNED
        }

        // If any note has a reminder, consider whole selection has a reminder,
        // so the single note reminder can be deleted.
        val hasReminder = selectedNotes.any { it.reminder != null }
        val hasLock = selectedNotes.any { it.lock.isNotEmpty() }

        for (note in selectedNotes) {

            _currentSelection.postValue(
                NoteSelection(
                    selectedNotes.size,
                    note.status,
                    pinned,
                    hasReminder,
                    hasLock
                )
            )
        }
    }

    /** Save [selectedNotes] to [savedStateHandle]. */
    private fun saveNoteSelectionState() {
        savedStateHandle[KEY_SELECTED_IDS] = selectedNoteIds.toList()
    }

    fun updateSelectedNotes(notesFolder: List<Note>) {
        viewModelScope.launch(Dispatchers.IO) {
            //update the notes
            notesRepository.updateNotes(notesFolder)
            delay(5000)
        }
    }

    /** Change the status of [notes] to [newStatus]. */
    protected fun changeNotesStatus(notes: Set<Note>, newStatus: NoteStatus) {
        val oldNotes = notes.filter { it.status != newStatus }.ifEmpty { return }

        viewModelScope.launch {
            val date = Date()
            val newNotes = mutableListOf<Note>()
            for (note in oldNotes) {
                newNotes += note.copy(
                    status = newStatus,
                    pinned = PinnedStatus.UNPINNED,
                    reminder = note.reminder.takeIf { newStatus != NoteStatus.DELETED },
                    lastModifiedDate = date,
                )
                if (newStatus == NoteStatus.DELETED) {
                    if (note.reminder != null) {
                        // Remove reminder alarm for deleted note.
                        reminderAlarmManager.removeAlarm(note.id)
                    }
                } else {
                    //assign the newStatus then update the selection
                    _selectedNotes = newNotes.toMutableSet()
                }
            }

            // Update the status in database
            notesRepository.updateNotes(newNotes)
        }
    }

    /** Change the status of selected notes to [newStatus], and clear selection. */
    private fun changeSelectedNotesStatus(newStatus: NoteStatus) {
        changeNotesStatus(selectedNotes, newStatus)
        updateNoteSelection()
    }

    fun noteUnLocked(item: NoteItem, pos: Int) {
        if (selectedNotes.isEmpty()) {
            changeSelection = true
            // Edit item
            _editItemEvent.send(item.note, pos)
            _selectedNotes.add(item.note)
            //setting the ids of selected notes
            selectedNoteIds.add(item.id)
        } else {
            // Toggle item selection
            toggleItemChecked(item, pos)
            _itemClickEvent.send(pos, item)
        }
    }


    override fun onNoteItemClicked(item: NoteItem, pos: Int) {
        if (item.note.lock.isNotEmpty()) {
            _noteLockedContentEvent.send(pos, item)
            _noteLockedEvent.send(1)
        }
        else {
            changeSelection = true

            if (selectedNotes.isEmpty()) {
                // Edit item
                _editItemEvent.send(item.note, pos)
                _selectedNotes.add(item.note)
                //setting the ids of selected notes
                selectedNoteIds.add(item.id)
            }
            else {
                // Toggle item selection
                toggleItemChecked(item, pos)

                //checking the status of both objects added
                if (selectedNotes.isNotEmpty()) {

                    var check = false
                    val list = selectedNotes.toList()

                    list.zipWithNext().forEach { (item1, item2) ->
                        check = item1.pinned != item2.pinned || item.note.pinned != item1.pinned
                    }

                    if (check && selectedNotes.size > 1) {

                        //update the selection
                        val list = selectedNotes.toList()
                        val hasReminder = selectedNotes.any { it.reminder != null }
                        val hasLock = selectedNotes.any { it.lock.isNotEmpty() }

                        for (note in list) {

                            _currentSelection.postValue(
                                NoteSelection(
                                    selectedNotes.size,
                                    note.status,
                                    PinnedStatus.CANT_PIN,
                                    hasReminder,
                                    hasLock
                                )
                            )
                        }
                    }
                    else {

                        //sending event to know the selection list contains something
                        _onItemSelectEvent.send(true)
                        updateNoteSelection()
                    }
                }
                else {
                    //sending event to know the selection list not contains anything
                    _onItemSelectEvent.send(false)
                    updateNoteSelection()
                }
            }
        }
    }

    override fun onNoteItemLongClicked(item: NoteItem, pos: Int) {
        changeSelection = true
        toggleItemChecked(item, pos)
        //sending event to know the selection list contains something
        _onItemSelectEvent.send(true)
        _selectedNotes.add(item.note)
        //setting the ids of selected notes
        selectedNoteIds.add(item.id)
    }

    private fun toggleItemChecked(item: NoteItem, pos: Int) =
        // Set the item as checked and update the list.
        changeListItems { listSelection ->
            listSelection[pos] = item.withChecked(!item.checked)
        }

    override val strikethroughCheckedItems: Boolean get() = prefs.strikethroughChecked

    protected inline fun changeListItems(change: (MutableList<NoteListItem>) -> Unit) {
        val newList = listItems.toMutableList()
        change(newList)
        listItems = newList
    }

    data class NoteSelection(
        val count: Int,
        val status: NoteStatus?,
        val pinned: PinnedStatus,
        val hasReminder: Boolean,
        val isLocked: Boolean
    )

    companion object {
        private const val KEY_SELECTED_IDS = "selected_ids"

        //searching
        val ARCHIVED_HEADER_ITEM = HeaderItem(-1, R.string.note_location_archived)
        private const val SEARCH_DEBOUNCE_DELAY = 100L
    }
}
