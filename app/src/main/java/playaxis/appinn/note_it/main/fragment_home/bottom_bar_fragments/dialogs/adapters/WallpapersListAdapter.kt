package playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.dialogs.adapters

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import playaxis.appinn.note_it.R
import playaxis.appinn.note_it.application.QuickNotepad
import playaxis.appinn.note_it.repository.model.entities.helper.ImageNoteBackground

class WallpapersListAdapter(private var itemClicked: WallpaperItemCLickEvent): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var wallpaperList: ArrayList<ImageNoteBackground> = ArrayList()
    private val VIEW_TYPE_FIRST_ITEM = 0
    private val VIEW_TYPE_REGULAR_ITEM = 1

    private var selectedItemPosition = 0

    fun setWallpaperList(wallpaperList: ArrayList<ImageNoteBackground>){
        this.wallpaperList = wallpaperList
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
        return if (wallpaperList.isNotEmpty())
            wallpaperList.size
        else
            0
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        //set the colors if the list of colors is not null
        val item_position = position

        if (holder is FirstItemViewHolder) {
            // Bind data for the first item (with image)
            // You can load the image using an image loading library like Glide or Picasso
            holder.itemColor.setImageResource(R.drawable.remove_background_icon)
            holder.cardItem.strokeColor = ContextCompat.getColor(QuickNotepad.appContext,R.color.white)

            //change background of clicked item
            changeItemBackground(holder, wallpaperList[position].isSelected && selectedItemPosition == position)
        }
        else if (holder is RegularItemViewHolder) {
            // Bind data for regular items

            holder.itemColor.setImageResource(0)
            holder.itemColor.setImageDrawable(wallpaperList[position].image)
            holder.cardItem.strokeColor = ContextCompat.getColor(QuickNotepad.appContext,R.color.white)

            //change background of clicked item
            changeItemBackground(holder, wallpaperList[position].isSelected && selectedItemPosition == position)
        }

        holder.itemView.setOnClickListener {

            if (holder is FirstItemViewHolder) {
                // remove the background color
                if (selectedItemPosition != item_position) {
                    selectedItemPosition = item_position
                    itemClicked.wallpaperItemClicked(null,item_position)
                }

            }
            else if (holder is RegularItemViewHolder) {
                // set the background color
                if (selectedItemPosition != item_position) {
                    selectedItemPosition = item_position
                    itemClicked.wallpaperItemClicked(wallpaperList[position].image,item_position)
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        // Return different view types based on the position
        return if (position == 0) VIEW_TYPE_FIRST_ITEM else VIEW_TYPE_REGULAR_ITEM
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

    @SuppressLint("NotifyDataSetChanged")
    fun changeSelectionStatus(position: Int) {
        for (image in wallpaperList) {
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

    interface WallpaperItemCLickEvent{
        fun wallpaperItemClicked(wallpaper: Drawable?, item_position: Int)
    }
}