package playaxis.appinn.note_it.main.drawer_fragments.label.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import playaxis.appinn.note_it.main.drawer_fragments.label.adapters.LabelAdapter
import playaxis.appinn.note_it.main.drawer_fragments.label.adapters.LabelListItem
import playaxis.appinn.note_it.main.utils.Event
import playaxis.appinn.note_it.main.utils.send
import playaxis.appinn.note_it.main.viewModels.AssistedSavedStateViewModelFactory
import playaxis.appinn.note_it.repository.label.LabelsRepository
import playaxis.appinn.note_it.repository.model.entities.Label
import playaxis.appinn.note_it.repository.model.entities.LabelRef

class LabelViewModel @AssistedInject constructor(
    private val labelsRepository: LabelsRepository,
    @Assisted private val savedStateHandle: SavedStateHandle,
) : ViewModel(), LabelAdapter.Callback {

    private val _labelItems = MutableLiveData<List<LabelListItem>>()
    val labelItems: LiveData<List<LabelListItem>>
        get() = _labelItems

    // current number of selected labels (only when managing labels)
    private val _labelSelection = MutableLiveData<Int>()
    val labelSelection: LiveData<Int>
        get() = _labelSelection

    private val _placeholderShown = MutableLiveData<Boolean>()
    val placeholderShown: LiveData<Boolean>
        get() = _placeholderShown

    private val _showDeleteConfirmEvent = MutableLiveData<Event<Unit>>()
    val showDeleteConfirmEvent: LiveData<Event<Unit>>
        get() = _showDeleteConfirmEvent

    private val _showRenameDialogEvent = MutableLiveData<Event<Long>>()
    val showRenameDialogEvent: LiveData<Event<Long>>
        get() = _showRenameDialogEvent

    private var listItems = mutableListOf<LabelListItem>()
        set(value) {
            field = value
            _labelItems.value = value

            // Update selected notes.
            val selectedBefore = selectedLabels.size
            selectedLabels.clear()
            selectedLabelIds.clear()
            for (item in value) {
                if (item.checked) {
                    selectedLabels += item.label
                    selectedLabelIds += item.label.id
                }
            }

            // Update placeholder visibility
            _placeholderShown.value = value.isEmpty()

            if (managingLabels) {
                _labelSelection.value = selectedLabels.size
            }
            if (selectedLabels.size != selectedBefore) {
                saveLabelSelectionState()
            }
        }

    /**
     * IDs of notes to set labels on.
     * Empty if managing labels
     */
    private var noteIds = emptyList<Long>()

    private val managingLabels: Boolean get() = noteIds.isEmpty()

    private val selectedLabelIds = mutableSetOf<Long>()
    private val selectedLabels = mutableSetOf<Label>()

    private var renamingLabel = false

    private var labelsListJob: Job? = null
    private var restoreStateJob: Job? = null

    init {
        restoreStateJob = viewModelScope.launch {
            // Restore state
            noteIds = savedStateHandle.get<List<Long>>(KEY_NOTE_IDS).orEmpty()
            selectedLabelIds += savedStateHandle.get<List<Long>>(KEY_SELECTED_IDS).orEmpty()
            selectedLabels += selectedLabelIds.mapNotNull { labelsRepository.getLabelById(it) }
            renamingLabel = savedStateHandle[KEY_RENAMING_LABEL] ?: false
            restoreStateJob = null
        }
    }

    /**
     * Initializes view model to set labels on notes by ID.
     * If ID list is empty, view model will be set up to manage labels instead.
     */
    fun start(ids: List<Long>) {
        labelsListJob?.cancel()
        labelsListJob = viewModelScope.launch {
            if (noteIds.isEmpty() && ids.isNotEmpty()) {
                // First view model start.
                // Initially, set selected notes to the subset of labels shared by all notes.
                noteIds = ids
                selectedLabelIds.clear()
                selectedLabelIds += labelsRepository.getLabelIdsForNote(noteIds.first())
                for (noteId in noteIds.listIterator(1)) {
                    selectedLabelIds.retainAll(labelsRepository.getLabelIdsForNote(noteId).toSet())
                }

                savedStateHandle[KEY_NOTE_IDS] = noteIds
                saveLabelSelectionState()
            }

            // Initialize label list
            labelsRepository.getAllLabelsByUsage().collect { labels ->
                // Since state restoration is suspending, ensure that state is properly restored
                // (e.g. selection) before setting list items, or selection may be lost.
                restoreStateJob?.join()

                if (renamingLabel) {
                    // List was updated after renaming label, this can only be due to label rename.
                    // Deselect label since it was probably selected only for renaming.
                    renamingLabel = false
                    selectedLabelIds.clear()
                    selectedLabels.clear()
                }
                listItems = labels.mapTo(mutableListOf()) { label ->
                    LabelListItem(label.id, label, label.id in selectedLabelIds)
                }
            }
        }
    }

    fun setNotesLabels() {
        viewModelScope.launch {
            for (noteId in noteIds) {
                // Find difference between old labels and new labels
                val labelsToRemove = labelsRepository.getLabelIdsForNote(noteId).toMutableSet()
                val labelsToAdd = selectedLabelIds.toMutableSet()
                val unchangedLabels = labelsToAdd intersect labelsToRemove
                labelsToRemove.removeAll(unchangedLabels)
                labelsToAdd.removeAll(unchangedLabels)
                labelsRepository.deleteLabelRefs(labelsToRemove.map { LabelRef(noteId, it) })
                labelsRepository.insertLabelRefs(labelsToAdd.map { LabelRef(noteId, it) })
            }
        }
    }

    private fun clearSelection() {
        setAllSelected(false)
    }

    fun selectAll() {
        setAllSelected(true)
    }

    fun renameSelection() {
        if (selectedLabels.size != 1) {
            // Renaming multiple or no labels, abort.
            return
        }
        renamingLabel = true
        _showRenameDialogEvent.send(selectedLabelIds.first())
    }

    fun deleteSelectionPre() {
        viewModelScope.launch {
            var used = false
            for (label in selectedLabels) {
                if (labelsRepository.countLabelRefs(label.id) > 0) {
                    used = true
                    break
                }
            }

            if (used) {
                _showDeleteConfirmEvent.send()
            } else {
                // None of the labels are used, delete without confirmation.
                deleteSelection()
            }
        }
    }

    private fun deleteSelection() {
        // Delete labels (called after confirmation)
        viewModelScope.launch {
            labelsRepository.deleteLabels(selectedLabels.toList())
            clearSelection()
        }
    }

    /** Set the selected state of all notes to [selected]. */
    private fun setAllSelected(selected: Boolean) {
        val allSelected = selected && selectedLabels.size == listItems.size
        val allUnselected = !selected && selectedLabels.isEmpty()
        if (allSelected || allUnselected) {
            // No changes needed.
            return
        }

        changeListItems { items ->
            for ((i, item) in items.withIndex()) {
                if (item.checked != selected) {
                    items[i] = item.copy(checked = selected)
                }
            }
        }
    }

    fun selectNewLabel(label: Label) {
        if (!managingLabels) {
            // If selecting labels and a new label was just added select it automatically.
            // The user most likely added the label with the intention of using it.
            val itemPos = listItems.indexOfFirst { it.id == label.id }
            if (itemPos != -1) {
                // Note that when this is called the list may or may not have been updated with the new label.
                toggleItemChecked(itemPos)
            } else {
                // List wasn't updated yet, select it then.
                selectedLabelIds += label.id
            }
        }
    }

    override val shouldHighlightCheckedItems: Boolean
        // When managing labels, items are highlighted if checked.
        // When selecting labels for a note, only the left icon is changed.
        get() = managingLabels

    override fun onLabelItemClicked(item: LabelListItem, pos: Int) {
        if (!managingLabels || selectedLabels.isNotEmpty()) {
            toggleItemChecked(pos)
        } else {
            _showRenameDialogEvent.send(item.label.id)
        }
    }

    override fun onLabelItemIconClicked(item: LabelListItem, pos: Int) {
        toggleItemChecked(pos)
    }

    override fun editItemButtonClicked(item: LabelListItem) {

        //update label
        viewModelScope.launch(Dispatchers.IO) {
            labelsRepository.updateLabel(Label(item.id,item.label.name,item.label.hidden))
        }
    }

    override fun deleteItemButtonClicked(item: LabelListItem) {

        //delete label item
        viewModelScope.launch(Dispatchers.IO) {
            labelsRepository.deleteLabel(Label(item.id,item.label.name,item.label.hidden))
        }
    }

    private fun toggleItemChecked(pos: Int) {
        // Set the item as checked and update the list.
        changeListItems { items ->
            items[pos] = items[pos].copy(checked = !items[pos].checked)
        }
    }

    private inline fun changeListItems(change: (MutableList<LabelListItem>) -> Unit) {
        val newList = listItems.toMutableList()
        change(newList)
        listItems = newList
    }

    /** Save [selectedLabels] to [savedStateHandle]. */
    private fun saveLabelSelectionState() {
        savedStateHandle[KEY_SELECTED_IDS] = selectedLabelIds.toList()
    }

    @AssistedFactory
    interface Factory : AssistedSavedStateViewModelFactory<LabelViewModel> {
        override fun create(savedStateHandle: SavedStateHandle): LabelViewModel
    }

    companion object {
        private const val KEY_NOTE_IDS = "note_ids"
        private const val KEY_SELECTED_IDS = "selected_ids"
        private const val KEY_RENAMING_LABEL = "renaming_label"
    }
}
