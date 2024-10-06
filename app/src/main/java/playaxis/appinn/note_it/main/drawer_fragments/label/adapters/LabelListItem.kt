package playaxis.appinn.note_it.main.drawer_fragments.label.adapters

import playaxis.appinn.note_it.repository.model.entities.Label

data class LabelListItem(
    val id: Long,
    val label: Label,
    var checked: Boolean
)
