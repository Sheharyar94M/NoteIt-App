package playaxis.appinn.note_it.extensions

import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavGraph
import androidx.navigation.Navigator

fun NavController.navigateSafe(
    directions: NavDirections,
    allowSameDest: Boolean = false,
    extras: Navigator.Extras? = null
) {
    // Get action by ID. If action doesn't exist, return.
    val action = (currentDestination ?: graph).getAction(directions.actionId) ?: return
    var destId = action.destinationId
    val dest = graph.findNode(destId)
    if (dest is NavGraph) {
        // Action destination is a nested graph, which isn't a real destination.
        // The real destination is the start destination of that graph so resolve it.
        destId = dest.startDestinationId
    }
    if (allowSameDest || currentDestination?.id != destId) {
        if (extras != null) {
            navigate(directions, extras)
        }
        else {
            navigate(directions)
        }
    }
}
