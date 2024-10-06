package playaxis.appinn.note_it.main.fragment_home.fragments.folder.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import playaxis.appinn.note_it.R
import playaxis.appinn.note_it.main.fragment_home.fragments.folder.helper.FolderSelectionItem
import playaxis.appinn.note_it.repository.model.entities.FolderWithNotes

class FolderListAdapter(private var folderItemCLickEvent: FolderItemCLickEvent) :
    RecyclerView.Adapter<FolderListAdapter.FolderListViewHolder>() {

    private var listFolders: List<FolderSelectionItem> = ArrayList()
    var selectionFolder: Boolean = false
    private var selectedItemPosition = -1

    @SuppressLint("NotifyDataSetChanged")
    fun setFolderList(listFolders: List<FolderSelectionItem>) {
        this.listFolders = listFolders
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderListViewHolder {

        val view = LayoutInflater.from(parent.context).inflate(R.layout.folder_list_item_view, parent, false)
        return FolderListViewHolder(view)
    }

    override fun onBindViewHolder(holder: FolderListViewHolder, position: Int) {

        val item_position = position

        //folder name set
        holder.folder.text = listFolders[position].folderWithNotes.folder.name

        //selection status
        if (selectionFolder){

            //change background of clicked item
            changeItemBackground(holder, listFolders[position].isSelected && selectedItemPosition == position)
            holder.selectionIcon.visibility = View.VISIBLE
        }
        else
            holder.selectionIcon.visibility = View.GONE

        holder.itemView.setOnClickListener {

            //set the card stroke color and width and send the clicked item to the fragment
            if (selectionFolder){

                if (selectedItemPosition != item_position) {
                    selectedItemPosition = item_position
                    folderItemCLickEvent.folderClicked(listFolders[position].folderWithNotes,item_position)
                }
            }
            else{

                // open the folder
                folderItemCLickEvent.folderClicked(listFolders[position].folderWithNotes,item_position)
            }
        }
    }

    override fun getItemCount(): Int {
        return if (listFolders.isNotEmpty())
            listFolders.size
        else
            0
    }

    @SuppressLint("NotifyDataSetChanged")
    fun changeSelectionStatus(position: Int) {
        for (folder in listFolders) {
            folder.isSelected = selectedItemPosition == position
        }
        notifyDataSetChanged()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun changeItemBackground(holder: FolderListViewHolder, isSelected: Boolean) {
        if (isSelected) {
            holder.selectionIcon.isSelected = true
            holder.selectionIcon.setImageResource(0)
            holder.selectionIcon.setImageResource(R.drawable.selection_icon)
        } else {
            holder.selectionIcon.isSelected = false
            holder.selectionIcon.setImageResource(0)
            holder.selectionIcon.setImageResource(R.drawable.unselection)
        }
    }

    class FolderListViewHolder(itemView: View) : ViewHolder(itemView) {

        var folder: AppCompatTextView = itemView.findViewById(R.id.folder_item_name)
        var selectionIcon: AppCompatImageView = itemView.findViewById(R.id.selection_icon)
        var parent: ConstraintLayout = itemView.findViewById(R.id.parent_layout)
    }

    interface FolderItemCLickEvent {
        fun folderClicked(folderNote: FolderWithNotes, position: Int)
    }
}