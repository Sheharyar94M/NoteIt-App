package playaxis.appinn.note_it.repository

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import playaxis.appinn.note_it.repository.label.LabelsRepository
import playaxis.appinn.note_it.repository.model.entities.Label
import playaxis.appinn.note_it.repository.model.entities.LabelRef
import playaxis.appinn.note_it.repository.room.dao.LabelsDao
import javax.inject.Inject

class DefaultLabelsRepository @Inject constructor(private val labelsDao: LabelsDao) : LabelsRepository {

    // Data modification methods are wrapped in non-cancellable context
    // so that calling them in onPause for example won't cancel the transaction on the
    // subsequent onDestroy call, which cancels the coroutine scope.

    override suspend fun insertLabel(label: Label) = withContext(NonCancellable) {
        labelsDao.insert(label)
    }

    override suspend fun updateLabel(label: Label) = withContext(NonCancellable) {
        labelsDao.update(label)
    }

    override suspend fun deleteLabel(label: Label) {
        labelsDao.delete(label)
    }

    override suspend fun deleteLabels(labels: List<Label>) {
        labelsDao.deleteAll(labels)
    }

    override suspend fun getLabelById(id: Long) = labelsDao.getById(id)

    override suspend fun getLabelByName(name: String) = labelsDao.getLabelByName(name)

    override suspend fun insertLabelRefs(refs: List<LabelRef>) = labelsDao.insertRefs(refs)

    override suspend fun deleteLabelRefs(refs: List<LabelRef>) = labelsDao.deleteRefs(refs)

    override suspend fun getLabelIdsForNote(noteId: Long) =
        labelsDao.getLabelIdsForNote(noteId)

    override suspend fun countLabelRefs(labelId: Long) = labelsDao.countRefs(labelId)

    override fun getAllLabelsByUsage() = labelsDao.getAllByUsage()

    override suspend fun clearAllData() {
        labelsDao.clear()
    }
}
