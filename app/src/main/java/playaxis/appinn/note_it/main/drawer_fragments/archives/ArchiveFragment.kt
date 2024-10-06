package playaxis.appinn.note_it.main.drawer_fragments.archives

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.ActionMode
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.SharedElementCallback
import androidx.core.view.OneShotPreDrawListener
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.snackbar.Snackbar
import playaxis.appinn.note_it.MainNavigationDirections
import playaxis.appinn.note_it.R
import playaxis.appinn.note_it.application.QuickNotepad
import playaxis.appinn.note_it.databinding.FragmentArchiveBinding
import playaxis.appinn.note_it.extensions.navigateSafe
import playaxis.appinn.note_it.main.fragment_home.HomeViewModel
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.NoteAdapter
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.NoteListLayoutMode
import playaxis.appinn.note_it.main.utils.StatusChange
import playaxis.appinn.note_it.main.utils.observeEvent
import playaxis.appinn.note_it.main.viewModels.MainViewModel
import playaxis.appinn.note_it.navigation.HomeDestination
import playaxis.appinn.note_it.preferences.PrefsManager
import playaxis.appinn.note_it.repository.model.entities.NoteStatus
import playaxis.appinn.note_it.repository.model.entities.NoteType
import java.text.NumberFormat
import javax.inject.Inject

class ArchiveFragment : Fragment() {

    private lateinit var binding: FragmentArchiveBinding

    private val viewModelHome: HomeViewModel by activityViewModels()
    private val viewModelMain: MainViewModel by activityViewModels()

    @Inject
    lateinit var prefsManager: PrefsManager

    private var spanCount = 1
    private var currentHomeDestinationChanged: Boolean = false
    private var isSharedElementTransitionPlaying: Boolean = false
    private var rcvOneShotPreDrawListener: OneShotPreDrawListener? = null
    private var createdNote: View? = null
    private var createdNoteId: Long? = null

    private var actionMode: ActionMode? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        binding = FragmentArchiveBinding.inflate(inflater, container, false)

        //injection initialization
        (QuickNotepad.appContext as QuickNotepad).appComponent.inject(this)

        setExitSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(
                names: MutableList<String>?,
                sharedElements: MutableMap<String, View>?
            ) {
                isSharedElementTransitionPlaying = !sharedElements.isNullOrEmpty()
                super.onMapSharedElements(names, sharedElements)
            }
        })

        viewModelHome.changeSelection = true
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //recyclerview
        val rcv = binding.archiveList
        rcv.setHasFixedSize(true)
        val adapter = NoteAdapter(requireContext(), viewModelHome, prefsManager)
        val layoutManager = StaggeredGridLayoutManager(spanCount, StaggeredGridLayoutManager.VERTICAL)
        rcv.adapter = adapter
        rcv.layoutManager = layoutManager
        rcv.itemAnimator = object : DefaultItemAnimator() {
            override fun animateAppearance(
                viewHolder: RecyclerView.ViewHolder,
                preLayoutInfo: ItemHolderInfo?,
                postLayoutInfo: ItemHolderInfo
            ): Boolean {
                return if (preLayoutInfo != null && (preLayoutInfo.left != postLayoutInfo.left
                            || preLayoutInfo.top != postLayoutInfo.top)
                ) {
                    // item move, handle normally
                    super.animateAppearance(viewHolder, preLayoutInfo, postLayoutInfo)
                } else {
                    // do not animate new item appearance
                    // this is mainly to avoid animating the whole list when fragment view is recreated.
                    dispatchAddFinished(viewHolder)
                    false
                }
            }
        }

        observers(adapter,layoutManager)
    }

    private fun noteListCommitCallback() {
        // Scroll to top of notes list, when the HomeDestination has changed
        if (currentHomeDestinationChanged) {
            if (binding.archiveList.adapter!!.itemCount > 0) {
                binding.archiveList.scrollToPosition(0)
                binding.archiveList.scrollBy(0, -1)
            }
            currentHomeDestinationChanged = false
        }
    }

    private fun setupNoteItemsObserver(adapter: NoteAdapter) {

        viewModelHome.noteItems.observe(viewLifecycleOwner) { items ->

            Log.i("setupNoteItemsList: ",items.size.toString())

            if (items.isNotEmpty()){
                binding.archiveList.visibility = View.VISIBLE
                binding.noItemsView.visibility = View.GONE

                adapter.submitList(items, ::noteListCommitCallback)
            }
            else{
                binding.archiveList.visibility = View.GONE
                binding.noItemsView.visibility = View.VISIBLE
            }

            if (isSharedElementTransitionPlaying) {
                // Remove observer to prevent changes to the recyclerview content,
                // while a transition is playing.
                viewModelHome.noteItems.removeObservers(viewLifecycleOwner)
            }
        }
    }

    private fun observers(adapter: NoteAdapter, layoutManager: StaggeredGridLayoutManager){

        setupNoteItemsObserver(adapter)

        //it will set data in the list which will be passed to the main list adapter
        viewModelMain.currentHomeDestination.observe(viewLifecycleOwner) { destination ->
            viewModelHome.setDestination(destination)
        }

        viewModelHome.listLayoutMode.observe(viewLifecycleOwner) { mode ->
            layoutManager.spanCount = resources.getInteger(when (mode!!) {
                NoteListLayoutMode.LIST -> R.integer.note_list_layout_span_count
                NoteListLayoutMode.GRID -> R.integer.note_grid_layout_span_count
            })
            spanCount = layoutManager.spanCount
            adapter.updateForListLayoutChange()
        }

        viewModelHome.editItemEvent.observeEvent(viewLifecycleOwner) { note, pos ->

            //getting the controller
            val host = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_main_fragment_container) as NavHostFragment
            val navController = host.navController
            navController.setGraph(R.navigation.main_navigation)

            var itemView: View? = null

            if (itemView != null) {


                itemView = binding.archiveList.findViewHolderForAdapterPosition(pos)!!.itemView.findViewById(R.id.card_view)

                val extras = FragmentNavigatorExtras(itemView to "noteContainer${note.id}")

//            // If the selected note isn't completely in view, move it into view.
//            val firstVisibleItem = layoutManager.findFirstCompletelyVisibleItemPositions(null).minOrNull()
//            val lastVisibleItem = layoutManager.findLastCompletelyVisibleItemPositions(null).maxOrNull()
//            if (firstVisibleItem != null && lastVisibleItem != null && (pos < firstVisibleItem || pos > lastVisibleItem)) {
//                binding.notesList.scrollToPosition(pos)
//            }

                //setting the view
                viewModelMain.bottomBarItemsListenerEvent("add_note")

                //making editing true
                viewModelHome.editing = true

                //navigating to the fragment
                if (note.type == NoteType.TEXT) {
                    if (note.noteAudios.isNotEmpty())
                        navController.navigateSafe(
                            ArchiveFragmentDirections.actionArchiveFragmentToSpeechFragment(note.id),
                            extras = extras
                        )
                    else
                        navController.navigateSafe(
                            ArchiveFragmentDirections.actionArchiveFragmentToAddEditFragment(
                                note.id
                            ), extras = extras
                        )
                }
                else if (note.type == NoteType.LIST)
                    navController.navigateSafe(
                        ArchiveFragmentDirections.actionArchiveFragmentToTodoFragment(note.id),
                        extras = extras
                    )
            }
            else{

                //setting the view
                viewModelMain.bottomBarItemsListenerEvent("add_note")

                //making editing true
                viewModelHome.editing = true

                //navigating to the fragment
                if (note.type == NoteType.TEXT) {
                    if (note.noteAudios.isNotEmpty())
                        navController.navigateSafe(
                            ArchiveFragmentDirections.actionArchiveFragmentToSpeechFragment(note.id)
                        )
                    else
                        navController.navigateSafe(
                            ArchiveFragmentDirections.actionArchiveFragmentToAddEditFragment(
                                note.id
                            )
                        )
                }
                else if (note.type == NoteType.LIST)
                    navController.navigateSafe(
                        ArchiveFragmentDirections.actionArchiveFragmentToTodoFragment(note.id)
                    )
            }
        }

        //show the view when no data is available
        viewModelHome.placeholderData.observe(viewLifecycleOwner) { data ->
            if (data != null) {
                binding.noItemsView.setImageResource(data.iconId)
            } else if (binding.noItemsView.isVisible) {
                // Recreate layout manager to prevent an issue with weird spacing at the top of the recyclerview
                // after the placeholder has been shown.
                binding.archiveList.layoutManager = StaggeredGridLayoutManager(spanCount, StaggeredGridLayoutManager.VERTICAL)
            }

            binding.noItemsView.isVisible = data != null
        }

        viewModelHome.statusChangeEvent.observeEvent(viewLifecycleOwner) { statusChange ->
            viewModelHome.onStatusChange(statusChange)
        }

        viewModelHome.showLabelsFragmentEvent.observeEvent(viewLifecycleOwner) { noteIds ->
            //getting the controller
            val host = childFragmentManager.findFragmentById(R.id.nav_host_nested_fragments) as NavHostFragment
            val navController = host.navController
            navController.setGraph(R.navigation.home_nav_graph)

            navController.navigateSafe(MainNavigationDirections.mainToCreateEditLabelFragment(noteIds.toLongArray()))
        }

        viewModelHome.messageEvent.observeEvent(viewLifecycleOwner) { messageId ->
            Snackbar.make(requireView(), messageId, Snackbar.LENGTH_SHORT)
                .setGestureInsetBottomIgnored(true)
                .show()
        }
        viewModelHome.statusChangeEvent.observeEvent(viewLifecycleOwner) { statusChange ->
            showMessageForStatusChange(statusChange)
        }

        viewModelMain.currentHomeDestination.observe(viewLifecycleOwner) {
            currentHomeDestinationChanged = true
        }
        viewModelHome.sharedElementTransitionFinishedEvent.observeEvent(viewLifecycleOwner) {
            isSharedElementTransitionPlaying = false
            // Reattach observers
            setupNoteItemsObserver(adapter)

            // Reset the transition names of the fab and the newly created note
            if (createdNote != null && createdNoteId != null) {
                createdNote?.transitionName = "noteContainer$createdNoteId"
            }
        }
        viewModelHome.noteCreatedEvent.observeEvent(viewLifecycleOwner) { noteId ->
            rcvOneShotPreDrawListener?.removeListener()
            OneShotPreDrawListener.add(binding.archiveList) {
                exitTransition = null
                enterTransition = null

                // Change the transition names, so that the shared element transition returns to
                // the newly created note item in the recyclerview instead of to the FAB.
                for (c in binding.archiveList.children) {
                    if (c.findViewById<View>(R.id.card_view)?.transitionName == "noteContainer$noteId") {
                        c.transitionName = "createNoteTransition"

                        createdNoteId = noteId
                        createdNote = c
                        break
                    }
                }

                startPostponedEnterTransition()
            }
        }
    }

    @SuppressLint("WrongConstant")
    private fun showMessageForStatusChange(statusChange: StatusChange) {
        val messageId = when (statusChange.newStatus) {
            NoteStatus.ACTIVE -> if (statusChange.oldStatus == NoteStatus.DELETED) {
                R.plurals.edit_message_move_restore
            } else {
                R.plurals.edit_message_move_unarchive
            }
            NoteStatus.ARCHIVED -> R.plurals.edit_move_archive_message
            NoteStatus.DELETED -> R.plurals.edit_message_move_delete
        }
        val count = statusChange.oldNotes.size
        val message = requireContext().resources.getQuantityString(messageId, count, count)

        Snackbar.make(requireView(), message, STATUS_CHANGE_SNACKBAR_DURATION)
            .setAction(R.string.action_undo) {
                viewModelHome.undoStatusChange()
            }
            .setGestureInsetBottomIgnored(true)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        actionMode?.finish()

        rcvOneShotPreDrawListener = null
        createdNote = null
        createdNoteId = null
    }

    override fun onDetach() {
        super.onDetach()

        viewModelMain.bottomFragmentItemDetachEventEvent()
        //set the destination also
        viewModelHome.setDestination(HomeDestination.Status(NoteStatus.ACTIVE))
    }
    companion object {
        private val NUMBER_FORMAT = NumberFormat.getInstance()

        private const val DELETE_CONFIRM_DIALOG_TAG = "delete_confirm_dialog"

        private const val STATUS_CHANGE_SNACKBAR_DURATION = 7500
    }
}