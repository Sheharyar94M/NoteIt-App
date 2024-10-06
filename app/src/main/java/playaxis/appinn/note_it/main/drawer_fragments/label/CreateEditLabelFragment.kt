package playaxis.appinn.note_it.main.drawer_fragments.label

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.createViewModelLazy
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.transition.MaterialElevationScale
import playaxis.appinn.note_it.MainNavigationDirections
import playaxis.appinn.note_it.R
import playaxis.appinn.note_it.application.QuickNotepad
import playaxis.appinn.note_it.common.ConfirmDialog
import playaxis.appinn.note_it.databinding.FragmentLabelCreateEditBinding
import playaxis.appinn.note_it.extensions.navigateSafe
import playaxis.appinn.note_it.main.drawer_fragments.label.adapters.LabelAdapter
import playaxis.appinn.note_it.main.drawer_fragments.label.viewmodels.LabelEditViewModel
import playaxis.appinn.note_it.main.drawer_fragments.label.viewmodels.LabelViewModel
import playaxis.appinn.note_it.main.fragment_home.HomeViewModel
import playaxis.appinn.note_it.main.utils.observeEvent
import playaxis.appinn.note_it.main.viewModels.MainViewModel
import playaxis.appinn.note_it.main.viewModels.viewModel
import playaxis.appinn.note_it.navigation.HomeDestination
import playaxis.appinn.note_it.repository.model.entities.NoteStatus
import playaxis.appinn.note_it.utils.MainUtils
import java.text.NumberFormat
import javax.inject.Inject

class CreateEditLabelFragment : Fragment() {

    private lateinit var binding: FragmentLabelCreateEditBinding

    @Inject
    lateinit var viewModelFactory: LabelViewModel.Factory
    val viewModel by viewModel { viewModelFactory.create(it) }

    @Inject
    lateinit var viewModelFactoryEdit: LabelEditViewModel.Factory
    val viewModelEdit by viewModel { viewModelFactoryEdit.create(it) }

    private val viewModelHome: HomeViewModel by activityViewModels()
    private val viewModelMain: MainViewModel by activityViewModels()

    private val args: CreateEditLabelFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (requireContext().applicationContext as QuickNotepad).appComponent.inject(this)

        enterTransition = MaterialElevationScale(false).apply {
            duration = resources.getInteger(com.google.android.material.R.integer.material_motion_duration_short_2).toLong()
        }
        exitTransition = MaterialElevationScale(true).apply {
            duration = resources.getInteger(com.google.android.material.R.integer.material_motion_duration_short_2).toLong()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        binding = FragmentLabelCreateEditBinding.inflate(inflater, container, false)

        //getting all labels
        viewModel.start(args.noteIds.toList())
        viewModelEdit.start(args.labelId)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        MainUtils.statusBarColor(requireActivity(), ContextCompat.getColor(requireContext(), R.color.search_bar_background))

        Log.i( "labelScreen: ",args.noteIds.size.toString())
        if (args.noteIds.isNotEmpty()){

            binding.labelName.requestFocus()
            binding.createLabelButton.visibility = View.VISIBLE
            binding.addLabelLayout.visibility = View.INVISIBLE
            binding.progress.visibility = View.VISIBLE

            val rcv = binding.labelsList
            rcv.setHasFixedSize(true)
            val adapter = LabelAdapter(requireContext(), viewModel,editView = false)
            val layoutManager = LinearLayoutManager(context)
            rcv.adapter = adapter
            rcv.layoutManager = layoutManager

            setupViewModelObservers(adapter)
        }
        else {

            binding.labelName.requestFocus()
            binding.createLabelButton.visibility = View.VISIBLE
            binding.addLabelLayout.visibility = View.INVISIBLE
            binding.progress.visibility = View.VISIBLE

            val rcv = binding.labelsList
            rcv.setHasFixedSize(false)
            val adapter = LabelAdapter(requireContext(), viewModel,editView = true)
            val layoutManager = LinearLayoutManager(context)
            rcv.adapter = adapter
            rcv.layoutManager = layoutManager

            setupViewModelObservers(adapter)
        }

        binding.createLabelButton.setOnClickListener {

            binding.labelName.requestFocus()
            binding.createLabelButton.visibility = View.INVISIBLE
            binding.addLabelLayout.visibility = View.VISIBLE
            binding.progress.visibility = View.VISIBLE

            val rcv = binding.labelsList
            rcv.setHasFixedSize(true)
            val adapter = LabelAdapter(requireContext(), viewModel,editView = true)
            val layoutManager = LinearLayoutManager(context)
            rcv.adapter = adapter
            rcv.layoutManager = layoutManager

            setupViewModelObservers(adapter)
        }

        binding.removeLabel.setOnClickListener {
            binding.createLabelButton.visibility = View.VISIBLE
            binding.addLabelLayout.visibility = View.INVISIBLE
            binding.progress.visibility = View.VISIBLE

            val rcv = binding.labelsList
            rcv.setHasFixedSize(true)
            val adapter = LabelAdapter(requireContext(), viewModel,editView = false)
            val layoutManager = LinearLayoutManager(context)
            rcv.adapter = adapter
            rcv.layoutManager = layoutManager

            setupViewModelObservers(adapter)
        }

        //creating label
        binding.saveLabel.setOnClickListener {

            viewModelEdit.onNameChanged(binding.labelName.text.toString())
            viewModelEdit.addLabel()

            //clearing the editText
            binding.labelName.clearFocus()
            binding.labelName.setText("")

            val rcv = binding.labelsList
            rcv.setHasFixedSize(true)
            val adapter = LabelAdapter(requireContext(), viewModel,editView = false)
            val layoutManager = LinearLayoutManager(context)
            rcv.adapter = adapter
            rcv.layoutManager = layoutManager

            setupViewModelObservers(adapter)

            //save label
            viewModelHome.saveLabelEvent()
        }
    }

