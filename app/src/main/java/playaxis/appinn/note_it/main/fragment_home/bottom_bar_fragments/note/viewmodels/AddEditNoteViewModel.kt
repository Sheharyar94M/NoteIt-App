package playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.note.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.note.viewmodels.utils.EditMessage
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.todo.adapter.TodoListAdapter
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.todo.utils.EditCheckedHeaderItem
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.todo.utils.EditChipsItem
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.todo.utils.EditContentItem
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.todo.utils.EditDateItem
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.todo.utils.EditItemAddItem
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.todo.utils.EditItemItem
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.todo.utils.EditListItem
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.todo.utils.EditUncheckedHeaderItem
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.todo.utils.EditableText
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.ShownDateField
import playaxis.appinn.note_it.main.utils.Event
import playaxis.appinn.note_it.main.utils.ShareData
import playaxis.appinn.note_it.main.utils.StatusChange
import playaxis.appinn.note_it.main.utils.send
import playaxis.appinn.note_it.main.viewModels.AssistedSavedStateViewModelFactory
import playaxis.appinn.note_it.preferences.PrefsManager
import playaxis.appinn.note_it.repository.appData.AppDataRepository
import playaxis.appinn.note_it.repository.label.LabelsRepository
import playaxis.appinn.note_it.repository.model.entities.BlankNoteMetadata
import playaxis.appinn.note_it.repository.model.entities.ColorNote
import playaxis.appinn.note_it.repository.model.entities.Label
import playaxis.appinn.note_it.repository.model.entities.LabelRef
import playaxis.appinn.note_it.repository.model.entities.ListNoteMetadata
import playaxis.appinn.note_it.repository.model.entities.Note
import playaxis.appinn.note_it.repository.model.entities.NoteDraftStatus
import playaxis.appinn.note_it.repository.model.entities.NoteMetadata
import playaxis.appinn.note_it.repository.model.entities.NoteStatus
import playaxis.appinn.note_it.repository.model.entities.NoteTextFormat
import playaxis.appinn.note_it.repository.model.entities.NoteType
import playaxis.appinn.note_it.repository.model.entities.PinnedStatus
import playaxis.appinn.note_it.repository.model.entities.Reminder
import playaxis.appinn.note_it.repository.model.utils.ReminderAlarmManager
import playaxis.appinn.note_it.repository.note.NotesRepository
import java.util.Collections
import java.util.Date

