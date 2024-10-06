package playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.NoteAdapter.ViewType
import playaxis.appinn.note_it.repository.model.entities.Label
import playaxis.appinn.note_it.repository.model.entities.Note

sealed interface NoteListItem {
    val id: Long
    val type: ViewType
}

sealed interface NoteItem : NoteListItem {
    val note: Note
    val labels: List<Label>
    var checked: Boolean
    val title: Highlighted
    val showMarkAsDone: Boolean

    fun withChecked(checked: Boolean): NoteItem
}

data class NoteItemText(
    override val id: Long,
    override val note: Note,
    override val labels: List<Label>,
    override var checked: Boolean,
    override val title: Highlighted,
    val content: Highlighted,
    override val showMarkAsDone: Boolean,
) : NoteItem {

    override val type: ViewType
        get() = ViewType.TEXT_NOTE

    override fun withChecked(checked: Boolean) = copy(checked = checked)
}

data class NoteItemList(
    override val id: Long,
    override val note: Note,
    override val labels: List<Label>,
    override var checked: Boolean,
    override val title: Highlighted,
    val items: List<Highlighted>,
    val itemsChecked: List<Boolean>,
    val overflowCount: Int,
    val onlyCheckedInOverflow: Boolean,
    override val showMarkAsDone: Boolean,
) : NoteItem {

    override val type: ViewType
        get() = ViewType.LIST_NOTE

    override fun withChecked(checked: Boolean) = copy(checked = checked)
}

data class HeaderItem(
    override val id: Long,
    val title: Int,
) : NoteListItem {

    override val type get() = ViewType.HEADER
}

data class MessageItem(
    override val id: Long,
    @StringRes @PluralsRes val message: Int,
    val args: List<Any> = emptyList(),
) : NoteListItem {

    override val type get() = ViewType.MESSAGE
}