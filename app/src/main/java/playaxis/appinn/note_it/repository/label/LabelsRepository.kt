package playaxis.appinn.note_it.repository.label

import kotlinx.coroutines.flow.Flow
import playaxis.appinn.note_it.repository.model.entities.Label
import playaxis.appinn.note_it.repository.model.entities.LabelRef

interface LabelsRepository {

    suspend fun insertLabel(label: Label): Long

    suspend fun updateLabel(label: Label)

    suspend fun deleteLabel(label: Label)
    suspend fun deleteLabels(labels: List<Label>)

    suspend fun getLabelById(id: Long): Label?
    suspend fun getLabelByName(name: String): Label?

    suspend fun insertLabelRefs(refs: List<LabelRef>)
    suspend fun deleteLabelRefs(refs: List<LabelRef>)
    suspend fun getLabelIdsForNote(noteId: Long): List<Long>
    suspend fun countLabelRefs(labelId: Long): Long

    fun getAllLabelsByUsage(): Flow<List<Label>>

    suspend fun clearAllData()
}