class AddEditNoteViewModel @AssistedInject constructor(
    appDataRepository: AppDataRepository,
    private val notesRepository: NotesRepository,
    private val labelsRepository: LabelsRepository,
    private val prefs: PrefsManager,
    private val reminderAlarmManager: ReminderAlarmManager,
    @Assisted private val savedStateHandle: SavedStateHandle
) : AbstractAddEditNoteViewModel(appDataRepository), TodoListAdapter.Callback {

    /**
     * Whether the current note is a new note.
     * This is important to remember as to not recreate as new blank note
     */
    private var isNewNote = false

    /**
     * Note being edited by user. This note data is not up-to-date with the UI.
     * - Call [updateNote] to update it to reflect UI state.
     * - Call [saveNote] to update it from UI and update database.
     */
    private var note = BLANK_NOTE

    /**
     * List of labels on note. Always reflects the UI.
     */
    private var labels = emptyList<Label>()

    /**
     * Status of the note being edited. This is separate from [note] so that
     * note status can be updated from this in [updateNote].
     */
    var status = note.status

    /**
     * Whether the note being edited is pinned or not.
     */
    var pinned = note.pinned

    /**
     * The reminder set on the note, or `null` if none is set.
     */
    private var reminder: Reminder? = null

    var title: String = ""
    var content: String = ""
    var noteClickedId: Long = note.id
    var colorNote: ColorNote = ColorNote("", false)

    var locked: String = ""
    var noteAudios: String = ""
    var noteImage: List<String> = ArrayList()
    var metadata: NoteMetadata = BlankNoteMetadata
    var noteTextFormat: NoteTextFormat = NoteTextFormat(spannable = false, fontPosition = -1)
    var folderId: Long? = null

    /**
     * URL of last clicked span, if any.
     */
    private var linkUrl: String? get() = savedStateHandle[KEY_LINK_URL]
        set(value) {
            savedStateHandle[KEY_LINK_URL] = value
        }

    private var listItems: MutableList<EditListItem> = mutableListOf()

    private val _noteType = MutableLiveData<NoteType>()
    val noteType: LiveData<NoteType> get() = _noteType

    private val _noteStatus = MutableLiveData<NoteStatus>()
    val noteStatus: LiveData<NoteStatus> get() = _noteStatus

    private val _notePinned = MutableLiveData<PinnedStatus>()
    val notePinned: LiveData<PinnedStatus> get() = _notePinned

    private val _noteReminder = MutableLiveData<Reminder?>()
    val noteReminder: LiveData<Reminder?> get() = _noteReminder

    private val _editItems = MutableLiveData<MutableList<EditListItem>>()
    val editItems: LiveData<MutableList<EditListItem>> get() = _editItems

    private val _noteCreateEvent = MutableLiveData<Event<Long>>()
    val noteCreateEvent: LiveData<Event<Long>> get() = _noteCreateEvent

    private val _focusEvent = MutableLiveData<Event<FocusChange>>()
    val focusEvent: LiveData<Event<FocusChange>> get() = _focusEvent

    private val _messageEvent = MutableLiveData<Event<EditMessage>>()
    val messageEvent: LiveData<Event<EditMessage>> get() = _messageEvent

    private val _statusChangeEvent = MutableLiveData<Event<StatusChange>>()
    val statusChangeEvent: LiveData<Event<StatusChange>> get() = _statusChangeEvent

    private val _shareEvent = MutableLiveData<Event<ShareData>>()
    val shareEvent: LiveData<Event<ShareData>> get() = _shareEvent

    private val _showDeleteConfirmEvent = MutableLiveData<Event<Unit>>()
    val showDeleteConfirmEvent: LiveData<Event<Unit>> get() = _showDeleteConfirmEvent

    private val _showRemoveCheckedConfirmEvent = MutableLiveData<Event<Unit>>()
    val showRemoveCheckedConfirmEvent: LiveData<Event<Unit>> get() = _showRemoveCheckedConfirmEvent

    private val _showReminderDialogEvent = MutableLiveData<Event<Long>>()
    val showReminderDialogEvent: LiveData<Event<Long>> get() = _showReminderDialogEvent

    private val _showLabelsFragmentEvent = MutableLiveData<Event<Long>>()
    val showLabelsFragmentEvent: LiveData<Event<Long>> get() = _showLabelsFragmentEvent

    private val _newNoteEvent = MutableLiveData<Event<Note>>()
    val newNoteEvent: LiveData<Event<Note>> get() = _newNoteEvent

    /**
     * Whether to show date item.
     */
    private val shouldShowDate: Boolean get() = if (isNewNote) false else prefs.shownDateField != ShownDateField.NONE

    /**
     * Whether note is currently in trash (deleted) or not.
     */
    private val isNoteInTrash: Boolean get() = status == NoteStatus.DELETED

    private var updateNoteJob: Job? = null
    private var restoreNoteJob: Job? = null

    private val _noteClicked = MutableLiveData<Note?>()
    val noteClicked: LiveData<Note?> get() = _noteClicked

    var moveFolderNote = false


    init {
        if (KEY_NOTE_ID in savedStateHandle) {
            restoreNoteJob = viewModelScope.launch {
                isNewNote = savedStateHandle[KEY_IS_NEW_NOTE] ?: false

                val note = notesRepository.getNoteById(savedStateHandle[KEY_NOTE_ID] ?: Note.NO_ID)
                if (note != null) {
                    this@AddEditNoteViewModel.note = note
                }
                restoreNoteJob = null
            }
        }
    }

    /**
     * Initialize the view model to edit a note with the ID [noteId].
     * The view model can only be started once to edit a note.
     * Subsequent calls with different arguments will do nothing and previous note will be edited.
     *
     * @param noteId Can be [Note.NO_ID] to create a new note with [type], [title] and [content].
     * @param labelId Can be different from [Label.NO_ID] to initially set a label on a new note.
     * @param changeReminder Whether to start editing note by first changing the reminder.
     */
    fun start(
        noteId: Long = Note.NO_ID,
        labelId: Long = Label.NO_ID,
        changeReminder: Boolean = false,
        type: NoteType = NoteType.TEXT,
        title: String = "",
        content: String = "",
    ) {
        viewModelScope.launch {
            // If fragment was very briefly destroyed then recreated, it's possible that this job is launched
            // before the job to save the note on fragment destruction is called.
            updateNoteJob?.join()
            // Also make sure note is restored after recreation before this is called.
            restoreNoteJob?.join()

            val isFirstStart = (note == BLANK_NOTE)

            // Try to get note by ID with its labels.
            val noteWithLabels = notesRepository.getNoteByIdWithLabels(
                if (isFirstStart) {
                    // first start, use provided note ID
                    noteId
                } else {
                    // start() was already called, fragment view was probably recreated
                    // use the note ID of the note being edited previously
                    note.id
                }
            )

            var note = noteWithLabels?.note
            var labels = noteWithLabels?.labels

            if (note == null) {
                // Note doesn't exist, create new blank note of the corresponding type.
                // This is the expected path for creating a new note (by passing Note.NO_ID)
                val date = Date()
                note = BLANK_NOTE.copy(
                    addedDate = date, lastModifiedDate = date, title = title, content = content
                )
                if (type == NoteType.LIST) {
                    note = note.asListNote()
                }

                val id = notesRepository.insertNote(note)
                note = note.copy(id = id)

                // If a label was passed to be initially set, use it.
                // Otherwise no labels will be set.
                val label = labelsRepository.getLabelById(labelId)
                labels = listOfNotNull(label)
                if (label != null) {
                    labelsRepository.insertLabelRefs(listOf(LabelRef(id, labelId)))
                }

                _noteCreateEvent.send(id)

                isNewNote = true
                savedStateHandle[KEY_IS_NEW_NOTE] = true
            }

            this@AddEditNoteViewModel.note = note
            this@AddEditNoteViewModel.labels = labels!!
            status = note.status
            pinned = note.pinned
            reminder = note.reminder

            _noteType.value = note.type
            _noteStatus.value = status
            _notePinned.value = pinned
            _noteReminder.value = reminder

            savedStateHandle[KEY_NOTE_ID] = note.id

            if (!moveFolderNote)
                recreateListItems()

            if (isFirstStart && isNewNote) {

                //Focus on title
                focusItemAt(findItemPos<EditContentItem>(), 0, false)

                if (changeReminder) {
                    changeReminder()
                }

                //add new note to selection list
                _newNoteEvent.send(note)
            }
        }
    }

    fun setItemsList(listItems: MutableList<EditListItem>){
        this.listItems = listItems
    }

    fun getNoteById(id: Long): Note? {

        var note: Note? = null
        viewModelScope.launch(Dispatchers.IO) {
            note = notesRepository.getNoteById(id)
        }
        return note
    }

    suspend fun deleteNoteSpeech(note: Note) {
        notesRepository.deleteNote(note)
        reminderAlarmManager.removeAlarm(note.id)
    }

    private fun focusItemAt(pos: Int, textPos: Int, itemExists: Boolean) {
        _focusEvent.send(FocusChange(pos, textPos, itemExists))
    }

    private fun recreateListItems() {
        listItems.clear()
        val canEdit = !isNoteInTrash

        // Date item
        if (shouldShowDate) {
            listItems += EditDateItem(
                when (prefs.shownDateField) {
                    ShownDateField.ADDED -> note.addedDate.time
                    ShownDateField.MODIFIED -> note.lastModifiedDate.time
                    else -> 0L  // never happens
                }
            )
        }

        when (note.type) {
            NoteType.TEXT -> {
                // Content item
                listItems += EditContentItem(DefaultEditableText(note.content), canEdit)
            }

            NoteType.LIST -> {
                val noteItems = note.listItems
                // Unchecked list items
                val unCheckCount = noteItems.count { !it.checked }
                listItems += EditUncheckedHeaderItem(unCheckCount)
                for ((i, item) in noteItems.withIndex()) {
                    if (!item.checked) {
                        listItems += EditItemItem(
                            DefaultEditableText(item.content), false, canEdit, i
                        )
                    }
                }

                // Item add item
                if (canEdit) {
                    listItems += EditItemAddItem
                }

                // Checked list items
                val checkCount = noteItems.count { it.checked }
                if (checkCount > 0) {
                    listItems += EditCheckedHeaderItem(checkCount)
                    for ((i, item) in noteItems.withIndex()) {
                        if (item.checked) {
                            listItems += EditItemItem(
                                DefaultEditableText(item.content), true, canEdit, i
                            )
                        }
                    }
                }
            }
        }

        val chips = mutableListOf<Any>()
        if (reminder != null) {
            chips += reminder!!
        }
        chips.addAll(labels)
        if (chips.isNotEmpty()) {
            listItems += EditChipsItem(chips)
        }

        updateListItems()
    }

    fun updateListItems() {
        _editItems.value = listItems.toMutableList()
    }

    /**
     * Update note and save it in database if it was changed.
     * This updates last modified date.
     */
    fun saveNote() {
        // Update note
        updateNote()

        // NonCancellable to avoid save being cancelled if called right before fragment destruction
        updateNoteJob = viewModelScope.launch(NonCancellable) {
            // Compare previously saved note from database with new one.
            // It is possible that note will be null here, if:
            // - Back arrow is clicked, saving note.
            // - Exit is called subsequently, deleting blank note.
            // - onStop calls saveNote again, but note was deleted.
            notesRepository.updateNote(note)

            Log.i("saveNote: ",note.toString())

            val oldNote = notesRepository.getNoteById(note.id) ?: return@launch
            if (oldNote != note) {
                // Note was changed.
                // To know whether last modified date should be changed, compare note
                // with a copy that has the original values for fields we don't care about.
                val noteForComparison = note.copy(
                    pinned = if (note.status == oldNote.status) oldNote.pinned else note.pinned
                )
                if (oldNote != noteForComparison) {
                    note = note.copy(lastModifiedDate = Date())
                }

                notesRepository.updateNote(note)
                updateNoteJob = null
            }
        }
    }

    /**
     * Send exit event. If note is blank, it's discarded.
     */
    fun exit() {
        viewModelScope.launch {
            updateNoteJob?.join()
            if (note.isBlank) {
                // Delete blank note
                deleteNoteInternal()
                _messageEvent.send(EditMessage.BLANK_NOTE_DISCARDED)
            }
        }
    }

    /**
     * Update [note] to reflect UI changes, like text changes.
     * Note is not updated in database and last modified date isn't changed.
     */
    private fun updateNote() {
        if (listItems.isEmpty()) {
            // updateNote seems to be called before list items are created due to
            // live data events being called in a non-deterministic order? Return to avoid a crash.
            return
        }

        // Create note
        val titleText = title
        var contentText = ""
        val colorNote = colorNote
        val lock = locked
        val noteImage = noteImage
        val noteAudios = noteAudios
        val format = noteTextFormat

        when (note.type) {
            NoteType.TEXT -> {
                contentText = content
                metadata = BlankNoteMetadata
            }

            NoteType.LIST -> {
                // Add items in the correct actual order
                val items = MutableList(listItems.count { it is EditItemItem }) { TEMP_ITEM }
                for (item in listItems) {
                    if (item is EditItemItem)
                        items[item.actualPos] = item
                }
                contentText = items.joinToString("\n") { it.content.text }
                metadata = ListNoteMetadata(items.map { it.checked })
            }
        }
        note = note.copy(
            folderId = folderId,
            title = titleText,
            content = contentText,
            colorNote = colorNote,
            isDrafted = NoteDraftStatus.NON_DRAFTED,
            lock = lock,
            noteAudios = noteAudios,
            noteImages = noteImage,
            noteTextFormat = format,
            metadata = metadata,
            status = status,
            pinned = pinned,
            reminder = reminder
        )
    }

    private suspend fun deleteNoteInternal() {
        notesRepository.deleteNote(note)
        reminderAlarmManager.removeAlarm(note.id)
    }

    fun deleteNoteForeverAndExit() {
        viewModelScope.launch {
            deleteNoteInternal()
        }
        exit()
    }

    private fun changeReminder() {
        _showReminderDialogEvent.send(note.id)
    }

    private fun changeLabels() {
        _showLabelsFragmentEvent.send(note.id)
    }

    fun onReminderChange(reminder: Reminder?) {
        this.reminder = reminder
        _noteReminder.value = reminder

        // Update reminder chip
        updateNote()
        recreateListItems()
    }

    fun toggleNoteType() {
        updateNote()

        // Convert note type
        note = when (note.type) {
            NoteType.TEXT -> note.asListNote()
            NoteType.LIST -> {
                if ((note.metadata as ListNoteMetadata).checked.any { it }) {
                    _showRemoveCheckedConfirmEvent.send()
                    return
                }
                else {
                    note.asTextNote(true)
                }
            }
        }
        _noteType.value = note.type

        // Update list items
        recreateListItems()

        // Go to first focusable item
        when (note.type) {
            NoteType.TEXT -> {
                val contentPos = listItems.indexOfLast { it is EditContentItem }
                focusItemAt(
                    contentPos,
                    (listItems[contentPos] as EditContentItem).content.text.length,
                    false
                )
            }

            NoteType.LIST -> {
                val lastItemPos = listItems.indexOfLast { it is EditItemItem }
                focusItemAt(
                    lastItemPos, (listItems[lastItemPos] as EditItemItem).content.text.length, false
                )
            }
        }
    }

    fun restoreNoteAndEdit(selection: Note) {
        note = selection.copy(status = NoteStatus.ACTIVE, pinned = PinnedStatus.UNPINNED)

        status = note.status
        pinned = note.pinned
        _noteStatus.value = status
        _notePinned.value = pinned

        // Recreate list items so that they are editable.
        recreateListItems()
    }

    fun shareNote() {
        updateNote()
        _shareEvent.send(ShareData(note.title, note.asText()))
    }

    private fun createLabelRefs(noteId: Long) = labels.map { LabelRef(noteId, it.id) }

    data class FocusChange(val itemPos: Int, val pos: Int, val itemExists: Boolean)

    private inline fun <reified T : EditListItem> findItem(): T {
        return (listItems.find { it is T } ?: error("List item not found")) as T
    }

    private inline fun <reified T : EditListItem> findItemPos(): Int {
        return listItems.indexOfFirst { it is T }
    }

    class DefaultEditableText(text: CharSequence = "") : EditableText {
        override val text = StringBuilder(text)

        override fun append(text: CharSequence) {
            this.text.append(text)
        }

        override fun replaceAll(text: CharSequence) {
            this.text.replace(0, this.text.length, text.toString())
        }

        override fun equals(other: Any?) =
            (other is DefaultEditableText && other.text.toString() == text.toString())

        override fun hashCode() = text.hashCode()

        override fun toString() = text.toString()
    }

    //Todo-List functions
    private fun deleteListItemAt(pos: Int) {
        val listItem = listItems[pos] as EditItemItem
        listItems.removeAt(pos)
        // Shift the actual pos of all items after this one
        for (item in listItems) {
            if (item is EditItemItem && item.actualPos > listItem.actualPos) {
                item.actualPos--
            }
        }
        updateListItems()
    }

    fun uncheckAllItems() {
        for ((i, item) in listItems.withIndex()) {
            if (item is EditItemItem && item.checked) {
                // FIXME breaks animation
                listItems[i] = item.copy(checked = false)
            }
        }
        moveCheckedItemsToBottom()
    }

    fun deleteCheckedItems() {
        listItems.removeAll { it is EditItemItem && it.checked }

        // Update actual pos of items by shifting down
        val itemsByActualPos = listItems.asSequence().filterIsInstance<EditItemItem>().sortedBy { it.actualPos }
        var lastActualPos = -1
        for (item in itemsByActualPos) {
            if (item.actualPos != lastActualPos + 1) {
                item.actualPos = lastActualPos + 1
            }
            lastActualPos = item.actualPos
        }

        moveCheckedItemsToBottom()
    }

    private fun moveCheckedItemsToBottom() {
        // Remove the whole checked group
        val checkedItems = listItems.asSequence().filterIsInstance<EditItemItem>().filter { it.checked }.toMutableList()
        val unCheckedItems = listItems.asSequence().filterIsInstance<EditItemItem>().filter { !it.checked }.toMutableList()

        Log.i( "unCheckedItems: ",unCheckedItems.size.toString())
        listItems.removeAll(checkedItems)
        listItems.removeAll { item ->
            item is EditCheckedHeaderItem
        }
        listItems.remove(EditItemAddItem)

        // Sort unchecked items by actual pos
        var lastUncheckedPos = listItems.indexOfLast { it is EditItemItem }
        if (lastUncheckedPos != -1) {
            lastUncheckedPos++
            var firstUncheckedPos = listItems.indexOfFirst { it is EditItemItem }

            //adding the header to the unchecked list
            if (firstUncheckedPos == 0){
                listItems.add(firstUncheckedPos, EditUncheckedHeaderItem(unCheckedItems.size))
                //incrementing the first and last positions of the list
                firstUncheckedPos++
                lastUncheckedPos++
            }

            listItems.subList(firstUncheckedPos, lastUncheckedPos).sortBy { (it as EditItemItem).actualPos }
        }
        else
            listItems.removeAll { it is EditUncheckedHeaderItem }

        // Re-add the checked group if any checked items, items sorted by actual pos
        var pos = lastUncheckedPos
        if (pos == -1)
            pos = 0

        listItems.add(pos, EditItemAddItem)
        pos++
        if (checkedItems.isNotEmpty()) {
            listItems.add(pos, EditCheckedHeaderItem(checkedItems.size))
            pos++
            checkedItems.sortBy { it.actualPos }
            for (item in checkedItems) {
                listItems.add(pos, item)
                pos++
            }
        }
        updateListItems()
    }

    override fun onNoteItemChanged(pos: Int, isPaste: Boolean) {

        val item = listItems[pos] as EditItemItem
        if ('\n' in item.content.text) {
            // User inserted line breaks in list items, split it into multiple items.
            // If this happens in the checked group when moving checked to the bottom, new items will be checked.
            val lines = item.content.text.split('\n')
            item.content.replaceAll(lines.first())
            for (listItem in listItems) {
                if (listItem is EditItemItem && listItem.actualPos > item.actualPos) {
                    listItem.actualPos += lines.size - 1
                }
            }
            for (i in 1 until lines.size) {
                listItems.add(
                    pos + i, EditItemItem(
                        DefaultEditableText(lines[i]),
                        checked = item.checked && moveCheckedToBottom,
                        editable = true,
                        item.actualPos + i
                    )
                )
            }

            moveCheckedItemsToBottom() // just to update checked count
            updateListItems()

            // If text was pasted, set focus at the end of last items pasted.
            // If a single linebreak was inserted, focus on the new item.
            focusItemAt(pos + lines.size - 1, if (isPaste) lines.last().length else 0, false)
        }
    }

    override fun onNoteItemCheckChanged(pos: Int, checked: Boolean) {

        Log.i("onNoteItemCheckChangedListSize: ",listItems.size.toString())

        val item = listItems[pos] as EditItemItem

        if (item.checked != checked) {
            item.checked = checked
            moveCheckedItemsToBottom()
        }
    }

    override fun onNoteItemBackspacePressed(pos: Int) {
        if (listItems.isNotEmpty()) {

            if (pos >= 1) {

                val prevItem = listItems[pos - 1]
                if (prevItem is EditItemItem) {
                    // Previous item is also a note list item. Merge the two items content,
                    // and delete the current item.
                    val prevText = prevItem.content
                    val prevLength = prevText.text.length
                    prevText.append((listItems[pos] as EditItemItem).content.text)
                    deleteListItemAt(pos)

                    // Set focus on merge boundary.
                    focusItemAt(pos - 1, prevLength, true)
                }
            }
        }
    }

    override fun onNoteItemDeleteClicked(pos: Int) {
        if (pos >= 1) {

            val prevItem = listItems[pos - 1]
            if (prevItem is EditItemItem) {
                // Set focus at the end of previous item.
                focusItemAt(pos - 1, prevItem.content.text.length, true)
            } else {
                val nextItem = listItems.getOrNull(pos + 1)
                if (nextItem is EditItemItem) {
                    // Set focus at the end of next item.
                    focusItemAt(pos + 1, nextItem.content.text.length, true)
                }
            }
            deleteListItemAt(pos)
        }
    }

    override fun onNoteItemAddClicked(pos: Int) {
        // pos is the position of EditItemAdd item, which is also the position to insert the new item.
        // The new item is added last, so the actual pos is the maximum plus one.
        val actualPos = listItems.maxOf { (it as? EditItemItem)?.actualPos ?: -1 } + 1
        listItems.add(
            pos, EditItemItem(DefaultEditableText(), checked = false, editable = true, actualPos)
        )
        updateListItems()
        focusItemAt(pos, 0, false)
    }

    override fun onNoteLabelClicked() {
        changeLabels()
    }

    override fun onNoteReminderClicked() {
        changeReminder()
    }

    override fun onNoteClickedToEdit() {
        if (isNoteInTrash) {
            // Cannot edit note in trash! Show message suggesting user to restore the note.
            // This is not just for show. Editing note would change its last modified date
            // which would mess up the auto-delete interval in trash.
            _messageEvent.send(EditMessage.CANT_EDIT_IN_TRASH)
        }
    }

    override fun onLinkClickedInNote(linkText: String, linkUrl: String) {
        this.linkUrl = linkUrl
    }

    override val isNoteDragEnabled: Boolean get() = !isNoteInTrash && listItems.count { it is EditItemItem } > 1

    override fun onNoteItemSwapped(from: Int, to: Int) {
        // Swap items actual positions in list note
        val fromItem = listItems[from] as EditItemItem
        val toItem = listItems[to] as EditItemItem
        val actualPosTemp = fromItem.actualPos
        fromItem.actualPos = toItem.actualPos
        toItem.actualPos = actualPosTemp

        // Don't update live data, adapter was notified of the change already.
        // However the live data value must be updated!
        Collections.swap(listItems, from, to)
        Collections.swap(_editItems.value!!, from, to)
    }

    override val strikethroughCheckedItems: Boolean get() = prefs.strikethroughChecked

    override val moveCheckedToBottom: Boolean get() = prefs.moveCheckedToBottom

    @AssistedFactory
    interface Factory : AssistedSavedStateViewModelFactory<AddEditNoteViewModel> {
        override fun create(savedStateHandle: SavedStateHandle): AddEditNoteViewModel
    }

    companion object {
        private val BLANK_NOTE = Note(
            Note.NO_ID,
            null,
            NoteType.TEXT,
            "",
            "",
            ColorNote("", isSelected = false),
            NoteDraftStatus.NON_DRAFTED,
            "",
            "",
            arrayListOf(),
            NoteTextFormat(
                spannable = false,
                fontPosition = -1
            ),
            BlankNoteMetadata,
            Date(0),
            Date(0),
            NoteStatus.ACTIVE,
            PinnedStatus.UNPINNED,
            null
        )

        private const val KEY_NOTE_ID = "noteId"
        private const val KEY_IS_NEW_NOTE = "isNewNote"
        private const val KEY_LINK_URL = "linkUrl"

        private val TEMP_ITEM = EditItemItem(DefaultEditableText(), checked = false, editable = false, actualPos = 0)
    }
}