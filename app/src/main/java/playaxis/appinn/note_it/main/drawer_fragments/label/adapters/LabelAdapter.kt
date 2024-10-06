package playaxis.appinn.note_it.main.drawer_fragments.label.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import playaxis.appinn.note_it.databinding.EditCreateLabelItemViewBinding
import playaxis.appinn.note_it.databinding.LabelNameItemViewBinding

class LabelAdapter(
    val context: Context,
    val callback: Callback,
    private val editView: Boolean
) : ListAdapter<LabelListItem, ViewHolder>(LabelListDiffCallback()) {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (editView){
            LabelEditListViewHolder(EditCreateLabelItemViewBinding.inflate(inflater,parent,false))
        }
        else{
            LabelListViewHolder(LabelNameItemViewBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        when(holder){
            is LabelEditListViewHolder ->{
                holder.bind(getItem(position), this)
            }
            is LabelListViewHolder ->{
                holder.bind(getItem(position), this)
            }
        }
    }
    override fun getItemId(position: Int) = getItem(position).id

    interface Callback {
        val shouldHighlightCheckedItems: Boolean

        /** Called when a label [item] at [pos] is clicked. */
        fun onLabelItemClicked(item: LabelListItem, pos: Int)

        /** Called when the icon of a label [item] at [pos] is clicked. */
        fun onLabelItemIconClicked(item: LabelListItem, pos: Int)

        fun editItemButtonClicked(item: LabelListItem)

        fun deleteItemButtonClicked(item: LabelListItem)
    }
}
