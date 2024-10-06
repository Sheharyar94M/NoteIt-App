package playaxis.appinn.note_it.navigation

import android.os.Parcelable
import androidx.navigation.NavDirections
import kotlinx.parcelize.Parcelize
import playaxis.appinn.note_it.repository.model.entities.Label
import playaxis.appinn.note_it.repository.model.entities.NoteStatus

/**
 * Different destinations accessible from the navigation drawer.
 */
sealed interface NavigationDestination {
    /**
     * Destination to navigate to another fragment in the navigation graph.
     */
    data class NavGraph(val directions: NavDirections) : NavigationDestination
}

/**
 * A destination accessible only by changing the content of the [HomeFragment].
 */
sealed interface HomeDestination : NavigationDestination, Parcelable {
    /**
     * Destination to view all notes of with a specific [status].
     */
    @Parcelize
    data class Status(val status: NoteStatus) : HomeDestination

    /**
     * Destination to view all notes with a [label].
     */
    @Parcelize
    data class Labels(val label: Label) : HomeDestination

    /**
     * Destination to view all notes with a reminder.
     */
    @Parcelize
    object Reminders : HomeDestination
}