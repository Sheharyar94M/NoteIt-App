package playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.dialogs.adapters

import android.annotation.SuppressLint
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import playaxis.appinn.note_it.R
import playaxis.appinn.note_it.application.QuickNotepad
import playaxis.appinn.note_it.repository.model.entities.GradientNote

class GradientColorsListAdapter(private var itemClicked: GradientItemCLickEvent) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var gradientList: ArrayList<GradientNote> = ArrayList()
    private val VIEW_TYPE_FIRST_ITEM = 0
    private val VIEW_TYPE_REGULAR_ITEM = 1

    private var selectedItemPosition = 0

    fun setGradient1List(gradientList: ArrayList<GradientNote>) {
        this.gradientList = gradientList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return if (viewType == VIEW_TYPE_FIRST_ITEM) {
            val view: View = inflater.inflate(R.layout.color_palette_dialog_list_item_view, parent, false)
            FirstItemViewHolder(view)
        } else {
            val view: View = inflater.inflate(R.layout.color_palette_dialog_list_item_view, parent, false)
            RegularItemViewHolder(view)
        }
    }

    override fun getItemCount(): Int {
        return if (gradientList.isNotEmpty())
            gradientList.size
        else
            0
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        //set the colors if the list of gradient is not null
        val item_position = position

        if (holder is FirstItemViewHolder) {
            // Bind data for the first item (with image)
            // You can load the image using an image loading library like Glide or Picasso
            holder.itemColor.setImageResource(R.drawable.remove_background_icon)
            holder.cardItem.strokeColor = ContextCompat.getColor(QuickNotepad.appContext,R.color.white)

            //change background of clicked item
            changeItemBackground(holder, gradientList[position].isSelected && selectedItemPosition == position)
        }
        else if (holder is RegularItemViewHolder) {
            // Bind data for regular items
            holder.itemColor.setImageResource(0)

            val gradientColors = intArrayOf(
                gradientList[position].color1,  // Start color
                gradientList[position].color2   // End color
            )
            val gradientDrawable = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, gradientColors)

            holder.itemColor.background = gradientDrawable
            holder.cardItem.strokeColor = ContextCompat.getColor(QuickNotepad.appContext,R.color.white)

            //change background of clicked item
            changeItemBackground(holder, gradientList[position].isSelected && selectedItemPosition == position)
        }

        holder.itemView.setOnClickListener {

            if (holder is FirstItemViewHolder) {
                // remove the background color
                if (selectedItemPosition != item_position) {
                    selectedItemPosition = item_position
                    itemClicked.gradientItemClicked(null,item_position)
                }

            }
            else if (holder is RegularItemViewHolder) {
                // set the background color
                if (selectedItemPosition != item_position) {
                    selectedItemPosition = item_position
                    itemClicked.gradientItemClicked(gradientList[position],item_position)
                }
            }
        }

    }

    override fun getItemViewType(position: Int): Int {
        // Return different view types based on the position
        return if (position == 0) VIEW_TYPE_FIRST_ITEM else VIEW_TYPE_REGULAR_ITEM
    }

    @SuppressLint("NotifyDataSetChanged")
    fun changeSelectionStatus(position: Int) {
        for (image in gradientList) {
            image.isSelected = selectedItemPosition == position
        }
        notifyDataSetChanged()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    fun changeItemBackground(holder: RecyclerView.ViewHolder, isSelected: Boolean) {
        if (holder is FirstItemViewHolder){
            if (isSelected) {
                holder.cardItem.isSelected = true
                holder.itemColor.isSelected = true
                holder.cardItem.strokeColor = ContextCompat.getColor(QuickNotepad.appContext,R.color.selected_tab_color)
            } else {
                holder.cardItem.isSelected = false
                holder.itemColor.isSelected = false
                holder.cardItem.strokeColor = ContextCompat.getColor(QuickNotepad.appContext,R.color.white)
            }
        }
        else if (holder is RegularItemViewHolder){
            if (isSelected) {
                holder.cardItem.isSelected = true
                holder.itemColor.isSelected = true
                holder.cardItem.strokeColor = ContextCompat.getColor(QuickNotepad.appContext,R.color.selected_tab_color)
            } else {
                holder.cardItem.isSelected = false
                holder.itemColor.isSelected = false
                holder.cardItem.strokeColor = ContextCompat.getColor(QuickNotepad.appContext,R.color.white)
            }
        }
    }

    class FirstItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardItem: MaterialCardView
        val itemColor: AppCompatImageView

        init {
            cardItem = itemView.findViewById(R.id.color_item)
            itemColor = itemView.findViewById(R.id.color_palette)
        }
    }
    // ViewHolder for regular items
    class RegularItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val cardItem: MaterialCardView
        val itemColor: AppCompatImageView

        init {
            cardItem = itemView.findViewById(R.id.color_item)
            itemColor = itemView.findViewById(R.id.color_palette)
        }
    }

    interface GradientItemCLickEvent {
        fun gradientItemClicked(gradient1: GradientNote?, item_position: Int)
    }
}