package playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.draw.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
    private var viewLifecycleOwner: LifecycleOwner,
    private var colorsItemClick: ColorsItemSelectedEvent,
    private var pen1SelectionEvent: MutableLiveData<Event<ColorObserve>>,
    private var pen2SelectionEvent: MutableLiveData<Event<ColorObserve>>,
    private var pen3SelectionEvent: MutableLiveData<Event<ColorObserve>>
) :
    RecyclerView.Adapter<ColorListAdapter.ColorListViewHolder>() {

    private var listColors = ArrayList<ColorNote>()
    private var selectedItemPosition = 0

    fun setColors(listColors: ArrayList<ColorNote>) {
        this.listColors = listColors
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.draw_color_selection_list, parent, false)
        return ColorListViewHolder(view)
    }

    override fun onBindViewHolder(holder: ColorListViewHolder, position: Int) {

        val item_position = position
        holder.colorCard.setCardBackgroundColor(listColors[position].color.toInt())

        changeItemBackground(holder, listColors[position].isSelected && selectedItemPosition == position)

        holder.colorCard.setOnClickListener {

            if (selectedItemPosition != item_position) {
                selectedItemPosition = item_position
                colorsItemClick.colorItemClicked(listColors[position], item_position)
            }
        }

        //pen1 selection event
        pen1SelectionEvent.observeEvent(viewLifecycleOwner){ colorSelected ->

            if (selectedItemPosition != colorSelected.colorPosition) {
                selectedItemPosition = colorSelected.colorPosition
                colorsItemClick.colorItemClicked(colorSelected.colorSelected, colorSelected.colorPosition)
            }
        }

        //pen2 selection event
        pen2SelectionEvent.observeEvent(viewLifecycleOwner){ colorSelected ->

            if (selectedItemPosition != colorSelected.colorPosition) {
                selectedItemPosition = colorSelected.colorPosition
                colorsItemClick.colorItemClicked(colorSelected.colorSelected, colorSelected.colorPosition)
            }
        }

        //pen3 selection event
        pen3SelectionEvent.observeEvent(viewLifecycleOwner){ colorSelected ->

            if (selectedItemPosition != colorSelected.colorPosition) {
                selectedItemPosition = colorSelected.colorPosition
                colorsItemClick.colorItemClicked(colorSelected.colorSelected, colorSelected.colorPosition)
            }
        }
    }

    override fun getItemCount(): Int {
        return if (listColors.isNotEmpty())
            listColors.size
        else
            0
    }

    @SuppressLint("NotifyDataSetChanged")
    fun changeSelectionStatus(position: Int) {
        for (image in listColors) {
            image.isSelected = selectedItemPosition == position
        }
        notifyDataSetChanged()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun changeItemBackground(holder: ColorListViewHolder, isSelected: Boolean) {
        if (isSelected) {
            holder.colorCard.isSelected = true
            holder.colorCard.strokeColor = ContextCompat.getColor(QuickNotepad.appContext, R.color.wave_progress_color)
            holder.selectionIcon.visibility = View.VISIBLE
        }
        else {
            holder.colorCard.isSelected = false
            holder.colorCard.strokeColor = ContextCompat.getColor(QuickNotepad.appContext, R.color.white)
            holder.selectionIcon.visibility = View.GONE
        }
    }

    class ColorListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val colorCard: MaterialCardView = itemView.findViewById(R.id.color_card)
        val selectionIcon: AppCompatImageView = itemView.findViewById(R.id.selection_icon_draw)
    }

    interface ColorsItemSelectedEvent {
        fun colorItemClicked(color: ColorNote, position: Int)
    }
}