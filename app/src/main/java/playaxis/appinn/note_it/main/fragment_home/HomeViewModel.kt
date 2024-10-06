package playaxis.appinn.note_it.main.fragment_home

import android.media.MediaRecorder
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import playaxis.appinn.note_it.R
import playaxis.appinn.note_it.extensions.setToStartOfDay
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.note.viewmodels.AddEditNoteViewModel
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.speech.helper.Speech
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.todo.utils.EditItemItem
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.todo.utils.EditListItem
import playaxis.appinn.note_it.main.fragment_home.fragments.folder.adapter.FolderListAdapter
import playaxis.appinn.note_it.main.fragment_home.fragments.note.NoteViewModel
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.NoteAdapter
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.HeaderItem
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.MessageItem
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.NoteItem
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.NoteItemFactory
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.NoteListItem
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.PlaceholderData
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.SwipeAction
import playaxis.appinn.note_it.main.utils.Event
import playaxis.appinn.note_it.main.utils.StatusChange
import playaxis.appinn.note_it.main.utils.send
import playaxis.appinn.note_it.main.viewModels.AssistedSavedStateViewModelFactory
import playaxis.appinn.note_it.navigation.HomeDestination
import playaxis.appinn.note_it.preferences.PrefsManager
import playaxis.appinn.note_it.repository.folder.FoldersRepository
import playaxis.appinn.note_it.repository.label.LabelsRepository
import playaxis.appinn.note_it.repository.model.entities.BlankNoteMetadata
import playaxis.appinn.note_it.repository.model.entities.ColorNote
import playaxis.appinn.note_it.repository.model.entities.FolderWithNotes
import playaxis.appinn.note_it.repository.model.entities.Label
import playaxis.appinn.note_it.repository.model.entities.Note
import playaxis.appinn.note_it.repository.model.entities.NoteDraftStatus
import playaxis.appinn.note_it.repository.model.entities.NoteStatus
import playaxis.appinn.note_it.repository.model.entities.NoteTextFormat
import playaxis.appinn.note_it.repository.model.entities.NoteType
import playaxis.appinn.note_it.repository.model.entities.NoteWithLabels
import playaxis.appinn.note_it.repository.model.entities.PinnedStatus
import playaxis.appinn.note_it.repository.model.entities.Reminder
import playaxis.appinn.note_it.repository.model.utils.ReminderAlarmManager
import playaxis.appinn.note_it.repository.model.utils.SortDirection
import playaxis.appinn.note_it.repository.model.utils.SortField
import playaxis.appinn.note_it.repository.model.utils.SortSettings
import playaxis.appinn.note_it.repository.note.NotesRepository
import playaxis.appinn.note_it.utils.debugCheck
import java.io.File
import java.util.Calendar
import java.util.Date