    private fun setupViewModelObservers(adapter: LabelAdapter) {
        viewModel.labelItems.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            binding.progress.visibility = View.GONE
        }

        viewModel.showDeleteConfirmEvent.observeEvent(viewLifecycleOwner) {
            showDeleteConfirmDialog()
        }

        viewModelHome.labelAddEventSelect.observeEvent(viewLifecycleOwner) { label ->
            viewModel.selectNewLabel(label)
        }

        viewModelEdit.labelAddEvent.observeEvent(this, viewModelHome::onLabelAdd)

        //adding the labels to the note
        viewModelHome.saveLabelEvent.observeEvent(viewLifecycleOwner){
            viewModel.setNotesLabels()
        }

        //the reminder will be called from home fragment or any other screen from where the reminder will be needed to be called
        viewModelHome.showReminderDialogEvent.observeEvent(viewLifecycleOwner) { noteIds ->
            //getting the controller
            val host = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_main_fragment_container) as NavHostFragment
            val navController = host.navController
            navController.setGraph(R.navigation.main_navigation)

            navController.navigateSafe(MainNavigationDirections.actionReminder(noteIds.toLongArray()))
        }
    }
    private fun showDeleteConfirmDialog() {
        ConfirmDialog.newInstance(
            title = R.string.action_delete_selection,
            message = R.string.label_delete_message,
            btnPositive = R.string.action_delete
        ).show(childFragmentManager, DELETE_CONFIRM_DIALOG_TAG)
    }

    override fun onDetach() {
        super.onDetach()

        val host = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_main_fragment_container) as NavHostFragment
        val navController = host.navController
        navController.setGraph(R.navigation.main_navigation)

        val currentFragId = navController.currentBackStackEntry!!.destination.id
        val currentFragment = navController.graph.findNode(currentFragId)

        if (currentFragment!!.label == "fragment_home"){
            viewModelMain.bottomFragmentItemDetachEventEvent()
            viewModelHome.clearSelection()
            viewModelHome.clear()
        }

        //set the destination also
        viewModelHome.setDestination(HomeDestination.Status(NoteStatus.ACTIVE))
    }

    companion object {
        private val NUMBER_FORMAT = NumberFormat.getInstance()

        private const val DELETE_CONFIRM_DIALOG_TAG = "delete_confirm_dialog"
    }
}