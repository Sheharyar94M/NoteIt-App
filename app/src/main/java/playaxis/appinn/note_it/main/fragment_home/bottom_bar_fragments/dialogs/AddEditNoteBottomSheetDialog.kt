package playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.dialogs

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import playaxis.appinn.note_it.MainNavigationDirections
import playaxis.appinn.note_it.R
import playaxis.appinn.note_it.databinding.BottomSheetFragmentLayoutBinding
import playaxis.appinn.note_it.extensions.navigateSafe
import playaxis.appinn.note_it.main.fragment_home.HomeViewModel
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.note.helper_model.BottomDialogItem
import playaxis.appinn.note_it.main.viewModels.MainViewModel
import playaxis.appinn.note_it.utils.MainUtils

class AddEditNoteBottomSheetDialog(
    private val bottomSheetDialogList: ArrayList<BottomDialogItem>,
    private val menu: Boolean,
    private val openGalleryEvent: OpenGalleryEvent,
    private val noteType: String
) : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetFragmentLayoutBinding
    private val viewModelHome: HomeViewModel by activityViewModels()
    private val viewModelMain: MainViewModel by activityViewModels()

    companion object{
        var REQUEST_CODE_STORAGE = 3
        val REQ_CODE_CAMERA = 100
    }

//    // Initialize the ActivityResultLauncher in your fragment
//    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
//
//        val host = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_main_fragment_container) as NavHostFragment
//        val navController = host.navController
//        navController.setGraph(R.navigation.main_navigation)
//
//        if (result.resultCode == Activity.RESULT_OK) {
//            // Get the URI of the selected image
//            val data = result.data
//            if (data != null) {
//                Log.i("dataUriImage: ",data.data.toString())
//
//                //fire the event to handle the view
//                viewModelMain.bottomBarItemsListenerEvent("gallery_note")
//                viewModelHome.loadImageFromBottomDialogEvent(data.data)
//                dismiss()
//            }
//            else
//                viewModelHome.imageNote.clear()
//        }
//    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)

        binding = BottomSheetFragmentLayoutBinding.inflate(inflater, container, false)

        //setting values to the views
        if (menu){
            //making visible
            binding.item4.visibility = View.VISIBLE
            binding.item5.visibility = View.VISIBLE

            binding.itemIcon1.setImageDrawable(bottomSheetDialogList[0].icon)
            binding.itemText1.text = bottomSheetDialogList[0].text
            binding.itemIcon2.setImageDrawable(bottomSheetDialogList[1].icon)
            binding.itemText2.text = bottomSheetDialogList[1].text
            binding.itemIcon3.setImageDrawable(bottomSheetDialogList[2].icon)
            binding.itemText3.text = bottomSheetDialogList[2].text
            binding.itemIcon4.setImageDrawable(bottomSheetDialogList[3].icon)
            binding.itemText4.text = bottomSheetDialogList[3].text
            binding.itemIcon5.setImageDrawable(bottomSheetDialogList[4].icon)
            binding.itemText5.text = bottomSheetDialogList[4].text
        }
        else{
            binding.itemIcon1.setImageDrawable(bottomSheetDialogList[0].icon)
            binding.itemText1.text = bottomSheetDialogList[0].text
            binding.itemIcon2.setImageDrawable(bottomSheetDialogList[1].icon)
            binding.itemText2.text = bottomSheetDialogList[1].text
            binding.itemIcon3.setImageDrawable(bottomSheetDialogList[2].icon)
            binding.itemText3.text = bottomSheetDialogList[2].text

            //making invisible
            binding.item4.visibility = View.GONE
            binding.item5.visibility = View.GONE
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //listeners
        listeners()
    }

    private fun listeners() {

        binding.item1.setOnClickListener {

            if (menu) {
                //delete perform
                viewModelHome.deleteSelectedNotesPre()

                val host = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_main_fragment_container) as NavHostFragment
                val navController = host.navController
                navController.setGraph(R.navigation.main_navigation)
                navController.popBackStack()
            }
            else {

                //call the scanner fragment to scan the image and make a note of it
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    //ask for permission
                    ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.CAMERA), REQ_CODE_CAMERA)
                }
                else {

                    viewModelHome.openGalleryEventVal = false

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {

                            //take photo navigate to camera screen
                            val host = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_main_fragment_container) as NavHostFragment
                            val navController = host.navController
                            navController.setGraph(R.navigation.main_navigation)

                            //send the event to normalize the toolbar
                            viewModelHome.cameraToolbarNormalizeEvent()
                            //navigation
                            navController.navigateSafe(MainNavigationDirections.mainToCameraFragment(false))
                        }
                        else {
                            // Request the permissions
                            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.READ_MEDIA_IMAGES), REQUEST_CODE_STORAGE)
                        }
                    }
                    else {

                        if (MainUtils.hasStoragePermission(requireContext())) {
                            //take photo navigate to camera screen
                            val host = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_main_fragment_container) as NavHostFragment
                            val navController = host.navController
                            navController.setGraph(R.navigation.main_navigation)

                            //send the event to normalize the toolbar
                            viewModelHome.cameraToolbarNormalizeEvent()
                            //navigation
                            navController.navigateSafe(MainNavigationDirections.mainToCameraFragment(false))
                        }
                        else {
                            //requesting the Gallery permission
                            MainUtils.getStoragePermission(requireActivity())
                        }
                    }
                }
            }
            //dismiss the dialog
            dismissNow()
        }

        binding.item2.setOnClickListener {

            if (menu) {
                //Make a copy
                viewModelHome.copySelectedNote("Copy","")
            }
            else {

                viewModelHome.openGalleryEventVal = true

                //add image
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                        //opening the gallery
                        openGalleryEvent.openGalleryFromDialog()
                    }
                    else {
                        // Request the permissions
                        ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.READ_MEDIA_IMAGES), REQUEST_CODE_STORAGE)
                    }
                }
                else {

                    if (MainUtils.hasStoragePermission(requireContext())) {
                        //opening the gallery
                        openGalleryEvent.openGalleryFromDialog()
                    }
                    else {
                        //requesting the Gallery permission
                        MainUtils.getStoragePermission(requireActivity())
                    }
                }
            }
            //dismiss the dialog
            dismissNow()
        }

        binding.item3.setOnClickListener {

            if (menu) {

                //send
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "")
                    type = "text/plain"
                }
                val chooser = Intent.createChooser(shareIntent, "Share Note")
                startActivity(chooser)
            }
            else {

                //drawing
                val host = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_main_fragment_container) as NavHostFragment
                val navController = host.navController
                navController.setGraph(R.navigation.main_navigation)
                //navigation
                navController.navigateSafe(MainNavigationDirections.mainToDrawFragment())
                //fire the event to handle the view
                viewModelMain.bottomBarItemsListenerEvent("draw_note")
            }
            //dismiss the dialog
            dismissNow()
        }

        binding.item4.setOnClickListener {

            if (menu) {

                //label
                val host = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_main_fragment_container) as NavHostFragment
                val navController = host.navController
                navController.setGraph(R.navigation.main_navigation)
                //navigation
                val selectedNotesIds = viewModelHome.selectedNotes.map { it.id }.toLongArray()
                navController.navigateSafe(MainNavigationDirections.mainToCreateEditLabelFragment(selectedNotesIds))
            }