open class HomeViewModel @AssistedInject constructor(
    @Assisted savedStateHandle: SavedStateHandle,
    notesRepository: NotesRepository,
    labelsRepository: LabelsRepository,
    foldersRepository: FoldersRepository,
    prefs: PrefsManager,
    reminderAlarmManager: ReminderAlarmManager,
    noteItemFactory: NoteItemFactory,
) : NoteViewModel(savedStateHandle, notesRepository, labelsRepository, foldersRepository,prefs, noteItemFactory, reminderAlarmManager), NoteAdapter.Callback,
    FolderListAdapter.FolderItemCLickEvent {

    private var batteryRestricted = false
    private var notificationsRestricted = false
    private var remindersRestricted = false
    var openGalleryEventVal = false

    protected var currentDestination: HomeDestination = HomeDestination.Status(NoteStatus.ACTIVE)

    private val _moveToFolderSelectionEvent = MutableLiveData<Event<String>>()
    val moveToFolderSelectionEvent: LiveData<Event<String>> get() = _moveToFolderSelectionEvent

    private val _folderSelectionListEvent = MutableLiveData<Event<Unit>>()
    val folderSelectionListEvent: LiveData<Event<Unit>> get() = _folderSelectionListEvent

    private val _createNoteEvent = MutableLiveData<Event<NewNoteSettings>>()
    val createNoteEvent: LiveData<Event<NewNoteSettings>>
        get() = _createNoteEvent

    private val _shiftToTodoListEvent = MutableLiveData<Event<Unit>>()
    val shiftToTodoListEvent: LiveData<Event<Unit>>
        get() = _shiftToTodoListEvent

    private val _showEmptyTrashDialogEvent = MutableLiveData<Event<Unit>>()
    val showEmptyTrashDialogEvent: LiveData<Event<Unit>>
        get() = _showEmptyTrashDialogEvent

    private val _reminderChangeEvent = MutableLiveData<Event<Reminder?>>()
    val reminderChangeEvent: LiveData<Event<Reminder?>>
        get() = _reminderChangeEvent

    private val _labelAddEventNav = MutableLiveData<Event<Label>>()
    val labelAddEventNav: LiveData<Event<Label>>
        get() = _labelAddEventNav

    private val _labelAddEventSelect = MutableLiveData<Event<Label>>()
    val labelAddEventSelect: LiveData<Event<Label>>
        get() = _labelAddEventSelect

    private val _sortChangeEvent = MutableLiveData<Event<SortSettings>>()
    val sortChangeEvent: LiveData<Event<SortSettings>>
        get() = _sortChangeEvent

    private val _sharedElementTransitionFinishedEvent = MutableLiveData<Event<Unit>>()
    val sharedElementTransitionFinishedEvent: LiveData<Event<Unit>>
        get() = _sharedElementTransitionFinishedEvent

    private val _noteCreatedEvent = MutableLiveData<Event<Long>>()
    val noteCreatedEvent: LiveData<Event<Long>> get() = _noteCreatedEvent

    private var lastStatusChange: StatusChange? = null

    //close app event
    private val _closeAppBackPressedEvent = MutableLiveData<Event<Unit>>()
    val closeAppBackPressedEvent: LiveData<Event<Unit>> get() = _closeAppBackPressedEvent

    private val _saveLabelEvent = MutableLiveData<Event<Unit>>()
    val saveLabelEvent: LiveData<Event<Unit>> get() = _saveLabelEvent

    private val _moveToFolderActionEvent = MutableLiveData<Event<Unit>>()
    val moveToFolderActionEvent: LiveData<Event<Unit>> get() = _moveToFolderActionEvent

    private val _moveToFolderOperationCompleteEvent = MutableLiveData<Event<Unit>>()
    val moveToFolderOperationCompleteEvent: LiveData<Event<Unit>> get() = _moveToFolderOperationCompleteEvent

    private val _folderItemClickEvent = MutableLiveData<Event<FolderWithNotes>>()
    val folderItemClickEvent: LiveData<Event<FolderWithNotes>> get() = _folderItemClickEvent

    private val _folderItemArgsEvent = MutableLiveData<Event<FolderWithNotes>>()
    val folderItemArgsEvent: LiveData<Event<FolderWithNotes>> get() = _folderItemArgsEvent

    private val _homeFragmentsBackButtonTransactionEvent = MutableLiveData<Event<Unit>>()
    val homeFragmentsBackButtonTransactionEvent: LiveData<Event<Unit>> get() = _homeFragmentsBackButtonTransactionEvent

    private val _loadImageFromBottomDialogEvent = MutableLiveData<Event<Uri?>>()
    val loadImageFromBottomDialogEvent: LiveData<Event<Uri?>> get() = _loadImageFromBottomDialogEvent

    private val _moveToFolderToolbarNormalizeEvent = MutableLiveData<Event<Unit>>()
    val moveToFolderToolbarNormalizeEvent: LiveData<Event<Unit>> get() = _moveToFolderToolbarNormalizeEvent

    private val _cameraToolbarNormalizeEvent = MutableLiveData<Event<Unit>>()
    val cameraToolbarNormalizeEvent: LiveData<Event<Unit>> get() = _cameraToolbarNormalizeEvent

    private val _toolbarListenerEvent = MutableLiveData<Event<Unit>>()
    val toolbarListenerEvent: LiveData<Event<Unit>> get() = _toolbarListenerEvent

    private val _saveInstanceOfNoteEvent = MutableLiveData<Event<Unit>>()
    val saveInstanceOfNoteEvent: LiveData<Event<Unit>> get() = _saveInstanceOfNoteEvent

    private val _messageEvent = MutableLiveData<Event<Int>>()
    val messageEvent: LiveData<Event<Int>> get() = _messageEvent

    private val _editItemsCopy = MutableLiveData<MutableList<EditListItem>>()
    val editItemsCopy: LiveData<MutableList<EditListItem>> get() = _editItemsCopy

    //to know that folderSelection is true or not to handle the view
    var selectedFolder: FolderWithNotes? = null

    //to know that the drawing is being edited
    var drawingEdit = false

    var tabSelected = false

    //image from gallery or canvas (both will set data in it)
    var imageNote = ArrayList<String>()

    //audio files for temporary storage
    var noteAudios = ArrayList<Speech>()

    //image to scan
    var scanImage = ""

    //camera note identifier
    var cameraNote = false
    //new drawing identifier
    var newDrawing = false

    //identifier
    var editing = false

    //identifier for the camera screen from main activity
    var scanScreen = false

    private var mediaRecorder: MediaRecorder?
    private lateinit var outputFile: File

    //to keep track of the child home fragment transactions
    lateinit var adapter: FolderListAdapter

    private var note = BLANK_NOTE

    //just to retain the data
    var title: String? = null
    var content: String? = null
    var folderId: Long? = null
    var locked: String = ""
    var status = note.status

    var noteTextFormat: NoteTextFormat = NoteTextFormat(spannable = false, fontPosition = -1)
    var colorNote: ColorNote = ColorNote("", false)
    var pinned = note.pinned

    private var noteItemsList: MutableList<EditListItem>? = null

    init {
        mediaRecorder = MediaRecorder()
        changeSortSettings(SortSettings(SortField.ADDED_DATE, SortDirection.ASCENDING))
    }

    fun editItemsCopy(noteItemsList: MutableList<EditListItem>){
        _editItemsCopy.value = noteItemsList
    }

    fun saveInstanceOfNoteEvent(){
        _saveInstanceOfNoteEvent.send()
    }

    fun onBlankNoteDiscarded() {
        // Not shown from EditFragment so that FAB is pushed up.
        _messageEvent.send(R.string.edit_message_blank_note_discarded)
    }

    fun toolbarListenerEvent(){
        _toolbarListenerEvent.send()
    }
    fun emptyTrashDialogEvent(){
        _showEmptyTrashDialogEvent.send()
    }

    fun cameraToolbarNormalizeEvent(){
        _cameraToolbarNormalizeEvent.send()
    }

    fun moveToFolderToolbarNormalizeEvent(){
        _moveToFolderToolbarNormalizeEvent.send()
    }

    fun backPressOfHomeChildFragments(){
        _homeFragmentsBackButtonTransactionEvent.send()
    }

    fun folderItemArgsNoteListEvent(folderNote: FolderWithNotes){
        _folderItemArgsEvent.send(folderNote)
    }

    fun folderItemClickEvent(folderNote: FolderWithNotes) {
        _folderItemClickEvent.send(folderNote)
    }

    fun moveToFolderOperationCompleted(){
        _moveToFolderOperationCompleteEvent.send()
    }
    fun folderSelectionListEvent(){
        _folderSelectionListEvent.send()
    }

    fun setAudioFile(outputFile: File){
        this.outputFile = outputFile
    }

    fun startRecording() {
        try {
            mediaRecorder = MediaRecorder()
            mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mediaRecorder?.setAudioEncodingBitRate(128000)
            mediaRecorder?.setAudioSamplingRate(16000)
            mediaRecorder!!.setOutputFile(outputFile.absolutePath)
            mediaRecorder?.prepare()
            mediaRecorder?.start()

            // Add logic to stop recording after a certain duration or based on user interaction
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Stop listening (call this when you want to stop recording)
    fun stopRecording(): String {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.reset()
            mediaRecorder?.release()
            mediaRecorder = null

        }
        catch (e: Exception) {
            e.printStackTrace()
        }

        // Add logic to send the recorded audio file to the Google Cloud Speech-to-Text API for recognition
        return outputFile.absolutePath
    }

    fun addNewNoteToSelection(note: Note){
        addNoteToSelection(note)
    }

    fun closeApp(){
        _closeAppBackPressedEvent.send()
    }

    //only called when certain element is clicked in the main list
    fun getClickedNote(): Note{
        return selectedNotes.elementAt(0)
    }

    fun saveLabelEvent(){
        _saveLabelEvent.send()
    }

    fun setMoveToFolderSelectionEvent(noteType: String){
        _moveToFolderSelectionEvent.send(noteType)
    }

    fun setMoveToFolderActionEvent(){
        _moveToFolderActionEvent.send()
    }

    fun createNote() {
        val destination = currentDestination
        _createNoteEvent.send(
            NewNoteSettings(
                if (destination is HomeDestination.Labels)
                    destination.label.id
                else
                    Label.NO_ID, destination is HomeDestination.Reminders
            ))
    }

    fun setDestination(destination: HomeDestination) {
        currentDestination = destination
        updateNoteList()
    }

    //Need to decide the data flow in between screens from view model!
    private fun updateNoteList() {
        // Update note items live data when database flow emits a list.
        val destination = currentDestination
        noteListJob?.cancel()
        noteListJob = viewModelScope.launch(Dispatchers.IO) {
            waitForRestoredState()

            when (destination) {
                is HomeDestination.Status -> {
                    notesRepository.getNotesByStatus(destination.status).collect { notes ->

                        Log.i("updateNoteList: ",notes.toString())

                        listItems = when (destination.status) {
                            NoteStatus.ACTIVE -> createActiveListItems(notes)
                            NoteStatus.ARCHIVED -> createArchivedListItems(notes)
                            NoteStatus.DELETED -> createDeletedListItems(notes)
                        }
                    }
                }
                is HomeDestination.Labels -> {
                    notesRepository.getNotesByLabel(destination.label.id).collect { notes ->
                        listItems = createLabelsListItems(notes, destination.label)
                    }
                }
                is HomeDestination.Reminders -> {
                    notesRepository.getNotesWithReminder().collect { notes ->
                        listItems = createRemindersListItems(notes)
                    }
                }
            }
        }
    }

    private fun createActiveListItems(notes: List<NoteWithLabels>): List<NoteListItem> = buildList {

        // Separate pinned and not pinned notes with headers
        var addedPinnedHeader = false
        var addedNotPinnedHeader = false

        if (notes.isNotEmpty() && notes.first().note.pinned == PinnedStatus.PINNED) {
            this += PINNED_HEADER_ITEM
            addedPinnedHeader = true
        }

        for (note in notes) {
            if (addedPinnedHeader && !addedNotPinnedHeader && note.note.pinned == PinnedStatus.UNPINNED) {
                this += NOT_PINNED_HEADER_ITEM
                addedNotPinnedHeader = true
            }
            addNoteItem(note)
        }
    }

    private fun createArchivedListItems(notes: List<NoteWithLabels>) = buildList {
        for (note in notes) {
            addNoteItem(note)
        }
    }

    private fun createDeletedListItems(notes: List<NoteWithLabels>) = buildList {
        for (note in notes) {
            addNoteItem(note)
        }
    }

    private fun createLabelsListItems(notes: List<NoteWithLabels>, label: Label) = buildList {
        // Separate pinned, active and archived notes with headers
        var addedPinnedHeader = false
        var addedNotPinnedHeader = false
        var addedArchivedHeader = false

        if (notes.isNotEmpty() && notes.first().note.pinned == PinnedStatus.PINNED) {
            this += PINNED_HEADER_ITEM
            addedPinnedHeader = true
        }

        for (noteWithLabels in notes) {
            val note = noteWithLabels.note

            // Add headers if necessary
            if (!addedArchivedHeader && note.status == NoteStatus.ARCHIVED) {
                this += ARCHIVED_HEADER_ITEM
                addedArchivedHeader = true
            } else if (addedPinnedHeader && !addedNotPinnedHeader &&
                note.pinned == PinnedStatus.UNPINNED
            ) {
                this += NOT_PINNED_HEADER_ITEM
                addedNotPinnedHeader = true
            }

            // Omit the filtered label from the note since all notes have it.
            addNoteItem(noteWithLabels, excludeLabel = label)
        }
    }

    private fun createRemindersListItems(notes: List<NoteWithLabels>) = buildList {
        val calendar = Calendar.getInstance()
        calendar.setToStartOfDay()
        calendar.add(Calendar.DATE, 1)
        val endOfToday = calendar.timeInMillis
        val now = System.currentTimeMillis()

        // If needed, add warning that notifications won't work properly if battery is restricted.
        if (batteryRestricted && notes.isNotEmpty() && now - prefs.lastRestrictedBatteryReminderTime >
            PrefsManager.RESTRICTED_BATTERY_REMINDER_DELAY.inWholeMilliseconds) {
            this += MessageItem(BATTERY_RESTRICTED_ITEM_ID, R.string.reminder_restricted_battery)
        }

        // If needed, add warning that notification permission has been denied.
        if (notes.isNotEmpty() && notificationsRestricted) {
            this += MessageItem(NOTIFICATION_DENIED_ITEM_ID, R.string.reminder_notif_permission_denied)
        }
        if (notes.isNotEmpty() && remindersRestricted) {
            this += MessageItem(REMINDER_DENIED_ITEM_ID, R.string.reminder_alarm_permission_denied)
        }

        var addedOverdueHeader = false
        var addedTodayHeader = false
        var addedUpcomingHeader = false

        for (noteWithLabels in notes) {
            val reminderTime = (noteWithLabels.note.reminder ?: continue).next.time

            if (!addedOverdueHeader && reminderTime <= now) {
                // Reminder is past, add overdue header before it
                this += OVERDUE_HEADER_ITEM
                addedOverdueHeader = true
            } else if (!addedTodayHeader && reminderTime > now && reminderTime < endOfToday) {
                // Reminder is today but hasn't happened yet, add today header before it.
                this += TODAY_HEADER_ITEM
                addedTodayHeader = true
            } else if (!addedUpcomingHeader && reminderTime >= endOfToday) {
                // Reminder is after the end of today, add upcoming header before it.
                this += UPCOMING_HEADER_ITEM
                addedUpcomingHeader = true
            }

            // Show "Mark as done" action button.
            addNoteItem(noteWithLabels, showMarkAsDone = reminderTime <= now)
        }
    }

    override fun updatePlaceholder() = when (val destination = currentDestination) {

        is HomeDestination.Status -> when (destination.status) {
            NoteStatus.ACTIVE -> PlaceholderData(R.drawable.no_items_icon, R.string.note_placeholder_active)
            NoteStatus.ARCHIVED -> PlaceholderData(R.drawable.no_items_icon, R.string.note_placeholder_archived)
            NoteStatus.DELETED -> PlaceholderData(R.drawable.no_items_icon, R.string.note_placeholder_deleted)
        }
        is HomeDestination.Reminders -> {
            PlaceholderData(R.drawable.no_items_icon, R.string.reminder_empty_placeholder)
        }
        is HomeDestination.Labels -> {
            PlaceholderData(R.drawable.no_items_icon, R.string.label_notes_empty_placeholder)
        }
    }

    override val selectedNoteStatus: NoteStatus? get() {
        val destination = currentDestination
        return if (destination is HomeDestination.Status) {
            // Only same status notes in this destination
            destination.status
        }
        else {
            // If one note is active, consider all active.
            // Otherwise consider archived.
            debugCheck(selectedNotes.none { it.status == NoteStatus.DELETED })
            when {
                selectedNotes.isEmpty() -> null
                selectedNotes.any { it.status == NoteStatus.ACTIVE } -> NoteStatus.ACTIVE
                else -> NoteStatus.ARCHIVED
            }
        }
    }

    /** Update restrictions status so that appropriate warnings may be shown to user. */
    fun updateRestrictions(battery: Boolean, notifications: Boolean, reminders: Boolean) {
        val updateList = battery != batteryRestricted || notifications != notificationsRestricted || reminders != remindersRestricted
        batteryRestricted = battery
        notificationsRestricted = notifications
        remindersRestricted = reminders
        if (updateList) {
            updateNoteList()
        }
    }

    private fun MutableList<NoteListItem>.addNoteItem(noteWithLabels: NoteWithLabels, showMarkAsDone: Boolean = false, excludeLabel: Label? = null) {
        val note = noteWithLabels.note
        val checked = isNoteSelected(note)
        val labels = if (excludeLabel == null) {
            noteWithLabels.labels
        } else {
            noteWithLabels.labels - excludeLabel
        }
        this += noteItemFactory.createItem(note, labels, checked, showMarkAsDone)
    }

    fun onStatusChange(statusChange: StatusChange) {
        lastStatusChange = statusChange
        _statusChangeEvent.send(statusChange)
    }

    fun undoStatusChange() {
        val change = lastStatusChange ?: return
        viewModelScope.launch {
            notesRepository.updateNotes(change.oldNotes)
        }

        if (change.newStatus == NoteStatus.DELETED) {
            // Notes were deleted, removing any reminder alarm that had been set. Set them back.
            for (note in change.oldNotes) {
                if (note.reminder != null) {
                    reminderAlarmManager.setNoteReminderAlarm(note)
                }
            }
        }

        lastStatusChange = null
    }

    fun onReminderChange(reminder: Reminder?) {
        _reminderChangeEvent.send(reminder)
    }

    fun onLabelAdd(label: Label) {
        _labelAddEventNav.send(label)
        _labelAddEventSelect.send(label)
    }

    private fun changeSortSettings(settings: SortSettings) {
        _sortChangeEvent.send(settings)
    }

    fun sharedElementTransitionFinished() {
        _sharedElementTransitionFinishedEvent.send()
    }

    fun noteCreated(noteId: Long) {
        _noteCreatedEvent.send(noteId)
    }

    fun emptyTrash() {
        viewModelScope.launch {
            notesRepository.emptyTrash()
        }
    }

    override fun onMessageItemDismissed(item: MessageItem, pos: Int) {
        val now = System.currentTimeMillis()
        when (item.id) {
            TRASH_REMINDER_ITEM_ID -> prefs.lastTrashReminderTime = now
            BATTERY_RESTRICTED_ITEM_ID -> prefs.lastRestrictedBatteryReminderTime = now
        }

        // Remove message item in list
        changeListItems { it.removeAt(pos) }
    }

    override fun onNoteActionButtonClicked(item: NoteItem, pos: Int) {
        // Action button is only shown for "Mark as done" in Reminders screen.
        // So mark reminder as done.
        viewModelScope.launch {
            val note = item.note
            notesRepository.updateNote(note.copy(reminder = note.reminder?.markAsDone()))
        }
    }

    override fun getNoteSwipeAction(direction: NoteAdapter.SwipeDirection) : SwipeAction{
        return if (currentDestination == HomeDestination.Status(NoteStatus.ACTIVE) && selectedNotes.isEmpty()) {
            when (direction) {
                NoteAdapter.SwipeDirection.LEFT -> prefs.swipeActionLeft
                NoteAdapter.SwipeDirection.RIGHT -> prefs.swipeActionRight
            }
        } else {
            SwipeAction.NONE
        }
    }

    override fun onNoteSwiped(pos: Int, direction: NoteAdapter.SwipeDirection) {
        val note = (noteItems.value!![pos] as NoteItem).note
        changeNotesStatus(setOf(note), when (getNoteSwipeAction(direction)) {
            SwipeAction.ARCHIVE -> NoteStatus.ARCHIVED
            SwipeAction.DELETE -> NoteStatus.DELETED
            SwipeAction.NONE -> return  // should not happen
        })
    }

    @AssistedFactory
    interface Factory : AssistedSavedStateViewModelFactory<HomeViewModel> {
        override fun create(savedStateHandle: SavedStateHandle): HomeViewModel
    }

    //new note settings
    data class NewNoteSettings(val labelId: Long, val initialReminder: Boolean)

    companion object {
        private const val TRASH_REMINDER_ITEM_ID = -1L
        private const val BATTERY_RESTRICTED_ITEM_ID = -8L
        private const val NOTIFICATION_DENIED_ITEM_ID = -10L
        private const val REMINDER_DENIED_ITEM_ID = -11L

        val PINNED_HEADER_ITEM = HeaderItem(-2, R.string.note_pinned)
        val NOT_PINNED_HEADER_ITEM = HeaderItem(-3, R.string.note_not_pinned)
        val TODAY_HEADER_ITEM = HeaderItem(-4, R.string.reminder_today)
        val OVERDUE_HEADER_ITEM = HeaderItem(-5, R.string.reminder_overdue)
        val UPCOMING_HEADER_ITEM = HeaderItem(-6, R.string.reminder_upcoming)
        val ARCHIVED_HEADER_ITEM = HeaderItem(-1, R.string.note_location_archived)

        private val TEMP_ITEM = EditItemItem(AddEditNoteViewModel.DefaultEditableText(), checked = false, editable = false, actualPos = 0)

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
    }

    override fun folderClicked(folderNote: FolderWithNotes, position: Int) {
        //do nothing
    }
}