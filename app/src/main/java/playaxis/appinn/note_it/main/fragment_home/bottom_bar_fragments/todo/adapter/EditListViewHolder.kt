package playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.todo.adapter

import android.text.Editable
import android.text.format.DateUtils
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isInvisible
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import playaxis.appinn.note_it.R
import playaxis.appinn.note_it.databinding.ItemEditContentBinding
import playaxis.appinn.note_it.databinding.ItemEditDateBinding
import playaxis.appinn.note_it.databinding.ItemEditHeaderBinding
import playaxis.appinn.note_it.databinding.ItemEditItemAddBinding
import playaxis.appinn.note_it.databinding.ItemEditItemBinding
import playaxis.appinn.note_it.databinding.ItemEditLabelsBinding
import playaxis.appinn.note_it.extensions.hideKeyboard
import playaxis.appinn.note_it.extensions.showKeyboard
import playaxis.appinn.note_it.extensions.strikethroughText
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.todo.utils.EditChipsItem
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.todo.utils.EditContentItem
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.todo.utils.EditDateItem
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.todo.utils.EditItemItem
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.todo.utils.EditableText
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.todo.utils.RelativeDateFormatter
import playaxis.appinn.note_it.preferences.PrefsManager
import playaxis.appinn.note_it.repository.model.entities.Label
import playaxis.appinn.note_it.repository.model.entities.Reminder
import java.text.DateFormat

sealed interface EditFocusableViewHolder {
    fun setFocus(pos: Int)
}

class EditDateViewHolder(binding: ItemEditDateBinding) : RecyclerView.ViewHolder(binding.root) {

    private val dateEdt = binding.dateEdt

    private val dateFormatter = RelativeDateFormatter(dateEdt.resources) { date ->
        DateUtils.formatDateTime(
            dateEdt.context, date, DateUtils.FORMAT_SHOW_DATE or
                    DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_ALL
        )
    }

    fun bind(item: EditDateItem) {
        dateEdt.text = dateFormatter.format(
            item.date, System.currentTimeMillis(),
            PrefsManager.MAXIMUM_RELATIVE_DATE_DAYS
        )
    }
}

class EditContentViewHolder(binding: ItemEditContentBinding, callback: TodoListAdapter.Callback) : RecyclerView.ViewHolder(binding.root), EditFocusableViewHolder {

    private val contentEdt = binding.contentEdt
    private var item: EditContentItem? = null

    init {
        contentEdt.addTextChangedListener(BulletTextWatcher())
        contentEdt.doAfterTextChanged { editable ->
            if (editable != null && editable != item?.content?.text) {
                item?.content = AndroidEditableText(editable)
            }
        }

        contentEdt.setOnClickListener {
            callback.onNoteClickedToEdit()
        }
    }

    fun bind(item: EditContentItem) {
        this.item = item
        contentEdt.isFocusable = item.editable
        contentEdt.isFocusableInTouchMode = item.editable
        contentEdt.setText(item.content.text)
    }

    override fun setFocus(pos: Int) {
        contentEdt.requestFocus()
        contentEdt.setSelection(pos)
        contentEdt.showKeyboard()
    }
}

class EditItemViewHolder(binding: ItemEditItemBinding, callback: TodoListAdapter.Callback) : RecyclerView.ViewHolder(binding.root), EditFocusableViewHolder {

    private val itemCheck = binding.itemChk
    private val itemEdt = binding.contentEdt
    private val deleteImv = binding.deleteImv
    private var item: EditItemItem? = null

