package playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.note.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import playaxis.appinn.note_it.R
import playaxis.appinn.note_it.application.QuickNotepad
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.draw.helper.ColorObserve
import playaxis.appinn.note_it.main.utils.Event
import playaxis.appinn.note_it.main.utils.observeEvent
import playaxis.appinn.note_it.repository.model.entities.ColorNote

class ColorListAdapter(
    private var colorsItemClick: ColorsItemSelectedEvent,
    private var owner: LifecycleOwner
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var listColors = ArrayList<ColorNote>()
    private var selectedItemPosition = 0
    val pen1SelectionEvent: MutableLiveData<Event<ColorObserve>> = MutableLiveData()
    val pen2SelectionEvent: MutableLiveData<Event<ColorObserve>> = MutableLiveData()
    val pen3SelectionEvent: MutableLiveData<Event<ColorObserve>> = MutableLiveData()

    private val VIEW_TYPE_FIRST_ITEM = 0
    private val VIEW_TYPE_REGULAR_ITEM = 1

    fun setColors(listColors: ArrayList<ColorNote>) {
        this.listColors = listColors
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        val inflater = LayoutInflater.from(parent.context)

        return if (viewType == VIEW_TYPE_FIRST_ITEM) {
            val view: View = inflater.inflate(R.layout.color_list_item_view, parent, false)
            FirstItemViewHolder(view)
        }
        else{
            val view: View = inflater.inflate(R.layout.draw_color_selection_list, parent, false)
            RegularItemViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        val item_position = position

        if (holder is FirstItemViewHolder) {
            // Bind data for the first item (with image)
            // You can load the image using an image loading library like Glide or Picasso
            holder.removeButton.setImageResource(R.drawable.remove_background_icon)

            //change background of clicked item
            changeItemBackground(holder, listColors[position].isSelected && selectedItemPosition == position)

            holder.cardItem.setOnClickListener {

                if (selectedItemPosition != item_position) {
                    selectedItemPosition = item_position
                    colorsItemClick.colorItemClicked(null, item_position)
                }
            }
        }
        else if (holder is RegularItemViewHolder){

            holder.colorCard.setCardBackgroundColor(listColors[position].color.toInt())

            changeItemBackground(holder, listColors[position].isSelected && selectedItemPosition == position)

            holder.colorCard.setOnClickListener {

                if (selectedItemPosition != item_position) {
                    selectedItemPosition = item_position
                    colorsItemClick.colorItemClicked(listColors[position], item_position)
                }
            }
        }

        //pen 1
        pen1SelectionEvent.observeEvent(owner) { observedColor ->

            if (selectedItemPosition != observedColor.colorPosition) {
                selectedItemPosition = observedColor.colorPosition
            }
        }

        //pen 2
        pen2SelectionEvent.observeEvent(owner) { observedColor ->

            if (selectedItemPosition != observedColor.colorPosition) {
                selectedItemPosition = observedColor.colorPosition
            }
        }

        //pen3
        pen3SelectionEvent.observeEvent(owner) { observedColor ->

            if (selectedItemPosition != observedColor.colorPosition) {
                selectedItemPosition = observedColor.colorPosition
            }
        }
    }

    override fun getItemCount(): Int {
        return if (listColors.isNotEmpty())
            listColors.size
        else
            0
    }

    override fun getItemViewType(position: Int): Int {
        // Return different view types based on the position
        return if (position == 0) VIEW_TYPE_FIRST_ITEM else VIEW_TYPE_REGULAR_ITEM
    }

    @SuppressLint("NotifyDataSetChanged")
    fun changeSelectionStatus(position: Int) {
        for (image in listColors) {
            image.isSelected = selectedItemPosition == position
        }
        notifyDataSetChanged()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    fun changeItemBackground(holder: RecyclerView.ViewHolder, isSelected: Boolean) {
        if (holder is FirstItemViewHolder) {

            if (isSelected) {
                holder.cardItem.isSelected = true
                holder.removeButton.isSelected = true
                holder.selectionIcon.visibility = View.VISIBLE
            }
            else {
                holder.cardItem.isSelected = false
                holder.removeButton.isSelected = false
                holder.selectionIcon.visibility = View.GONE
            }
        } else if (holder is RegularItemViewHolder) {

            if (isSelected) {
                holder.colorCard.isSelected = true
                holder.colorCard.strokeColor = ContextCompat.getColor(QuickNotepad.appContext, R.color.wave_progress_color)
                holder.selectionIcon.visibility = View.VISIBLE
            } else {
                holder.colorCard.isSelected = false
                holder.colorCard.strokeColor = ContextCompat.getColor(QuickNotepad.appContext, R.color.white)
                holder.selectionIcon.visibility = View.GONE
            }

        }
    }

    class FirstItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardItem: MaterialCardView
        val removeButton: AppCompatImageView
        val selectionIcon: AppCompatImageView

        init {
            cardItem = itemView.findViewById(R.id.remove_item)
            removeButton = itemView.findViewById(R.id.remove_icon)
            selectionIcon = itemView.findViewById(R.id.selection_icon_draw)
        }
    }

    class RegularItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val colorCard: MaterialCardView = itemView.findViewById(R.id.color_card)
        val selectionIcon: AppCompatImageView = itemView.findViewById(R.id.selection_icon_draw)
    }

    interface ColorsItemSelectedEvent {
        fun colorItemClicked(color: ColorNote?, position: Int)
    }
}