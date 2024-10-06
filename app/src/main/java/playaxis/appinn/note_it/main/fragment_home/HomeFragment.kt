package playaxis.appinn.note_it.main.fragment_home

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.tabs.TabLayout
import playaxis.appinn.note_it.HomeNavGraphDirections
import playaxis.appinn.note_it.R
import playaxis.appinn.note_it.application.QuickNotepad
import playaxis.appinn.note_it.databinding.FragmentHomeBinding
import playaxis.appinn.note_it.extensions.navigateSafe
import playaxis.appinn.note_it.main.fragment_home.fragments.folder.adapter.FolderListAdapter
import playaxis.appinn.note_it.main.utils.observeEvent
import playaxis.appinn.note_it.main.viewModels.MainViewModel
import playaxis.appinn.note_it.navigation.HomeDestination
import playaxis.appinn.note_it.repository.model.entities.FolderWithNotes
import playaxis.appinn.note_it.repository.model.entities.NoteStatus
import playaxis.appinn.note_it.utils.MainUtils

class HomeFragment : Fragment(), NavController.OnDestinationChangedListener,
    FolderListAdapter.FolderItemCLickEvent {

    private lateinit var binding: FragmentHomeBinding

    private val viewModelHome: HomeViewModel by activityViewModels()
    private val viewModelMain: MainViewModel by activityViewModels()

    private var REQUEST_CODE_STORAGE = 3
    private var typeNote = ""
    private var typeAudioNote = false

    // Create a dialog instance
    private lateinit var dialogPickerImage: Dialog
    private lateinit var navController: NavController
    private val PERMISSION_REQUEST_CODE = 1
    private val REQ_CODE_CAMERA = 100

    // Initialize the ActivityResultLauncher in your fragment
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->

        val host = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_main_fragment_container) as NavHostFragment
        val navController = host.navController
        navController.setGraph(R.navigation.main_navigation)

        if (result.resultCode == Activity.RESULT_OK) {
            // Get the URI of the selected image
            val data = result.data
            if (data != null) {
                Log.i("dataUriImage: ",data.data.toString())
                viewModelHome.imageNote.add(data.data.toString())

                //creating note for it
                viewModelHome.createNote()
                //fire the event to handle the view
                viewModelMain.bottomBarItemsListenerEvent("gallery_note")
                //navigate to list fragment
                navController.navigate(HomeFragmentDirections.homeToAddEditNoteFragment())
            }
            else
                viewModelHome.imageNote.clear()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        //injection initialization
        (QuickNotepad.appContext as QuickNotepad).appComponent.inject(this)

        val host = childFragmentManager.findFragmentById(R.id.nav_host_nested_fragments) as NavHostFragment
        navController = host.navController
        navController.setGraph(R.navigation.home_nav_graph)
        navController.addOnDestinationChangedListener(this)

        // Create a dialog instance
        dialogPickerImage = Dialog(requireContext())

        viewModelHome.adapter = FolderListAdapter(this)

        //setting the destination again
        viewModelHome.setDestination(HomeDestination.Status(NoteStatus.ACTIVE))

        return binding.root
    }

    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        //By default tab layout will be visible (i am making it visible here because on event its being made invisible)
        binding.tabsFragments.visibility = View.VISIBLE

        //default view
        viewModelMain.bottomFragmentItemDetachEventEvent()

        //bottombar listeners
        bottomBarListeners()

        val allNotesTab = binding.tabsFragments.newTab()
        allNotesTab.text = "All"
        val foldersTab = binding.tabsFragments.newTab()
        foldersTab.text = "Folders"
        binding.tabsFragments.addTab(allNotesTab)
        binding.tabsFragments.addTab(foldersTab)

        //selecting default tab
        binding.tabsFragments.getTabAt(0)!!.select()
        //removing both fragments from the stack if they exists
        navController.popBackStack()
        navController.popBackStack()
        navController.navigateSafe(HomeNavGraphDirections.toNoteListFragment())

        //tab select listener
        binding.tabsFragments.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {

                //updating view for account button
                viewModelMain.updateLayoutListButtonVisibility(View.VISIBLE)
                if (tab != null){
                    if (tab.position == 0){
                        viewModelMain.updateLayoutListButtonVisibility(View.VISIBLE)
                        //navigating
                        viewModelHome.tabSelected = true
                        navController.popBackStack()
                        navController.navigateSafe(HomeNavGraphDirections.toNoteListFragment())
                    }
                    else if (tab.position == 1){
                        viewModelMain.updateLayoutListButtonVisibility(View.GONE)
                        //navigating
                        viewModelHome.tabSelected = true
                        navController.popBackStack()
                        navController.navigateSafe(HomeNavGraphDirections.toFolderListFragment())
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                //updating view for account button
                viewModelMain.updateLayoutListButtonVisibility(View.VISIBLE)
                if (tab != null){
                    if (tab.position == 0){
                        viewModelMain.updateLayoutListButtonVisibility(View.VISIBLE)
                        //navigating
                        viewModelHome.tabSelected = true
                        navController.popBackStack()
                        navController.navigateSafe(HomeNavGraphDirections.toNoteListFragment())
                    }
                    else if (tab.position == 1){
                        viewModelMain.updateLayoutListButtonVisibility(View.GONE)
                        //navigating
                        viewModelHome.tabSelected = true
                        navController.popBackStack()
                        navController.navigateSafe(HomeNavGraphDirections.toFolderListFragment())
                    }
                }
            }
        })

        Log.i("homeFragmentsStackSizeBefore: ",navController.currentBackStack.value.size.toString())

        //back pressed
        viewModelMain.backPressDispatcherEvent.observeEvent(viewLifecycleOwner){ backPressed ->

            //selecting default tab
            if (binding.tabsFragments.selectedTabPosition == 1 && viewModelHome.tabSelected){
                binding.tabsFragments.getTabAt(0)!!.select()
                navController.popBackStack()
            }
            else{
                viewModelHome.closeApp()
            }
        }

        mainObserversHome()
    }

    private fun mainObserversHome(){

//        //it will set data in the list which will be passed to the main list adapter
//        viewModelMain.currentHomeDestination.observe(viewLifecycleOwner) { destination ->
//            viewModelHome.setDestination(destination)
//        }

        //create note event
        viewModelHome.createNoteEvent.observeEvent(viewLifecycleOwner) { settings ->

            val host = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_main_fragment_container) as NavHostFragment
            navController = host.navController
            navController.setGraph(R.navigation.main_navigation)

            if (typeNote == "list"){
                navController.navigateSafe(HomeFragmentDirections.homeToTodoFragment(
                    labelId = settings.labelId, changeReminder = settings.initialReminder))
            }
            else if (typeAudioNote){
                navController.navigateSafe(HomeFragmentDirections.homeToSpeechFragment(
                    labelId = settings.labelId, changeReminder = settings.initialReminder))
            }
            else if (viewModelHome.cameraNote){
                navController.navigateSafe(HomeFragmentDirections.actionHomeFragmentToCameraFragment(false))
            }
            else if (typeNote == "text"){
                navController.navigateSafe(HomeFragmentDirections.homeToAddEditNoteFragment(
                    labelId = settings.labelId, changeReminder = settings.initialReminder))
            }
        }

        //folder item click event
        viewModelHome.folderItemClickEvent.observeEvent(viewLifecycleOwner){ folderCLicked ->

            //this will be called when the item of the folder will be clicked
            navController.navigateSafe(HomeNavGraphDirections.toNoteListFragment())

            //create an event and send the folder clicked to the NoteListFragment
            viewModelHome.folderItemArgsNoteListEvent(folderCLicked)
        }

        //back-press event to handle the back stack of the home fragment container
        viewModelHome.homeFragmentsBackButtonTransactionEvent.observeEvent(viewLifecycleOwner){

            //make the view of home fragment
            navController.popBackStack()
            binding.tabsFragments.visibility = View.VISIBLE
        }

        //open gallery event
        viewModelMain.openGalleryEvent.observeEvent(viewLifecycleOwner){
            openGallery()
        }
    }

    override fun onResume() {
        super.onResume()

        var batteryRestricted = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Detect battery restriction as it affects reminder alarms.
            val activityManager = QuickNotepad.appContext.getSystemService(AppCompatActivity.ACTIVITY_SERVICE) as? ActivityManager
            if (activityManager?.isBackgroundRestricted == true) {
                batteryRestricted = true
            }
        }

        var notificationRestricted = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(QuickNotepad.appContext,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED
            ) {
                notificationRestricted = true
            }
        }

        var reminderRestricted = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE){
            val alarmManager: AlarmManager = QuickNotepad.appContext.getSystemService(AppCompatActivity.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()){
                Log.d("TAG","Crash" +alarmManager.canScheduleExactAlarms())
                reminderRestricted = true
            }
        }

        viewModelHome.updateRestrictions(batteryRestricted, notificationRestricted, reminderRestricted)
    }

    private fun bottomBarListeners() {

        //todoList
        binding.listButton.setOnClickListener {
            //telling the of note
            typeNote = "list"
            typeAudioNote = false
            viewModelHome.editing = false
            viewModelHome.cameraNote = false

            //creating note for it
            viewModelHome.createNote()
            //fire the event to handle the view
            viewModelMain.bottomBarItemsListenerEvent("list_note")
            //clearing the set image
            viewModelHome.imageNote.clear()
        }

        //draw
        binding.drawButton.setOnClickListener {

            val host = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_main_fragment_container) as NavHostFragment
            val navController = host.navController
            navController.setGraph(R.navigation.main_navigation)
            //telling the of note
            typeNote = "draw"
            typeAudioNote = false
            //writing identifier which will tell that its a new drawing
            viewModelHome.newDrawing = true
            viewModelHome.cameraNote = false

            //fire the event to handle the view
            viewModelMain.bottomBarItemsListenerEvent("draw_note")
            //navigate to list fragment
            //call the fragment using handler or coroutine
            requireActivity().runOnUiThread {
                navController.navigate(HomeFragmentDirections.homeToDrawFragment())
            }
            //clearing the set image
            viewModelHome.imageNote.clear()
        }

        //add edit note
        binding.addNoteButton.setOnClickListener {
            //telling the of note
            typeNote = "text"
            typeAudioNote = false
            viewModelHome.cameraNote = false

            viewModelHome.editing = false
            //creating note for it
            viewModelHome.createNote()
            //fire the event to handle the view
            viewModelMain.bottomBarItemsListenerEvent("add_note")
            //clearing the set image
            viewModelHome.imageNote.clear()
        }

        //speech
        binding.recordButton.setOnClickListener {
            //telling the of note
            typeNote = "speech"
            typeAudioNote = true
            viewModelHome.editing = false
            viewModelHome.cameraNote = false

            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_REQUEST_CODE)
            }
            else {
                //creating note for it
                viewModelHome.createNote()
                //fire the event to handle the view
                viewModelMain.bottomBarItemsListenerEvent("speech_note")
            }

            //clearing the set image
            viewModelHome.imageNote.clear()
        }

        //open gallery
        binding.imageButton.setOnClickListener {

            //telling the of note
            typeNote = "image"
            typeAudioNote = false
            viewModelHome.editing = false

            //show the dialog for opening camera or gallery
            dialogForImagePicking()
            //clearing the set image
            viewModelHome.imageNote.clear()
        }
    }

    private fun dialogForImagePicking(){

        // Set the content view to your custom layout
        dialogPickerImage.setContentView(R.layout.image_picker_dialog)
        dialogPickerImage.let {

            val windowManager = requireActivity().windowManager
            val display = windowManager.defaultDisplay
            val size = android.graphics.Point()
            display.getSize(size)
            val screenWidth = size.x
            val screenHeight = size.y

            val dialogWidth = (screenWidth * 1.0).toInt()
            val dialogHeight = (screenHeight * 0.4).toInt()

            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(dialogPickerImage.window?.attributes)
            layoutParams.width = dialogWidth
            layoutParams.height = dialogHeight
            dialogPickerImage.window?.attributes = layoutParams
            dialogPickerImage.window?.setGravity(Gravity.CENTER)
            dialogPickerImage.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialogPickerImage.setCancelable(true)
        }

        // Find views in the custom layout
        val camera: AppCompatTextView = dialogPickerImage.findViewById(R.id.camera_dialog_button)
        val gallery: AppCompatTextView = dialogPickerImage.findViewById(R.id.gallery_dialog_button)

        camera.setOnClickListener {

            //open camera with permission
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                //Permission granted
                //call camera fragment
                val host = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_main_fragment_container) as NavHostFragment
                val navController = host.navController
                navController.setGraph(R.navigation.main_navigation)

                viewModelHome.cameraNote = true
                //creating note for it
                viewModelHome.createNote()
                //send the event to normalize the toolbar
                viewModelHome.cameraToolbarNormalizeEvent()
                //navigation
                navController.navigateSafe(HomeFragmentDirections.actionHomeFragmentToCameraFragment(false))
            }
            else {
                // Request the permissions
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.CAMERA), REQ_CODE_CAMERA)
            }

            dialogPickerImage.dismiss()
        }

        gallery.setOnClickListener {

            viewModelHome.cameraNote = false

            //open gallery
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                    //opening the gallery
                    openGallery()
                }
                else {
                    // Request the permissions
                    ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.READ_MEDIA_IMAGES), REQUEST_CODE_STORAGE)
                }
            }
            else {

                if (MainUtils.hasStoragePermission(requireContext())) {
                    //opening the gallery
                    openGallery()
                }
                else {
                    //requesting the Gallery permission
                    MainUtils.getStoragePermission(requireActivity())
                }
            }

            dialogPickerImage.dismiss()
        }

        dialogPickerImage.show()
    }

    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(galleryIntent)
    }

    override fun onDetach() {
        super.onDetach()

        //setting selected tab false because then it will give issue in back press dispatcher
        viewModelHome.tabSelected = false
    }

    override fun onDestinationChanged(controller: NavController, destination: NavDestination, arguments: Bundle?) {}

    override fun folderClicked(folderNote: FolderWithNotes, position: Int) {}
}