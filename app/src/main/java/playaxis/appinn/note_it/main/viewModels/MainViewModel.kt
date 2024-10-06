package playaxis.appinn.note_it.main.viewModels

import android.view.Menu
import android.view.MenuItem
import androidx.core.view.contains
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDirections
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import playaxis.appinn.note_it.MainNavigationDirections
import playaxis.appinn.note_it.R
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.draw.canvas.DrawingView
import playaxis.appinn.note_it.main.utils.Event
import playaxis.appinn.note_it.main.utils.send
import playaxis.appinn.note_it.navigation.HomeDestination
import playaxis.appinn.note_it.preferences.SharedPreference
import playaxis.appinn.note_it.repository.folder.FoldersRepository
import playaxis.appinn.note_it.repository.label.LabelsRepository
import playaxis.appinn.note_it.repository.model.entities.FolderNote
import playaxis.appinn.note_it.repository.model.entities.Label
import playaxis.appinn.note_it.repository.model.entities.NoteStatus
import playaxis.appinn.note_it.repository.model.entities.NoteType
import playaxis.appinn.note_it.repository.note.NotesRepository

class MainViewModel @AssistedInject constructor(
    private var notesRepository: NotesRepository,
    private var folderRepository: FoldersRepository,
    private var labelsRepository: LabelsRepository,
    @Assisted savedStateHandle: SavedStateHandle
) : ViewModel(){

    private val _searchBarLayoutListButton = MutableLiveData<Event<Int>>()
    val searchBarLayoutListButton: LiveData<Event<Int>> get() = _searchBarLayoutListButton

    private val _bottomBarItemsListeners = MutableLiveData<Event<String>>()
    val bottomBarItemsListeners: LiveData<Event<String>> get() = _bottomBarItemsListeners

    private var labelsJob: Job? = null

    private val _deletionFinishedMutex = Mutex(locked = true)

    private val _currentHomeDestination = savedStateHandle.getLiveData<HomeDestination>(KEY_HOME_DESTINATION, HomeDestination.Status(NoteStatus.ACTIVE))
    val currentHomeDestination: LiveData<HomeDestination> get() = _currentHomeDestination

    private val _navDirectionsEvent = MutableLiveData<Event<NavDirections>>()
    val navDirectionsEvent: LiveData<Event<NavDirections>> get() = _navDirectionsEvent

    private val _clearLabelsEvent = MutableLiveData<Event<Unit>>()
    val clearLabelsEvent: LiveData<Event<Unit>> get() = _clearLabelsEvent

    private val _labelsAddEvent = MutableLiveData<Event<List<Label>?>>()
    val labelsAddEvent: LiveData<Event<List<Label>?>> get() = _labelsAddEvent

    private val _backPressDispatcherEvent = MutableLiveData<Event<Unit>>()
    val backPressDispatcherEvent: LiveData<Event<Unit>> get() = _backPressDispatcherEvent

    private val _defaultHomeViewShowEvent = MutableLiveData<Event<Unit>>()
    val defaultHomeViewShowEvent: LiveData<Event<Unit>> get() = _defaultHomeViewShowEvent

    private val _createNoteEvent = MutableLiveData<Event<NewNoteData>>()
    val createNoteEvent: LiveData<Event<NewNoteData>>
        get() = _createNoteEvent

    private val _editNoteEvent = MutableLiveData<Event<Long>>()
    val editItemEvent: LiveData<Event<Long>>
        get() = _editNoteEvent

    private val _drawerCloseEvent = MutableLiveData<Event<Unit>>()
    val drawerCloseEvent: LiveData<Event<Unit>>
        get() = _drawerCloseEvent

    private val _bottomFragmentItemDetachEvent = MutableLiveData<Event<Unit>>()
    val bottomFragmentItemDetachEvent: LiveData<Event<Unit>>
        get() = _bottomFragmentItemDetachEvent

    private val _saveNoteOnBackPressEvent = MutableLiveData<Event<Unit>>()
    val saveNoteOnBackPressEvent: LiveData<Event<Unit>>
        get() = _saveNoteOnBackPressEvent

    private val _drawingClickEvent = MutableLiveData<Event<ArrayList<DrawingView.CustomPath>>>()
    val drawingClickEvent: LiveData<Event<ArrayList<DrawingView.CustomPath>>>
        get() = _drawingClickEvent

    private val _openGalleryEvent = MutableLiveData<Event<Unit>>()
    val openGalleryEvent: LiveData<Event<Unit>>
        get() = _openGalleryEvent

    //from scanned screen
    val recognizedTextLiveData: MutableLiveData<Event<String>> = MutableLiveData()

    //value to handle view on transactions
    var noteImageNotClicked = true
    var moveFolderNote = false

    private val _labelCreateEvent = MutableLiveData<Event<Unit>>()
    val labelCreateEvent: LiveData<Event<Unit>>
        get() = _labelCreateEvent

    var noteMoveFolder: String = ""


    init {

        viewModelScope.launch {

            // Check if last added note is blank, in which case delete it.
            val lastCreatedNote = notesRepository.getLastCreatedNote()
            if (lastCreatedNote?.isBlank == true) {
                notesRepository.deleteNote(lastCreatedNote)
            }
            _deletionFinishedMutex.unlock()
        }

        //adding items to database on first run
        insertFoldersFirstItems()
    }

    fun openGalleryEvent(){
        _openGalleryEvent.send()
    }

    fun drawingClickEvent(drawing: ArrayList<DrawingView.CustomPath>){
        _drawingClickEvent.send(drawing)
    }

    //update search bar view
    fun updateLayoutListButtonVisibility(visibility: Int) {
        _searchBarLayoutListButton.send(visibility)
    }

    //update toolbar view
    fun bottomBarItemsListenerEvent(fragment: String) {
        _bottomBarItemsListeners.send(fragment)
    }

    fun onBackPressed() {
        _backPressDispatcherEvent.send()
    }

    fun bottomFragmentItemDetachEventEvent() {
        _bottomFragmentItemDetachEvent.send()
    }

    fun saveNoteOnBackPressEvent(){
        _saveNoteOnBackPressEvent.send()
    }

    private fun insertFoldersFirstItems(){

        if (SharedPreference.isFirstRun()){

            viewModelScope.launch(Dispatchers.IO) {
                folderRepository.insertFolder(FolderNote(FolderNote.NO_ID,"Office"))
                folderRepository.insertFolder(FolderNote(FolderNote.NO_ID,"Home"))
            }

            //making first run false
            SharedPreference.setFirstRunCompleted()
        }
    }

    fun refreshList(){
        _currentHomeDestination.value = HomeDestination.Status(NoteStatus.ACTIVE)
    }

    fun navigationItemSelected(item: MenuItem, labelsMenu: Menu) {
        _drawerCloseEvent.send()

        when (item.itemId) {
            R.id.drawer_item_notes -> {
                _currentHomeDestination.value = HomeDestination.Status(NoteStatus.ACTIVE)
                _navDirectionsEvent.send(MainNavigationDirections.mainToHomeFragment())
            }
            R.id.drawer_item_reminder -> {
                _currentHomeDestination.value = HomeDestination.Reminders
                _navDirectionsEvent.send(MainNavigationDirections.mainToReminderFragment())
            }
            R.id.drawer_item_create_label -> {
                _navDirectionsEvent.send(MainNavigationDirections.mainToCreateEditLabelFragment(longArrayOf()))
                //fire event for handling event
                _labelCreateEvent.send()
            }
            R.id.drawer_item_archived -> {
                _currentHomeDestination.value = HomeDestination.Status(NoteStatus.ARCHIVED)
                _navDirectionsEvent.send(MainNavigationDirections.mainToArchiveFragment())
            }
            R.id.drawer_item_trash -> {
                _currentHomeDestination.value = HomeDestination.Status(NoteStatus.DELETED)
                _navDirectionsEvent.send(MainNavigationDirections.mainToTrashFragment())
            }
            R.id.drawer_item_premium -> {
                _navDirectionsEvent.send(MainNavigationDirections.mainToPremiumFragment())
            }
            R.id.drawer_item_rate_us -> {
                _navDirectionsEvent.send(MainNavigationDirections.mainToRateUsFragment())
            }
        }

        // Navigate to label, if it has been selected
        if (labelsMenu.contains(item)) {
            viewModelScope.launch {
                val label = labelsRepository.getLabelByName(item.title as String)
                if (label != null)
                    selectLabel(label)
            }
        }
    }
    fun startPopulatingDrawerWithLabels() {
        labelsJob?.cancel()
        // Coroutine to populate drawer with labels
        labelsJob = viewModelScope.launch {
            var oldLabelsList: List<Label> = listOf()
            labelsRepository.getAllLabelsByUsage().collect { labelsList ->
                if (oldLabelsList != labelsList) {
                    oldLabelsList = labelsList

                    // Check if the currently shown label still exists.
                    // If the label has been deleted, navigate to the main notes view
                    if (_currentHomeDestination.value is HomeDestination.Labels) {
                        if ((_currentHomeDestination.value as HomeDestination.Labels).label !in labelsList) {
                            _currentHomeDestination.value = HomeDestination.Status(NoteStatus.ACTIVE)
                        }
                    }

                    // Update the labels in the navigation drawer
                    _clearLabelsEvent.send()
                    _labelsAddEvent.send(labelsList)
                }
            }
        }
    }
    private fun selectLabel(label: Label) {
        _currentHomeDestination.value = HomeDestination.Labels(label)
    }

    //for handling onResume of Main Activity
    fun createNote(newNoteData: NewNoteData) {
        viewModelScope.launch {
            // Wait until older notes have been checked / deleted
            _deletionFinishedMutex.withLock {
                _createNoteEvent.send(newNoteData)
            }
        }
    }

    fun editNote(id: Long) {
        viewModelScope.launch {
            // If note doesn't exist, EditFragment would be opened to create a new note without this check.
            if (notesRepository.getNoteById(id) != null) {
                _editNoteEvent.send(id)
            }
        }
    }

    @AssistedFactory
    interface Factory : AssistedSavedStateViewModelFactory<MainViewModel> {
        override fun create(savedStateHandle: SavedStateHandle): MainViewModel
    }
    data class NewNoteData(val type: NoteType, val title: String = "", val content: String = "")

    companion object {
        private const val KEY_HOME_DESTINATION = "destination"
    }
}