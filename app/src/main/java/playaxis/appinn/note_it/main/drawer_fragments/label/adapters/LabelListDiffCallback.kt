package playaxis.appinn.note_it.main.drawer_fragments.label.adapters

import androidx.recyclerview.widget.DiffUtil

class LabelListDiffCallback : DiffUtil.ItemCallback<LabelListItem>() {

    override fun areItemsTheSame(old: LabelListItem, new: LabelListItem) = old.id == new.id

    override fun areContentsTheSame(old: LabelListItem, new: LabelListItem) =
        old.label.name == new.label.name &&
                old.label.hidden == new.label.hidden &&
                old.checked == new.checked
}
