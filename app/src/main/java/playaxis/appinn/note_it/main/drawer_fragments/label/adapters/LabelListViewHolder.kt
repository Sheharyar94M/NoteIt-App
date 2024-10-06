package playaxis.appinn.note_it.main.drawer_fragments.label.adapters

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import playaxis.appinn.note_it.databinding.EditCreateLabelItemViewBinding
import playaxis.appinn.note_it.databinding.LabelNameItemViewBinding

class LabelListViewHolder(val binding: LabelNameItemViewBinding) : RecyclerView.ViewHolder(binding.root) {


    fun bind(item: LabelListItem, adapter: LabelAdapter) {

        //setting values 1st
        binding.labelName.text = item.label.name
        binding.labelChk.isChecked = item.checked
        val view = binding.root

        view.setOnClickListener {
            item.checked = binding.labelChk.isChecked
            adapter.callback.onLabelItemClicked(item, bindingAdapterPosition)
        }
    }
}

class LabelEditListViewHolder(val binding: EditCreateLabelItemViewBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: LabelListItem, adapter: LabelAdapter) {

        binding.labelName.text = item.label.name

        binding.editItem.setOnClickListener {

            binding.labelName.isEnabled = true
            binding.viewLabelLayout.visibility = View.GONE
            binding.editLabelLayout.visibility = View.VISIBLE

            //set text on the editor
            binding.labelNameEdit.setText(item.label.name)
            binding.labelNameEdit.requestFocus()
        }

        binding.saveLabel.setOnClickListener {
            //update view
            binding.viewLabelLayout.visibility = View.VISIBLE
            binding.editLabelLayout.visibility = View.GONE

            //updating item name
            item.label.name = binding.labelNameEdit.text.toString()
            //save the label
            adapter.callback.editItemButtonClicked(item)
        }

        binding.closeEditor.setOnClickListener {

            //update view
            binding.viewLabelLayout.visibility = View.VISIBLE
            binding.editLabelLayout.visibility = View.GONE
        }

        binding.deleteItem.setOnClickListener {
            //delete item
            adapter.callback.deleteItemButtonClicked(item)
        }
    }
}