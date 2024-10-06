package playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils

import android.annotation.SuppressLint
import android.text.SpannableString
import android.text.format.DateUtils
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.core.text.set
import androidx.core.view.ViewCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import playaxis.appinn.note_it.R
import playaxis.appinn.note_it.application.QuickNotepad
import playaxis.appinn.note_it.databinding.ItemHeaderBinding
import playaxis.appinn.note_it.databinding.ItemMessageBinding
import playaxis.appinn.note_it.databinding.ItemNoteLabelBinding
import playaxis.appinn.note_it.databinding.ItemNoteListBinding
import playaxis.appinn.note_it.databinding.ItemNoteListItemBinding
import playaxis.appinn.note_it.databinding.ItemNoteTextBinding
import playaxis.appinn.note_it.extensions.strikethroughText
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.NoteAdapter
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.NoteItemsImageListAdapter
import playaxis.appinn.note_it.preferences.PrefsManager
import playaxis.appinn.note_it.repository.model.entities.Label
import playaxis.appinn.note_it.repository.model.entities.Note
import playaxis.appinn.note_it.utils.MainUtils
import java.text.DateFormat

sealed class NoteViewHolder<T : NoteItem>(itemView: View): RecyclerView.ViewHolder(itemView){

    private val dateFormatter = RelativeDateFormatter(itemView.resources) { date ->
        DateUtils.formatDateTime(
            itemView.context,
            date,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_ALL
        )
    }
    private val reminderDateFormatter = RelativeDateFormatter(itemView.resources) { date ->
        DateFormat.getDateInstance(DateFormat.SHORT).format(date)
    }

    abstract val cardView: MaterialCardView
    abstract val swipeImv: AppCompatImageView
    protected abstract val titleTxv: TextView
    protected abstract val dateTxv: TextView
    protected abstract val reminderChip: Chip
    protected abstract val labelGroup: ChipGroup
    protected abstract val actionBtn: MaterialButton
    protected abstract val lockImage: AppCompatImageView
    protected abstract val imagesNote: RecyclerView
    protected abstract val mainLayout: LinearLayoutCompat
    protected abstract val background: AppCompatImageView
    protected abstract var adapterImages: NoteItemsImageListAdapter
    private val labelViewHolders = mutableListOf<LabelChipViewHolder>()
    private lateinit var adapter: NoteAdapter
    private lateinit var item: T

    open fun bind(adapter: NoteAdapter, item: T) {
        bindTitle(adapter, item)
        bindDate(adapter, item)
        bindReminder(item)
        bindLabels(adapter, item)
        bindActionBtn(adapter, item)

        adapterImages = NoteItemsImageListAdapter(adapter,item,bindingAdapterPosition)
        //making adapter global to class
        this.adapter = adapter
        this.item = item

        // Set transition names for shared transitions
        val noteId = item.note.id
        ViewCompat.setTransitionName(cardView, "noteContainer$noteId")

        //checking & un-checking items
        if (item.checked)
            cardView.strokeColor = ContextCompat.getColor(QuickNotepad.appContext, R.color.wave_progress_color)
        else
            cardView.strokeColor = ContextCompat.getColor(QuickNotepad.appContext, R.color.dialog_text_color)

        //the color of the card
        if (item.note.colorNote != null) {
            background.visibility = View.VISIBLE
            if (item.note.colorNote!!.color.length in 1..9) {
                //this means there is color in the background of the note item
                background.setBackgroundColor(item.note.colorNote!!.color.toInt())
            }
            else {
                //this means there is image in the background of the note item
                background.setImageDrawable(MainUtils.stringToDrawable(item.note.colorNote!!.color))
            }
        }
        else
            background.visibility = View.GONE

        // Click listeners for cardView
        cardView.setOnClickListener {
            adapter.callback.onNoteItemClicked(item, bindingAdapterPosition)
        }
        cardView.setOnLongClickListener {
            adapter.callback.onNoteItemLongClicked(item, bindingAdapterPosition)
            true
        }

        //locking the note
        if (item.note.lock.isNotEmpty()) {
            lockImage.visibility = View.VISIBLE
            mainLayout.visibility = View.GONE
            //making recyclerview gone because note is locked
            imagesNote.visibility = View.GONE
        }
        else {
            lockImage.visibility = View.GONE
            mainLayout.visibility = View.VISIBLE
            //making recyclerview visible because note is locked
            imagesNote.visibility = View.VISIBLE

            // Set transition names for shared transitions
            val noteId = item.note.id
            ViewCompat.setTransitionName(cardView, "noteContainer$noteId")
        }
    }

