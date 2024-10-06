package playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.note.adapters

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import playaxis.appinn.note_it.R
import playaxis.appinn.note_it.application.QuickNotepad
import playaxis.appinn.note_it.utils.MainUtils

class NoteImageListAdapter(
    private val clickEvent: NoteImageClickedEvent
): RecyclerView.Adapter<NoteImageListAdapter.NoteImageViewHolder>() {

    private var noteImages: List<String> = ArrayList()

    fun setImagesList(noteImages: List<String>){
        this.noteImages = noteImages
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.note_image_list_item_view,parent,false)
        return NoteImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteImageViewHolder, position: Int) {

        //loading the images in the item view
        if (noteImages[position].startsWith("content://") ||
            noteImages[position].startsWith("file://") ||
            noteImages[position].startsWith("http://") ||
            noteImages[position].startsWith("https://")) {

            Glide.with(QuickNotepad.appContext).load(Uri.parse(noteImages[position])).into(holder.noteImageItem)
        }
        else {
            //displaying image drawn

            Log.i("noteClicked_noteImage: ",noteImages[position])
            val drawing = MainUtils.deserializeDrawing(noteImages[position])
            //displaying image
            Glide.with(QuickNotepad.appContext).load(MainUtils.convertPathsToBitmap(drawing,QuickNotepad.appContext.resources)).into(holder.noteImageItem)
        }

        holder.itemView.setOnClickListener {
            clickEvent.noteImageClicked(noteImages[position],position)
        }
    }


    override fun getItemCount(): Int {
        return if (noteImages.isNotEmpty())
            noteImages.size
        else
            0
    }

    class NoteImageViewHolder(itemView:View) : RecyclerView.ViewHolder(itemView){

        val noteImageItem: AppCompatImageView = itemView.findViewById(R.id.image_note)
    }

    interface NoteImageClickedEvent{
        fun noteImageClicked(image: String,position: Int)
    }
}