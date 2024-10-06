package playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.todo.utils

import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.todo.adapter.TodoListAdapter.ViewType

sealed class EditListItem {
    abstract val type: ViewType
}

data class EditDateItem(
    val date: Long
) : EditListItem() {
    override val type get() = ViewType.DATE
}

//data class EditTitleItem(
//    var title: EditableText,
//    val editable: Boolean
//) : EditListItem() {
//    override val type get() = ViewType.TITLE
//}

data class EditContentItem(
    var content: EditableText,
    val editable: Boolean
) : EditListItem() {
    override val type get() = ViewType.CONTENT
}

data class EditItemItem(
    var content: EditableText,
    var checked: Boolean,
    val editable: Boolean,
    var actualPos: Int,
) : EditListItem() {
    override val type get() = ViewType.ITEM
}

data object EditItemAddItem : EditListItem() {
    override val type get() = ViewType.ITEM_ADD
}

data class EditCheckedHeaderItem(
    var count: Int
) : EditListItem() {
    override val type get() = ViewType.ITEM_CHECKED_HEADER
}

data class EditUncheckedHeaderItem(
    var count: Int
) : EditListItem() {
    override val type get() = ViewType.ITEM_UNCHECKED_HEADER
}

data class EditChipsItem(
    // Chips can be Label or Reminder
    val chips: List<Any>
) : EditListItem() {
    override val type get() = ViewType.ITEM_CHIPS
}

interface EditableText {
    val text: CharSequence
    fun append(text: CharSequence)
    fun replaceAll(text: CharSequence)
}