//            else {
//
////                //recording
////                val host = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_main_fragment_container) as NavHostFragment
////                val navController = host.navController
////                navController.setGraph(R.navigation.main_navigation)
////                //navigation
////                navController.navigateSafe(AddEditFragmentDirections.actionAddEditFragmentToSpeechFragment())
//            }
            //dismiss the dialog
            dismissNow()
        }

        binding.item5.setOnClickListener {

            if (menu) {
                /**move to folder**/
                viewModelMain.noteImageNotClicked = false
                viewModelMain.moveFolderNote = true
                //save the data in the instance of viewModel
                viewModelHome.saveInstanceOfNoteEvent()
                //normalize the toolbar here according to the view of selection
                viewModelHome.moveToFolderToolbarNormalizeEvent()
                //now send the event to navigate
                viewModelHome.setMoveToFolderSelectionEvent(noteType)
            }
//            else {
//
//                //move to TodoList
////                //creating note for it
////                viewModelHome.createNote()
////                //send the event to the AddEditFragment to toggle the type of note, So, that note should be saved as list note or todoList
////                viewModelHome.shiftToTodoListEvent()
////                //fire the event to handle the view
////                viewModelMain.bottomBarItemsListenerEvent("list_note")
//            }
            //dismiss the dialog
            dismissNow()
        }
    }

    interface OpenGalleryEvent{
        fun openGalleryFromDialog()
    }
}