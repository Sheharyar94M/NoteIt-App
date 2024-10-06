package playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.speech

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import playaxis.appinn.note_it.main.fragment_home.HomeViewModel
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.speech.helper.Speech
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.NoteItemFactory
import playaxis.appinn.note_it.main.viewModels.AssistedSavedStateViewModelFactory
import playaxis.appinn.note_it.preferences.PrefsManager
import playaxis.appinn.note_it.repository.folder.FoldersRepository
import playaxis.appinn.note_it.repository.label.LabelsRepository
import playaxis.appinn.note_it.repository.model.utils.ReminderAlarmManager
import playaxis.appinn.note_it.repository.note.NotesRepository

class SpeechViewModel @AssistedInject constructor(
    @Assisted savedStateHandle: SavedStateHandle,
    notesRepository: NotesRepository,
    labelsRepository: LabelsRepository,
    foldersRepository: FoldersRepository,
    prefs: PrefsManager,
    reminderAlarmManager: ReminderAlarmManager,
    noteItemFactory: NoteItemFactory,
) :  HomeViewModel(savedStateHandle, notesRepository, labelsRepository, foldersRepository,prefs, reminderAlarmManager, noteItemFactory) {

    private var listAudios: ArrayList<Speech> = ArrayList()
    private var audiosLiveData: MutableLiveData<ArrayList<Speech>> = MutableLiveData()

    fun getAudiosList(): MutableLiveData<ArrayList<Speech>> {
        return audiosLiveData
    }
    fun setAudioList(listAudios: ArrayList<Speech>){
        this.listAudios = listAudios
        audiosLiveData.value = listAudios
    }

    fun addAudioItem(audio: Speech){
        listAudios.add(audio)
        audiosLiveData.value = listAudios
    }

    fun deleteSpeechItem(speech: Speech) {

        //delete speech item
        if (listAudios.isNotEmpty()){
            for (sp in listAudios){
                if (sp.id == speech.id){

                    listAudios.remove(speech)
                    //updating the liveData also
                    audiosLiveData.value = listAudios
                    break
                }
            }
        }
        //updating list
        setAudioList(listAudios)
    }

    @AssistedFactory
    interface Factory : AssistedSavedStateViewModelFactory<SpeechViewModel> {
        override fun create(savedStateHandle: SavedStateHandle): SpeechViewModel
    }

    companion object{
        private const val TRASH_REMINDER_ITEM_ID = -1L
        private const val BATTERY_RESTRICTED_ITEM_ID = -8L
    }
}