    @SuppressLint("SetTextI18n")
    private fun bindTitle(adapter: NoteAdapter, item: NoteItem) {
        titleTxv.text = getHighlightedList(
            item.title,
            adapter.highlightBackgroundColor, adapter.highlightForegroundColor
        )

        //check first that the imagelist of note object is empty or not
        if (item.note.lock.isNotEmpty()){

            titleTxv.visibility = View.VISIBLE
            //setting the title if the note has images
            if (item.title.content.isNotEmpty())
                titleTxv.text = item.title.content
        }
        else{

            //setting the title if the note do not have images
            if (item.title.content.isNotEmpty()){

                titleTxv.visibility = View.VISIBLE
                titleTxv.text = item.title.content
            }
            else{
                titleTxv.visibility = View.GONE
            }
        }
    }

    private fun bindDate(adapter: NoteAdapter, item: NoteItem) {
        val note = item.note
        val dateField = adapter.prefsManager.shownDateField
        val date = when (dateField) {
            ShownDateField.ADDED -> note.addedDate.time
            ShownDateField.MODIFIED -> note.lastModifiedDate.time
            ShownDateField.NONE -> 0L
        }
        dateTxv.text = dateFormatter.format(
            date,
            System.currentTimeMillis(),
            PrefsManager.MAXIMUM_RELATIVE_DATE_DAYS
        )
        dateTxv.isGone = (dateField == ShownDateField.NONE)
    }

    private fun bindReminder(item: NoteItem) {
        val note = item.note
        reminderChip.isVisible = note.reminder != null

        if (note.reminder != null) {
            reminderChip.text = reminderDateFormatter.format(
                note.reminder.next.time,
                System.currentTimeMillis(), PrefsManager.MAXIMUM_RELATIVE_DATE_DAYS
            )
            reminderChip.strikethroughText = note.reminder.done
            reminderChip.isActivated = !note.reminder.done
            reminderChip.setChipIconResource(if (note.reminder.recurrence != null) R.drawable.ic_repeat else R.drawable.reminder_icon_)
        }
    }

    private fun bindLabels(adapter: NoteAdapter, item: NoteItem) {
        // Show labels in order up to the maximum, then show a +N chip at the end.
        if (item.labels.isNotEmpty()) {
            labelGroup.isVisible = item.labels.isNotEmpty()

            for (label in item.labels) {
                val viewHolder = adapter.obtainLabelViewHolder()
                labelGroup.addView(viewHolder.binding.root)
                labelViewHolders += viewHolder
                viewHolder.bind(label)
            }
        } else {
            // Don't show labels in preview
            labelGroup.isVisible = false
        }
    }

    private fun bindActionBtn(adapter: NoteAdapter, item: NoteItem) {

        if (item.showMarkAsDone && !item.checked) {
            actionBtn.isVisible = true
            actionBtn.setIconResource(R.drawable.tick_button_white)
            actionBtn.setText(R.string.action_mark_as_done)
            actionBtn.setOnClickListener {
                adapter.callback.onNoteActionButtonClicked(item, bindingAdapterPosition)
            }
        } else {
            actionBtn.isVisible = false
        }
    }

    /**
     * Unbind a previously bound view holder.
     * This is used to free "secondary" view holders.
     */
    open fun unbind(adapter: NoteAdapter) {
        // Free label view holders
        labelGroup.removeViews(0, labelGroup.childCount)
        for (viewHolder in labelViewHolders) {
            adapter.freeLabelViewHolder(viewHolder)
        }
        labelViewHolders.clear()
    }
}

class TextNoteViewHolder(binding: ItemNoteTextBinding): NoteViewHolder<NoteItemText>(binding.root){

    override val cardView = binding.cardView
    override val swipeImv = binding.swipeImv
    override val titleTxv = binding.titleTxv
    override val dateTxv = binding.dateTxv
    override val reminderChip = binding.reminderChip
    override val labelGroup = binding.labelGroup
    override val actionBtn = binding.actionBtn
    private val contentTxv = binding.contentTxv
    private val audioIdentifier = binding.audioNote
    override val imagesNote = binding.noteImageList
    override val lockImage = binding.lockImage
    override val mainLayout = binding.mainLayout
    override val background = binding.background
    override lateinit var adapterImages: NoteItemsImageListAdapter