    init {
        itemCheck.setOnCheckedChangeListener { _, isChecked ->
            itemEdt.clearFocus()
            itemEdt.hideKeyboard()
            itemEdt.strikethroughText = isChecked && callback.strikethroughCheckedItems
            itemEdt.isActivated = !isChecked // Controls text color selector.

            val pos = bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION)
                callback.onNoteItemCheckChanged(pos, isChecked)
        }
        itemEdt.doOnTextChanged { _, _, _, count ->
            if (itemEdt.text != item?.content?.text) {
                item?.content = AndroidEditableText(itemEdt.text!!)
            }

            // This is used to detect when user enters line breaks into the input, so the
            // item can be split into multiple items. When user enters a single line break,
            // selection is set at the beginning of new item. On paste, i.e. when more than one
            // character is entered, selection is set at the end of last new item.
            val pos = bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION)
                callback.onNoteItemChanged(pos, count > 1)
        }
        itemEdt.setOnFocusChangeListener { _, hasFocus ->
            // Only show delete icon for currently focused item.
            deleteImv.isInvisible = !hasFocus
        }
        itemEdt.setOnKeyListener { _, _, event ->
            val isCursorAtStart = itemEdt.selectionStart == 0 && itemEdt.selectionStart == itemEdt.selectionEnd
            if (isCursorAtStart && event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_DEL) {
                // If user presses backspace at the start of an item, current item
                // will be merged with previous.
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    callback.onNoteItemBackspacePressed(pos)
                }
            }
            false
        }
        itemEdt.setOnClickListener {
            callback.onNoteClickedToEdit()
        }
        deleteImv.setOnClickListener {
            val pos = bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                callback.onNoteItemDeleteClicked(pos)
            }
        }
    }

    fun bind(item: EditItemItem) {
        this.item = item

        itemEdt.isFocusable = item.editable
        itemEdt.isFocusableInTouchMode = item.editable
        itemEdt.setText(item.content.text)
        itemEdt.isActivated = !item.checked
        itemCheck.isChecked = item.checked
        itemCheck.isEnabled = true
    }

    override fun setFocus(pos: Int) {
        itemEdt.requestFocus()
        itemEdt.setSelection(pos)
        itemEdt.showKeyboard()
    }

    fun clearFocus() {
        itemEdt.clearFocus()
    }
}

class EditItemAddViewHolder(binding: ItemEditItemAddBinding, callback: TodoListAdapter.Callback) : RecyclerView.ViewHolder(binding.root) {

    init {
        itemView.setOnClickListener {
            callback.onNoteItemAddClicked(bindingAdapterPosition)
        }
    }
}

class EditCheckedHeaderViewHolder(binding: ItemEditHeaderBinding) : RecyclerView.ViewHolder(binding.root) {

    private val titleTxv = binding.titleTxv

    fun bind() {
        titleTxv.text = "Done List"
    }
}

class EditUnCheckedHeaderViewHolder(binding: ItemEditHeaderBinding) : RecyclerView.ViewHolder(binding.root) {

    private val titleTxv = binding.titleTxv

    fun bind() {
        titleTxv.text = "Todo List"
    }
}

class EditItemLabelsViewHolder(binding: ItemEditLabelsBinding, callback: TodoListAdapter.Callback) :
    RecyclerView.ViewHolder(binding.root) {

    private val chipGroup = binding.chipGroup
    private val labelClickListener = View.OnClickListener {
        callback.onNoteLabelClicked()
    }
    private val reminderClickListener = View.OnClickListener {
        callback.onNoteReminderClicked()
    }

    private val reminderDateFormatter = RelativeDateFormatter(itemView.resources) { date ->
        DateFormat.getDateInstance(DateFormat.SHORT).format(date)
    }

    fun bind(item: EditChipsItem) {
        val layoutInflater = LayoutInflater.from(chipGroup.context)
        chipGroup.removeAllViews()
        for (chip in item.chips) {
            when (chip) {
                is Label -> {
                    val view = layoutInflater.inflate(
                        R.layout.view_edit_chip_label,
                        chipGroup,
                        false
                    ) as Chip
                    chipGroup.addView(view)
                    view.text = chip.name
                    view.setOnClickListener(labelClickListener)
                }

                is Reminder -> {
                    val view = layoutInflater.inflate(
                        R.layout.view_edit_chip_reminder,
                        chipGroup,
                        false
                    ) as Chip
                    chipGroup.addView(view)
                    view.text = reminderDateFormatter.format(
                        chip.next.time,
                        System.currentTimeMillis(), PrefsManager.MAXIMUM_RELATIVE_DATE_DAYS
                    )
                    view.strikethroughText = chip.done
                    view.isActivated = !chip.done
                    view.setChipIconResource(if (chip.recurrence != null) R.drawable.ic_repeat else R.drawable.reminder_icon_)
                    view.setOnClickListener(reminderClickListener)
                }

                else -> error("Unknown chip type")
            }
        }
    }
}

// Wrapper around Editable to allow transparent access to text content from ViewModel.
// Editable items have a EditableText field which is set by a text watcher added to the
// EditText and called when text is set when item is bound.
// Note that the Editable instance can change during the EditText lifetime.
private class AndroidEditableText(override val text: Editable) : EditableText {

    override fun append(text: CharSequence) {
        this.text.append(text)
    }

    override fun replaceAll(text: CharSequence) {
        this.text.replace(0, this.text.length, text)
    }
}
