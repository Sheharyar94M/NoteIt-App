package playaxis.appinn.note_it.main.fragment_home.fragments.folder.viewmodels

import androidx.lifecycle.SavedStateHandle
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import playaxis.appinn.note_it.main.viewModels.AssistedSavedStateViewModelFactory
import playaxis.appinn.note_it.repository.folder.FoldersRepository

class FolderViewModel @AssistedInject constructor(
    foldersRepository: FoldersRepository,
    @Assisted savedStateHandle: SavedStateHandle
) : AbstractFolderViewModel(foldersRepository,savedStateHandle){

    @AssistedFactory
    interface Factory : AssistedSavedStateViewModelFactory<FolderViewModel> {
        override fun create(savedStateHandle: SavedStateHandle): FolderViewModel
    }
}