    override fun bind(adapter: NoteAdapter, item: NoteItemText) {
        super.bind(adapter, item)

        if (item.content.highlights.isNotEmpty())
            contentTxv.text = getHighlightedText(
                item.content,
                item.note,
                adapter.highlightBackgroundColor,
                adapter.highlightForegroundColor
            )
        else {

            if (item.content.content.isNotEmpty()){
                contentTxv.visibility = View.VISIBLE

                if (item.note.noteTextFormat.spannable)
                    contentTxv.text = MainUtils.deserializeSpannableString(QuickNotepad.appContext,item.note)
                else
                    contentTxv.text = item.content.content
            }
            else
                contentTxv.visibility = View.GONE
        }

        //checking if the note is an audio note
        if (item.note.noteAudios.isNotEmpty())
            audioIdentifier.visibility = View.VISIBLE
        else
            audioIdentifier.visibility = View.GONE

        //setting the images added to the note
        Log.i("item_note_noteImage: ", item.note.noteImages.toString())

        if (item.note.noteImages.isNotEmpty() && item.note.lock.isEmpty()) {

            imagesNote.visibility = View.VISIBLE

            //recyclerview
            imagesNote.setHasFixedSize(true)
            if (item.note.noteImages.size == 1) {
                val lManager = object : StaggeredGridLayoutManager(1, VERTICAL) {
                    override fun canScrollVertically() = false
                    override fun canScrollHorizontally() = false
                }

                imagesNote.layoutManager = lManager
                imagesNote.adapter = adapterImages
                //setting the list
                if (item.note.noteImages.size > 6)
                    adapterImages.setImagesList(item.note.noteImages.subList(0, 5))
                else
                    adapterImages.setImagesList(item.note.noteImages)
            }
            else if (item.note.noteImages.size < 3) {

                val lManager = object : StaggeredGridLayoutManager(2, VERTICAL) {
                    override fun canScrollVertically() = false
                    override fun canScrollHorizontally() = false
                }

                imagesNote.layoutManager = lManager
                imagesNote.adapter = adapterImages
                //setting the list
                if (item.note.noteImages.size > 6)
                    adapterImages.setImagesList(item.note.noteImages.subList(0, 5))
                else
                    adapterImages.setImagesList(item.note.noteImages)
            }
            else {

                val lManager = object : StaggeredGridLayoutManager(3, VERTICAL) {
                    override fun canScrollVertically() = false
                    override fun canScrollHorizontally() = false
                }

                imagesNote.layoutManager = lManager
                imagesNote.adapter = adapterImages
                //setting the list
                if (item.note.noteImages.size > 6)
                    adapterImages.setImagesList(item.note.noteImages.subList(0, 5))
                else
                    adapterImages.setImagesList(item.note.noteImages)
            }
        }
        else
            imagesNote.visibility = View.GONE
    }
}

class ListNoteViewHolder(private val binding: ItemNoteListBinding): NoteViewHolder<NoteItemList>(binding.root){

    override val cardView = binding.cardView
    override val swipeImv = binding.swipeImv
    override val titleTxv = binding.titleListTxv
    override val dateTxv = binding.dateTxv
    override val reminderChip = binding.reminderChip
    override val labelGroup = binding.labelGroup
    override val actionBtn = binding.actionBtn
    override val lockImage = binding.lockImage
    override val mainLayout = binding.mainLayout
    override val background = binding.background
    override val imagesNote = binding.noteImageList
    override lateinit var adapterImages: NoteItemsImageListAdapter

    private val itemViewHolders = mutableListOf<ListNoteItemViewHolder>()

    override fun bind(adapter: NoteAdapter, item: NoteItemList) {
        super.bind(adapter, item)

        // Bind list note items
        val itemsLayout = binding.itemsLayout
        itemsLayout.isVisible = item.items.isNotEmpty()

        //Issue is here
        Log.i("itemsList: ", item.items.size.toString())

        for ((i, noteItem) in item.items.withIndex()) {
            val viewHolder = adapter.obtainListNoteItemViewHolder()
            viewHolder.bind(adapter, noteItem, item.itemsChecked[i])
            itemsLayout.addView(viewHolder.binding.root, itemViewHolders.size)
            itemViewHolders += viewHolder
        }

        // Show a label indicating the number of items not shown.
        val infoTxv = binding.infoTxv
        infoTxv.isVisible = item.overflowCount > 0
        if (item.overflowCount > 0) {
            infoTxv.text = adapter.context.resources.getQuantityString(
                if (item.onlyCheckedInOverflow) {
                    R.plurals.note_list_item_info_checked
                } else {
                    R.plurals.note_list_item_info
                }, item.overflowCount, item.overflowCount
            )
        }

        //setting the images added to the note
        Log.i("item_note_noteImage: ", item.note.noteImages.toString())

        if (item.note.noteImages.isNotEmpty() && item.note.lock.isEmpty()) {

            imagesNote.visibility = View.VISIBLE
            //recyclerview
            imagesNote.setHasFixedSize(true)
            if (item.note.noteImages.size == 1) {
                val lManager = object : StaggeredGridLayoutManager(1, VERTICAL) {
                    override fun canScrollVertically() = false
                    override fun canScrollHorizontally() = false
                }

                imagesNote.layoutManager = lManager
                imagesNote.adapter = adapterImages
                //setting the list
                if (item.note.noteImages.size > 6)
                    adapterImages.setImagesList(item.note.noteImages.subList(0, 5))
                else
                    adapterImages.setImagesList(item.note.noteImages)
            }
            else if (item.note.noteImages.size < 3) {

                val lManager = object : StaggeredGridLayoutManager(2, VERTICAL) {
                    override fun canScrollVertically() = false
                    override fun canScrollHorizontally() = false
                }

                imagesNote.layoutManager = lManager
                imagesNote.adapter = adapterImages
                //setting the list
                if (item.note.noteImages.size > 6)
                    adapterImages.setImagesList(item.note.noteImages.subList(0, 5))
                else
                    adapterImages.setImagesList(item.note.noteImages)
            }
            else {

                val lManager = object : StaggeredGridLayoutManager(3, VERTICAL) {
                    override fun canScrollVertically() = false
                    override fun canScrollHorizontally() = false
                }

                imagesNote.layoutManager = lManager
                imagesNote.adapter = adapterImages
                //setting the list
                if (item.note.noteImages.size > 6)
                    adapterImages.setImagesList(item.note.noteImages.subList(0, 5))
                else
                    adapterImages.setImagesList(item.note.noteImages)
            }
        }
        else
            imagesNote.visibility = View.GONE
    }

