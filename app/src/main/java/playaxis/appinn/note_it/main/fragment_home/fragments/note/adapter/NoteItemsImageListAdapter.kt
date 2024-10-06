package playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.DelicateCoroutinesApi
import playaxis.appinn.note_it.R
import playaxis.appinn.note_it.application.QuickNotepad
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.NoteItem
import playaxis.appinn.note_it.utils.MainUtils

class NoteItemsImageListAdapter(
    private val adapter: NoteAdapter,
    private val item: NoteItem,
    private val bindingAdapterPosition: Int
    ): RecyclerView.Adapter<NoteItemsImageListAdapter.NoteImageViewHolder>() {

    private var noteImages: List<String> = ArrayList()

    fun setImagesList(noteImages: List<String>){
        this.noteImages = noteImages
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.note_item_image_list_item_view,parent,false)
        return NoteImageViewHolder(view)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onBindViewHolder(holder: NoteImageViewHolder, position: Int) {

        //loading the images in the item view
        if (noteImages[position].startsWith("content://") ||
            noteImages[position].startsWith("file://") ||
            noteImages[position].startsWith("http://") ||
            noteImages[position].startsWith("https://")) {

            Glide.with(holder.itemView.context)
                .load(Uri.parse(noteImages[position]))
                .into(holder.noteImageItem)
        }
        else {

            //displaying image drawn
            Log.i("noteClicked_noteImage: ",noteImages[position])
            val drawing = MainUtils.deserializeDrawing(noteImages[position])

            //displaying image
            Glide.with(holder.itemView.context).load(
                MainUtils.convertPathsToBitmap(drawing, QuickNotepad.appContext.resources)).into(holder.noteImageItem)
        }

        //click event
        holder.imageItem.setOnClickListener {
            //click
            adapter.callback.onNoteItemClicked(item, bindingAdapterPosition)
        }

        holder.imageItem.setOnLongClickListener {
            //click
            adapter.callback.onNoteItemLongClicked(item, bindingAdapterPosition)
            true
        }
    }


    override fun getItemCount(): Int {
        return if (noteImages.isNotEmpty())
            noteImages.size
        else
            0
    }

    class NoteImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){

        val noteImageItem: AppCompatImageView = itemView.findViewById(R.id.image_list_note)
        val imageItem: ConstraintLayout = itemView.findViewById(R.id.item_image)
    }
}