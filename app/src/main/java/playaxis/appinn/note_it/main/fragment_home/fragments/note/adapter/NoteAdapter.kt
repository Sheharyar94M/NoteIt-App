package playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import playaxis.appinn.note_it.R
import playaxis.appinn.note_it.databinding.ItemHeaderBinding
import playaxis.appinn.note_it.databinding.ItemMessageBinding
import playaxis.appinn.note_it.databinding.ItemNoteLabelBinding
import playaxis.appinn.note_it.databinding.ItemNoteListBinding
import playaxis.appinn.note_it.databinding.ItemNoteListItemBinding
import playaxis.appinn.note_it.databinding.ItemNoteTextBinding
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.HeaderItem
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.HeaderViewHolder
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.LabelChipViewHolder
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.ListNoteItemViewHolder
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.ListNoteViewHolder
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.MessageItem
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.MessageViewHolder
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.NoteItem
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.NoteItemList
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.NoteItemText
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.NoteListDiffCallback
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.NoteListItem
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.NoteViewHolder
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.SwipeAction
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.SwipeTouchHelperCallback
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.TextNoteViewHolder
import playaxis.appinn.note_it.preferences.PrefsManager

class NoteAdapter(
    val context: Context,
    val callback: Callback,
    val prefsManager: PrefsManager
) : ListAdapter<NoteListItem, RecyclerView.ViewHolder>(NoteListDiffCallback()) {

    private val listNoteItemViewHolderPool = ArrayDeque<ListNoteItemViewHolder>()

    private val labelViewHolderPool = ArrayDeque<LabelChipViewHolder>()

    private val itemTouchHelper = ItemTouchHelper(SwipeTouchHelperCallback(callback))

    // Used by view holders with highlighted text.
    val highlightBackgroundColor = ContextCompat.getColor(context, R.color.color_highlight)
    val highlightForegroundColor = ContextCompat.getColor(context, R.color.theme_color)

    init {
        setHasStableIds(true)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            ViewType.MESSAGE.ordinal -> MessageViewHolder(ItemMessageBinding.inflate(inflater, parent, false))
            ViewType.HEADER.ordinal -> HeaderViewHolder(ItemHeaderBinding.inflate(inflater, parent, false))
            ViewType.TEXT_NOTE.ordinal -> TextNoteViewHolder(ItemNoteTextBinding.inflate(inflater, parent, false))
            ViewType.LIST_NOTE.ordinal -> ListNoteViewHolder(ItemNoteListBinding.inflate(inflater, parent, false))
            else -> error("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is MessageViewHolder -> holder.bind(item as MessageItem, this)
            is HeaderViewHolder -> holder.bind(item as HeaderItem)
            is TextNoteViewHolder -> {
                // [onViewRecycled] is not always called so unbinding is also done here.
                holder.unbind(this)
                holder.bind(this, item as NoteItemText)
            }
            is ListNoteViewHolder -> {
                // [onViewRecycled] is not always called so unbinding is also done here.
                holder.unbind(this)
                holder.bind(this, item as NoteItemList)
            }
        }
    }

    override fun getItemViewType(position: Int) = getItem(position).type.ordinal

    override fun getItemId(position: Int) = getItem(position).id

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {

        // Used to recycle secondary view holders
        if (holder is NoteViewHolder<*>) {
            holder.unbind(this)
        }
    }

    @SuppressLint("InflateParams")
    fun obtainListNoteItemViewHolder(): ListNoteItemViewHolder =
        if (listNoteItemViewHolderPool.isNotEmpty()) {
            listNoteItemViewHolderPool.removeLast()
        }
        else {
            ListNoteItemViewHolder(
                ItemNoteListItemBinding.inflate(
                LayoutInflater.from(context), null, false))
        }

    @SuppressLint("InflateParams")
    fun obtainLabelViewHolder(): LabelChipViewHolder =
        if (labelViewHolderPool.isNotEmpty()) {
            labelViewHolderPool.removeLast()
        } else {
            LabelChipViewHolder(
                ItemNoteLabelBinding.inflate(
                LayoutInflater.from(context), null, false))
        }

    fun freeListNoteItemViewHolder(viewHolder: ListNoteItemViewHolder) {
        listNoteItemViewHolderPool += viewHolder
    }

    fun freeLabelViewHolder(viewHolder: LabelChipViewHolder) {
        labelViewHolderPool += viewHolder
    }


    @SuppressLint("NotifyDataSetChanged")
    fun updateForListLayoutChange() {
        // Number of preview lines have changed, must rebind all items
        notifyItemRangeChanged(0, itemCount)
        notifyDataSetChanged()
    }

    enum class ViewType {
        MESSAGE,
        HEADER,
        TEXT_NOTE,
        LIST_NOTE
    }

    enum class SwipeDirection {
        LEFT,
        RIGHT
    }

    interface Callback {
        /** Called when a note [item] at [pos] is clicked. */
        fun onNoteItemClicked(item: NoteItem, pos: Int)

        /** Called when a note [item] at [pos] is long-clicked. */
        fun onNoteItemLongClicked(item: NoteItem, pos: Int)

        /** Called when a message [item] at [pos] is dismissed by clicking on close button. */
        fun onMessageItemDismissed(item: MessageItem, pos: Int)

        /** Called when a note's action button is clicked. */
        fun onNoteActionButtonClicked(item: NoteItem, pos: Int)

        /** Returns the action for the given swipe direction. */
        fun getNoteSwipeAction(direction: SwipeDirection): SwipeAction

        /** Called when a [NoteItem] at [pos] is swiped. */
        fun onNoteSwiped(pos: Int, direction: NoteAdapter.SwipeDirection)

        /** Whether strikethrough should be added to checked items or not. */
        val strikethroughCheckedItems: Boolean
    }
}