    override fun unbind(adapter: NoteAdapter) {
        super.unbind(adapter)
        // Free view holders used by the item.
        binding.itemsLayout.removeViews(0, binding.itemsLayout.childCount - 1)
        for (viewHolder in itemViewHolders) {
            adapter.freeListNoteItemViewHolder(viewHolder)
        }
        itemViewHolders.clear()
    }
}

class MessageViewHolder(private val binding: ItemMessageBinding): RecyclerView.ViewHolder(binding.root) {

    fun bind(item: MessageItem, adapter: NoteAdapter) {
        val firstArg = item.args.getOrNull(0)
        binding.messageTxv.text = if (firstArg is Int) {
            // Likely a plural
            adapter.context.resources.getQuantityString(
                item.message,
                firstArg,
                *item.args.toTypedArray()
            )
        } else {
            // A string, possibly with format arguments
            adapter.context.getString(item.message, *item.args.toTypedArray())
        }
        binding.closeImv.setOnClickListener {
            adapter.callback.onMessageItemDismissed(item, bindingAdapterPosition)
            adapter.notifyItemRemoved(bindingAdapterPosition)
        }

        (itemView.layoutParams as StaggeredGridLayoutManager.LayoutParams).isFullSpan = true
    }
}

class HeaderViewHolder(private val binding: ItemHeaderBinding): RecyclerView.ViewHolder(binding.root) {

    fun bind(item: HeaderItem) {
        binding.titleTxv.setText(item.title)
        (itemView.layoutParams as StaggeredGridLayoutManager.LayoutParams).isFullSpan = true
    }
}

class ListNoteItemViewHolder(val binding: ItemNoteListItemBinding) {

    fun bind(adapter: NoteAdapter, item: Highlighted, checked: Boolean) {
        binding.contentTxv.apply {
            text = getHighlightedList(
                item,
                adapter.highlightBackgroundColor,
                adapter.highlightForegroundColor
            )
            strikethroughText = checked && adapter.callback.strikethroughCheckedItems
        }

        binding.checkboxImv.setImageResource(
            if (checked)
                R.drawable.checkbox_selected
            else
                R.drawable.checkbox_off
        )
    }
}

class LabelChipViewHolder(val binding: ItemNoteLabelBinding) {

    fun bind(label: Label) {
        binding.labelChip.text = label.name
    }
}

fun getHighlightedList(text: Highlighted,bgColor: Int, fgColor: Int): CharSequence {
    if (text.highlights.isEmpty()) {
        return text.content
    }
    val highlightedText = SpannableString(text.content)
    for (highlight in text.highlights) {
        highlightedText[highlight] = BackgroundColorSpan(bgColor)
        highlightedText[highlight] = ForegroundColorSpan(fgColor)
    }
    return highlightedText
}

fun getHighlightedText(text: Highlighted, item: Note, bgColor: Int, fgColor: Int): CharSequence {
    if (text.highlights.isEmpty()) {
        return if (item.noteTextFormat.spannable)
            MainUtils.deserializeSpannableString(QuickNotepad.appContext,item)
        else
            text.content
    }
    val highlightedText = SpannableString(MainUtils.deserializeSpannableString(QuickNotepad.appContext,item))
    for (highlight in text.highlights) {
        highlightedText[highlight] = BackgroundColorSpan(bgColor)
        highlightedText[highlight] = ForegroundColorSpan(fgColor)
    }
    return highlightedText
}
