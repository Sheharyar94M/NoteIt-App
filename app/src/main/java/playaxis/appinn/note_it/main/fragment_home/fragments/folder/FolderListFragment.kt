package playaxis.appinn.note_it.main.fragment_home.fragments.folder

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import playaxis.appinn.note_it.R
import playaxis.appinn.note_it.application.QuickNotepad
import playaxis.appinn.note_it.databinding.FragmentFolderListBinding
import playaxis.appinn.note_it.main.fragment_home.HomeViewModel
import playaxis.appinn.note_it.main.fragment_home.fragments.folder.adapter.FolderListAdapter
import playaxis.appinn.note_it.main.fragment_home.fragments.folder.helper.FolderSelectionItem
import playaxis.appinn.note_it.main.fragment_home.fragments.folder.viewmodels.FolderViewModel
import playaxis.appinn.note_it.main.utils.observeEvent
import playaxis.appinn.note_it.main.viewModels.viewModel
import playaxis.appinn.note_it.repository.model.entities.FolderNote
import playaxis.appinn.note_it.repository.model.entities.FolderWithNotes
import javax.inject.Inject

class FolderListFragment : Fragment(), FolderListAdapter.FolderItemCLickEvent {

    private lateinit var binding: FragmentFolderListBinding

    @Inject
    lateinit var viewModelFactoryFolder: FolderViewModel.Factory
    private val viewModel by viewModel { viewModelFactoryFolder.create(it) }
    private val viewModelHome: HomeViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentFolderListBinding.inflate(inflater, container, false)

        //injection initialization
        (QuickNotepad.appContext as QuickNotepad).appComponent.inject(this)

        viewModelHome.adapter = FolderListAdapter(this)
        viewModelHome.adapter.selectionFolder = false

        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //by default the button of add folder will be visible
        //making create folder button gone
        binding.addFolderButton.visibility = View.VISIBLE

        val rcv = binding.folderList
        rcv.setHasFixedSize(true)
        val layoutManager = GridLayoutManager(requireContext(), 2, GridLayoutManager.VERTICAL, false)
        rcv.adapter = viewModelHome.adapter
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

        //create a folder
        binding.addFolderButton.setOnClickListener {

            //creating a folder
            //display the dialog for it
            showCreateFolderDialog()
        }

        //setting the recyclerview of folder list
        viewModel.foldersLiveData.observe(viewLifecycleOwner) { folders ->

            if (folders != null) {

                val listFolders = ArrayList<FolderSelectionItem>()
                for (folder in folders) {
                    listFolders.add(FolderSelectionItem(folder))
                }
                viewModelHome.adapter.setFolderList(listFolders)
                viewModelHome.adapter.notifyDataSetChanged()
            }
        }

        observers()
    }

    private fun observers(){

        //for re-initializing the the list of folders for enabling folder items selection
        viewModelHome.folderSelectionListEvent.observeEvent(viewLifecycleOwner) {
            //initialize the adapter again the updated view of the recyclerview items
            initializeListAgain(selectionFolder = true)
            //making create folder button gone
            binding.addFolderButton.visibility = View.GONE
        }
    }

    private fun showCreateFolderDialog() {
        // Create a dialog instance
        val dialog = Dialog(requireContext())
        // Set the content view to your custom layout
        dialog.setContentView(R.layout.create_folder_dialog)

        dialog.let {

            val windowManager = requireActivity().windowManager
            val display = windowManager.defaultDisplay
            val size = android.graphics.Point()
            display.getSize(size)
            val screenWidth = size.x
            val screenHeight = size.y

            val dialogWidth = (screenWidth * 1.0).toInt()
            val dialogHeight = (screenHeight * 0.4).toInt()

            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(dialog.window?.attributes)
            layoutParams.width = dialogWidth
            layoutParams.height = dialogHeight
            dialog.window?.attributes = layoutParams
            dialog.window?.setGravity(Gravity.CENTER)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.setCancelable(false)
        }

        // Find views in the custom layout
        val closeButton: AppCompatTextView = dialog.findViewById(R.id.cancel_button)
        val saveButton: AppCompatTextView = dialog.findViewById(R.id.save_button)
        val folderName: AppCompatEditText = dialog.findViewById(R.id.folder_name)
        val alreadyExists: AppCompatTextView = dialog.findViewById(R.id.already_exists)

        // Set click listener for the close button
        closeButton.setOnClickListener {
            // Dismiss the dialog when the button is clicked
            dialog.dismiss()
        }

        // Set click listener for the save button
        saveButton.setOnClickListener {

            //creating the folder
            if (folderName.text.toString().isNotEmpty()) {

                //inserting the folder in database
                viewModel.foldersLiveData.observe(viewLifecycleOwner) { folders ->

                    for (folder in folders) {
                        if (folder.folder.name.equals(folderName.text.toString(),ignoreCase = false)) {
                            alreadyExists.visibility = View.VISIBLE
                            break
                        }
                        else {
                            alreadyExists.visibility = View.GONE
                        }
                    }

                    //create the folder
                    if (alreadyExists.visibility == View.GONE){
                        lifecycleScope.launch(Dispatchers.IO) {

                            viewModel.createFolder(FolderNote(name = folderName.text.toString()))

                            //Dismissing the dialog
                            requireActivity().runOnUiThread {
                                viewModel.setFoldersData()
                                dialog.dismiss()
                            }
                        }
                    }
                }
            } else {
                folderName.error = "Folder name!"
            }
        }

        // Show the dialog
        dialog.show()
    }

    private fun initializeListAgain(selectionFolder: Boolean) {

        viewModelHome.adapter = FolderListAdapter(this)
        viewModelHome.adapter.selectionFolder = selectionFolder

        binding.folderList.layoutManager = GridLayoutManager(requireContext(), 2, GridLayoutManager.VERTICAL, false)
        binding.folderList.setHasFixedSize(false)
        binding.folderList.adapter = viewModelHome.adapter
        binding.folderList.itemAnimator = object : DefaultItemAnimator() {
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
        viewModel.setFoldersData()
    }

    override fun folderClicked(folderNote: FolderWithNotes, position: Int) {

        //send the selected object to home fragment for moving
        if (viewModelHome.adapter.selectionFolder){

            Log.i( "notesWithFolderList: ",viewModelHome.selectedNotes.size.toString())

            //firstly, set the selected folder to the viewModel so that it items should be moved
            // or should be visible according to the folder selected
            viewModelHome.selectedFolder = folderNote

            viewModelHome.adapter.changeSelectionStatus(position)
            binding.folderList.scrollToPosition(position)
        }
        else{
            //call the fragment note list, so that selected folder notes should be visible
            viewModelHome.folderItemClickEvent(folderNote)
        }
    }
}