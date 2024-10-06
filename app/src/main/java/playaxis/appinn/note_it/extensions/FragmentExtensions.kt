package playaxis.appinn.note_it.extensions

import androidx.fragment.app.FragmentManager

/**
 * Returns whether this fragment manager contains a fragment with a [tag].
 */
operator fun FragmentManager.contains(tag: String) = this.findFragmentByTag(tag) != null
