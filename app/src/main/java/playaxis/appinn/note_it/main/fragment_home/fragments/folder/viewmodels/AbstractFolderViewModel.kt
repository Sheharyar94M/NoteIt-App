package playaxis.appinn.note_it.main.fragment_home.fragments.folder.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import playaxis.appinn.note_it.repository.folder.FoldersRepository
import playaxis.appinn.note_it.repository.model.entities.FolderNote
import playaxis.appinn.note_it.repository.model.entities.FolderWithNotes

abstract class AbstractFolderViewModel(
    private var foldersRepository: FoldersRepository,
    private var savedStateHandle: SavedStateHandle
)  : ViewModel() {

    private var foldersMutableLiveData: MutableLiveData<List<FolderWithNotes>> = MutableLiveData()
    var foldersLiveData: LiveData<List<FolderWithNotes>> = foldersMutableLiveData
    init {
        setFoldersData()
    }

    fun setFoldersData(){
        //getting the data from database
        viewModelScope.launch(Dispatchers.IO) {

            foldersMutableLiveData.postValue(foldersRepository.getAllFolders())
        }
    }

    suspend fun createFolder(folderNote: FolderNote){
        foldersRepository.insertFolder(folderNote)
    }
}