package playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.dialogs.interfaces

import playaxis.appinn.note_it.repository.model.entities.GradientNote

interface GradientSelectedInterface {
    fun selectedGradient(gradient1: GradientNote?)
}