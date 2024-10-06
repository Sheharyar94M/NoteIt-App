package playaxis.appinn.note_it.main.fragment_home.fragments.folder.helper

import playaxis.appinn.note_it.repository.model.entities.FolderWithNotes

data class FolderSelectionItem(
    var folderWithNotes: FolderWithNotes,
    var isSelected: Boolean = false
)
