package playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.note.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import playaxis.appinn.note_it.repository.appData.AppDataRepository
import playaxis.appinn.note_it.repository.model.entities.ColorNote
import playaxis.appinn.note_it.repository.model.entities.GradientNote
import playaxis.appinn.note_it.repository.model.entities.helper.ImageNoteBackground

abstract class AbstractAddEditNoteViewModel (private var appDataRepository: AppDataRepository) : ViewModel(){

    fun getColors(): MutableLiveData<ArrayList<ColorNote>> {
        return appDataRepository.getColors()
    }

    fun getGradients(): MutableLiveData<ArrayList<GradientNote>> {
        return appDataRepository.getGradientColors()
    }

    fun getWallpapers(): MutableLiveData<ArrayList<ImageNoteBackground>> {
        return appDataRepository.getWallpaperImages()
    }
}