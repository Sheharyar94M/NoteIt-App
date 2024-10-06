package playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.todo.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import playaxis.appinn.note_it.databinding.ItemEditContentBinding
import playaxis.appinn.note_it.databinding.ItemEditDateBinding
import playaxis.appinn.note_it.databinding.ItemEditHeaderBinding
import playaxis.appinn.note_it.databinding.ItemEditItemAddBinding
import playaxis.appinn.note_it.databinding.ItemEditItemBinding
import playaxis.appinn.note_it.databinding.ItemEditLabelsBinding
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.note.viewmodels.AddEditNoteViewModel
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.todo.utils.EditChipsItem
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.todo.utils.EditContentItem
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.todo.utils.EditDateItem
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.todo.utils.EditItemItem
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.todo.utils.EditListItem

class TodoListAdapter(val context: Context, val callback: Callback) :
    ListAdapter<EditListItem, RecyclerView.ViewHolder>(EditDiffCallback()) {

    private var recyclerView: RecyclerView? = null
    private var pendingFocusChange: AddEditNoteViewModel.FocusChange? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            ViewType.DATE.ordinal -> EditDateViewHolder(ItemEditDateBinding.inflate(inflater, parent, false))
            ViewType.CONTENT.ordinal -> EditContentViewHolder(ItemEditContentBinding.inflate(inflater, parent, false), callback)
            ViewType.ITEM_ADD.ordinal -> EditItemAddViewHolder(ItemEditItemAddBinding.inflate(inflater, parent, false), callback)
            ViewType.ITEM_CHECKED_HEADER.ordinal -> EditCheckedHeaderViewHolder(
                ItemEditHeaderBinding.inflate(inflater, parent, false))
            ViewType.ITEM_UNCHECKED_HEADER.ordinal -> EditUnCheckedHeaderViewHolder(ItemEditHeaderBinding.inflate(inflater, parent, false))
            ViewType.ITEM_CHIPS.ordinal -> EditItemLabelsViewHolder(ItemEditLabelsBinding.inflate(inflater, parent, false), callback)
            ViewType.ITEM.ordinal -> EditItemViewHolder(ItemEditItemBinding.inflate(inflater, parent, false), callback)
            else -> error("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        val item = getItem(position)
        when (holder) {
            is EditDateViewHolder -> holder.bind(item as EditDateItem)
            is EditContentViewHolder -> holder.bind(item as EditContentItem)
            is EditItemViewHolder -> holder.bind(item as EditItemItem)
            is EditCheckedHeaderViewHolder -> holder.bind()
            is EditUnCheckedHeaderViewHolder -> holder.bind()
            is EditItemLabelsViewHolder -> holder.bind(item as EditChipsItem)
        }
        if (holder is EditFocusableViewHolder && position == pendingFocusChange?.itemPos) {
            // Apply pending focus change event.
            holder.setFocus(pendingFocusChange!!.pos)
            pendingFocusChange = null
        }
    }

    override fun getItemViewType(position: Int) = getItem(position).type.ordinal

    fun setItemFocus(focus: AddEditNoteViewModel.FocusChange) {
        val rcv = recyclerView ?: return

        // If item to focus on doesn't exist yet, save it for later.
        if (!focus.itemExists) {
            pendingFocusChange = focus
            return
        }

        val viewHolder = rcv.findViewHolderForAdapterPosition(focus.itemPos)
        if (viewHolder is EditFocusableViewHolder) {
            viewHolder.setFocus(focus.pos)
        } else {
            // No item view holder for that position.
            // Not supposed to happen, but if it does, just save it for later.
            pendingFocusChange = focus
        }
    }

    enum class ViewType {
        DATE,
        CONTENT,
        ITEM,
        ITEM_ADD,
        ITEM_CHECKED_HEADER,
        ITEM_UNCHECKED_HEADER,
        ITEM_CHIPS
    }

    interface Callback {
        /**
         * Called when an [EditItemItem] at [pos] text is changed by user,
         * either from the keyboard or from a paste event.
         */
        fun onNoteItemChanged(pos: Int, isPaste: Boolean)
        /** Called when an [EditItemItem] at [pos] is checked or unchecked by user. */
        fun onNoteItemCheckChanged(pos: Int, checked: Boolean)
        /**
         * Called when backspace is pressed when EditText selection
         * is a position 0 in an [EditItemItem] at [pos].
         */
        fun onNoteItemBackspacePressed(pos: Int)
        /** Called when the delete button is clicked on an [EditItemItem].*/
        fun onNoteItemDeleteClicked(pos: Int)
        /** Called when [EditItemAddItem] is clicked.*/
        fun onNoteItemAddClicked(pos: Int)
        /** Called when a chip in [EditChipsItem] is clicked. */
        fun onNoteLabelClicked()
        fun onNoteReminderClicked()
        /** Called when any item is clicked on to start editing.*/
        fun onNoteClickedToEdit()
        /** Called when a link with an [url] is clicked in the note text.*/
        fun onLinkClickedInNote(linkText: String, linkUrl: String)
        /** Whether to enabled the dragging of [EditItemItem].*/
        val isNoteDragEnabled: Boolean
        /** Called after an [EditItemItem] was dragged [from] a position [to] another. */
        fun onNoteItemSwapped(from: Int, to: Int)
        /** Whether strikethrough should be added to checked items or not. */
        val strikethroughCheckedItems: Boolean
        /** Whether checked items are moved to the bottom or not. */
        val moveCheckedToBottom: Boolean
    }
}