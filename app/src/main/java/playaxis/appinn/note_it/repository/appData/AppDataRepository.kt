package playaxis.appinn.note_it.repository.appData

import androidx.lifecycle.MutableLiveData
import playaxis.appinn.note_it.repository.model.entities.ColorNote
import playaxis.appinn.note_it.repository.model.entities.GradientNote
import playaxis.appinn.note_it.repository.model.entities.helper.ImageNoteBackground

interface AppDataRepository {
    fun getColors(): MutableLiveData<ArrayList<ColorNote>>
    fun getGradientColors(): MutableLiveData<ArrayList<GradientNote>>
    fun getWallpaperImages(): MutableLiveData<ArrayList<ImageNoteBackground>>
}