package playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.speech

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Path.Direction
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import playaxis.appinn.note_it.MainNavigationDirections
import playaxis.appinn.note_it.R
import playaxis.appinn.note_it.application.QuickNotepad
import playaxis.appinn.note_it.databinding.FragmentSpeechBinding
import playaxis.appinn.note_it.extensions.hideKeyboard
import playaxis.appinn.note_it.extensions.navigateSafe
import playaxis.appinn.note_it.main.fragment_home.HomeViewModel
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.dialogs.AddEditNoteBottomSheetDialog
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.dialogs.ColorPaletteDialog
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.dialogs.interfaces.ColorSelectedInterface
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.dialogs.interfaces.GradientSelectedInterface
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.dialogs.interfaces.WallpaperSelectedInterface
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.draw.canvas.DrawingView
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.note.AddEditFragmentDirections
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.note.adapters.NoteImageListAdapter
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.note.helper_model.BottomDialogItem
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.note.viewmodels.AddEditNoteViewModel
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.speech.adapters.SpeechListAdapter
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.speech.helper.Speech
import playaxis.appinn.note_it.main.utils.observeEvent
import playaxis.appinn.note_it.main.viewModels.MainViewModel
import playaxis.appinn.note_it.main.viewModels.viewModel
import playaxis.appinn.note_it.navigation.HomeDestination
import playaxis.appinn.note_it.preferences.SharedPreference
import playaxis.appinn.note_it.repository.model.entities.ColorNote
import playaxis.appinn.note_it.repository.model.entities.GradientNote
import playaxis.appinn.note_it.repository.model.entities.NoteStatus
import playaxis.appinn.note_it.repository.model.entities.NoteTextFormat
import playaxis.appinn.note_it.repository.model.entities.NoteType
import playaxis.appinn.note_it.utils.MainUtils
import java.io.File
import javax.inject.Inject

class SpeechFragment : Fragment(),
    ColorSelectedInterface, GradientSelectedInterface,
    WallpaperSelectedInterface, ColorPaletteDialog.CloseDialogEvent,
    SpeechListAdapter.DeleteItemEvent, AddEditNoteBottomSheetDialog.OpenGalleryEvent,
    NoteImageListAdapter.NoteImageClickedEvent {

    private lateinit var binding: FragmentSpeechBinding
    private lateinit var colorPaletteDialog: ColorPaletteDialog
    private lateinit var bottomSheetDialogList: ArrayList<BottomDialogItem>
    private lateinit var adapter: SpeechListAdapter

    //viewModels
    @Inject
    lateinit var viewModelFactory: SpeechViewModel.Factory
    private val viewModel by viewModel { viewModelFactory.create(it) }

    @Inject
    lateinit var viewModelFactoryAddNote: AddEditNoteViewModel.Factory
    private val viewModelNote by viewModel { viewModelFactoryAddNote.create(it) }

    private val viewModelHome by activityViewModels<HomeViewModel>()
    private val viewModelMain: MainViewModel by activityViewModels()

    private var selectedBackground = ""
    private var saveBackgroundColor = false
    private val PERMISSION_REQUEST_CODE = 1
    private var REQUEST_CODE_STORAGE = 3

    private lateinit var adapterImages: NoteImageListAdapter
    private var drawing = ArrayList<DrawingView.CustomPath>()
    private var editNote: Boolean = false

    private val handler = Handler(Looper.getMainLooper())
    private var isRecording = false

    private val longPressRunnable = Runnable {
        isRecording = true

        //this means the user is holding the button
        //Start recording
        binding.micButton.playAnimation()
        binding.listeningText.visibility = View.VISIBLE

        // Permission has already been granted, start recording but set the file path first
        val internalDir: File = QuickNotepad.appContext.filesDir // or use getCacheDir() for cache directory
        val outputFile = File(internalDir, "audio_record${System.currentTimeMillis()}.mp3")
        viewModelHome.setAudioFile(outputFile)
        viewModelHome.startRecording()
    }

    //recognizedText
    private val args: SpeechFragmentArgs by navArgs()

    // Initialize the ActivityResultLauncher in your fragment
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->

        val host = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_main_fragment_container) as NavHostFragment
        val navController = host.navController
        navController.setGraph(R.navigation.main_navigation)

        if (result.resultCode == Activity.RESULT_OK) {
            // Get the URI of the selected image
            val data = result.data
            if (data != null) {
                Log.i("dataUriImage: ", data.data.toString())
                //add the image to the list here
                viewModelHome.imageNote.add(data.data.toString())

                binding.noteImageList.visibility = View.VISIBLE
                //recyclerview
                val rcv = binding.noteImageList
                rcv.setHasFixedSize(true)
                val adapter = adapterImages
                if (viewModelHome.imageNote.size == 1){
                    val layoutManager = object : StaggeredGridLayoutManager(1, VERTICAL) {
                        override fun canScrollVertically() = false
                        override fun canScrollHorizontally() = false
                    }

                    rcv.adapter = adapter
                    rcv.layoutManager = layoutManager
                    //setting the list
                    adapterImages.setImagesList(viewModelHome.imageNote)
                }
                else if (viewModelHome.imageNote.size < 3){

                    val layoutManager = object : StaggeredGridLayoutManager(2, VERTICAL) {
                        override fun canScrollVertically() = false
                        override fun canScrollHorizontally() = false
                    }

                    rcv.adapter = adapter
                    rcv.layoutManager = layoutManager
                    //setting the list
                    adapterImages.setImagesList(viewModelHome.imageNote)
                }
                else{

                    val layoutManager = object : StaggeredGridLayoutManager(3, VERTICAL) {
                        override fun canScrollVertically() = false
                        override fun canScrollHorizontally() = false
                    }

                    rcv.adapter = adapter
                    rcv.layoutManager = layoutManager
                    //setting the list
                    adapterImages.setImagesList(viewModelHome.imageNote)
                }
            }
            else
                viewModelHome.imageNote.clear()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        binding = FragmentSpeechBinding.inflate(inflater, container, false)

        //injection initialization
        (QuickNotepad.appContext as QuickNotepad).appComponent.inject(this)

        //init lists
        bottomSheetDialogList = ArrayList()

        colorPaletteDialog = ColorPaletteDialog(this, this, this, this)
        MainUtils.statusBarColor(requireActivity(), ContextCompat.getColor(requireContext(), R.color.search_bar_background))

        adapter = SpeechListAdapter(this)
        adapterImages = NoteImageListAdapter(this)

        /**Over here we are setting the data to the list and liveData**/
        if (args.noteId != 0L && viewModelHome.editing){

            editNote = true
            //setting the change selection true for the new note
            viewModelHome.changeSelection = true

            //Getting the clicked note
            val noteClicked = viewModelHome.getClickedNote()

            //setting the title first
            binding.titleEditText.setText(noteClicked.title)

            //setting the selected color
            if (noteClicked.colorNote != null) {
                if (noteClicked.colorNote.color.isNotEmpty()){
                    if (noteClicked.colorNote.color.length in 1..9) {
                        //this means there is color in the background of the note item
                        binding.rootLayoutSpeech.setBackgroundColor(noteClicked.colorNote.color.toInt())
                    }
                    else {
                        //this means there is image in the background of the note item
                        binding.rootLayoutSpeech.background = MainUtils.stringToDrawable(noteClicked.colorNote.color)
                    }
                }
            }

            val urisList = noteClicked.noteAudios.split(" ").filter { it.isNotEmpty() }
            //set the speech list in viewModel
            val listAudioNotes = ArrayList<Speech>()
            for (uri in urisList)
                listAudioNotes.add(Speech(args.noteId,uri))
            viewModel.setAudioList(listAudioNotes)

            //set the image list in viewModel
            viewModelHome.imageNote = noteClicked.noteImages as ArrayList<String>

            //set the recyclerview for the image list (this will give the list of the image items in the note)
            if (viewModelHome.imageNote.isNotEmpty()){

                binding.noteImageList.visibility = View.VISIBLE
                //recyclerview
                val rcv = binding.noteImageList
                rcv.setHasFixedSize(true)
                val adapter = adapterImages
                if (viewModelHome.imageNote.size == 1){
                    val layoutManager = object : StaggeredGridLayoutManager(1, VERTICAL) {
                        override fun canScrollVertically() = false
                        override fun canScrollHorizontally() = false
                    }

                    rcv.adapter = adapter
                    rcv.layoutManager = layoutManager
                    //setting the list
                    adapterImages.setImagesList(viewModelHome.imageNote)
                }
                else if (viewModelHome.imageNote.size < 3){

                    val layoutManager = object : StaggeredGridLayoutManager(2, VERTICAL) {
                        override fun canScrollVertically() = false
                        override fun canScrollHorizontally() = false
                    }

                    rcv.adapter = adapter
                    rcv.layoutManager = layoutManager
                    //setting the list
                    adapterImages.setImagesList(viewModelHome.imageNote)
                }
                else{

                    val layoutManager = object : StaggeredGridLayoutManager(3, VERTICAL) {
                        override fun canScrollVertically() = false
                        override fun canScrollHorizontally() = false
                    }

                    rcv.adapter = adapter
                    rcv.layoutManager = layoutManager
                    //setting the list
                    adapterImages.setImagesList(viewModelHome.imageNote)
                }
            }
            else
                binding.noteImageList.visibility = View.INVISIBLE
        }
        else{

            editNote = false
            //setting the change selection true for the new note
            viewModelHome.changeSelection = true
            //set the recyclerview for the image list (this will give the list of the image items in the note)
            if (viewModelHome.imageNote.isNotEmpty()){

                binding.noteImageList.visibility = View.VISIBLE
                //recyclerview
                val rcv = binding.noteImageList
                rcv.setHasFixedSize(true)
                val adapter = adapterImages
                if (viewModelHome.imageNote.size == 1){
                    val layoutManager = object : StaggeredGridLayoutManager(1, VERTICAL) {
                        override fun canScrollVertically() = false
                        override fun canScrollHorizontally() = false
                    }

                    rcv.adapter = adapter
                    rcv.layoutManager = layoutManager
                    //setting the list
                    adapterImages.setImagesList(viewModelHome.imageNote)
                }
                else if (viewModelHome.imageNote.size < 3){

                    val layoutManager = object : StaggeredGridLayoutManager(2, VERTICAL) {
                        override fun canScrollVertically() = false
                        override fun canScrollHorizontally() = false
                    }

                    rcv.adapter = adapter
                    rcv.layoutManager = layoutManager
                    //setting the list
                    adapterImages.setImagesList(viewModelHome.imageNote)
                }
                else{

                    val layoutManager = object : StaggeredGridLayoutManager(3, VERTICAL) {
                        override fun canScrollVertically() = false
                        override fun canScrollHorizontally() = false
                    }

                    rcv.adapter = adapter
                    rcv.layoutManager = layoutManager
                    //setting the list
                    adapterImages.setImagesList(viewModelHome.imageNote)
                }
            }
            else
                binding.noteImageList.visibility = View.INVISIBLE
        }

        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility", "NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //setting keyboard view
        normalizeKeyBoardView()
        keyBoardListeners()

        //init
        viewModelNote.start(
            args.noteId,
            args.labelId,
            args.changeReminder,
            NoteType.TEXT,
            args.title,
            args.content
        )

        if (viewModelMain.moveFolderNote){
            //setting values from viewModel
            setValuesSavedInitially()
            //setting identifier
            viewModelMain.moveFolderNote = false
        }

        //recyclerview
        val rcv = binding.voiceNoteList
        rcv.setHasFixedSize(true)
        val adapter = SpeechListAdapter(this)
        val layoutManager = object : LinearLayoutManager(requireContext(), VERTICAL,false) {
            override fun canScrollVertically() = false
            override fun canScrollHorizontally() = false
        }
        rcv.adapter = adapter
        rcv.layoutManager = layoutManager
        rcv.itemAnimator = object : DefaultItemAnimator() {
            override fun animateAppearance(viewHolder: RecyclerView.ViewHolder, preLayoutInfo: ItemHolderInfo?, postLayoutInfo: ItemHolderInfo): Boolean {

                return if (preLayoutInfo != null && (preLayoutInfo.left != postLayoutInfo.left || preLayoutInfo.top != postLayoutInfo.top)) {
                    // item move, handle normally
                    super.animateAppearance(viewHolder, preLayoutInfo, postLayoutInfo)
                }
                else {
                    // do not animate new item appearance
                    // this is mainly to avoid animating the whole list when fragment view is recreated.
                    dispatchAddFinished(viewHolder)
                    false
                }
            }
        }

        observers(adapter)

        //add new entries to the existing list in the viewModel
        binding.micButton.setOnTouchListener{ _,event ->

            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_REQUEST_CODE)
                false
            }
            else {

                //So, record the audio and add the item in the existing list
                when(event?.action){

                    MotionEvent.ACTION_DOWN->{
                        handler.postDelayed(longPressRunnable, 400)
                        false
                    }
                    MotionEvent.ACTION_UP->{

                        handler.removeCallbacks(longPressRunnable)
                        if (isRecording){

                            //this means user has lifted the button
                            binding.micButton.pauseAnimation()
                            binding.micButton.cancelAnimation()
                            binding.micButton.progress = 0f
                            binding.listeningText.visibility = View.GONE
                            //stop recording
                            val outputAudio = viewModelHome.stopRecording()
                            //we got the audio created by the user and now add it to the existing list and update the liveData
                            viewModel.addAudioItem(Speech(args.noteId,outputAudio))

                            //scroll to the bottom
                            binding.scrollview.fullScroll(ScrollView.FOCUS_DOWN)
                            //recording resetting identifier
                            isRecording = false
                        }
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun setValuesSavedInitially(){

        //save the list to the note view model
        viewModelNote.noteAudios = viewModelHome.noteAudios.joinToString(separator = " ")

        //set the list and adapter also
        viewModel.setAudioList(viewModelHome.noteAudios)

        val rcv = binding.voiceNoteList
        rcv.setHasFixedSize(true)
        val adapter = SpeechListAdapter(this)
        val layoutManager = object : LinearLayoutManager(requireContext(), VERTICAL,false) {
            override fun canScrollVertically() = false
            override fun canScrollHorizontally() = false
        }
        rcv.adapter = adapter
        rcv.layoutManager = layoutManager
        rcv.itemAnimator = object : DefaultItemAnimator() {
            override fun animateAppearance(viewHolder: RecyclerView.ViewHolder, preLayoutInfo: ItemHolderInfo?, postLayoutInfo: ItemHolderInfo): Boolean {

                return if (preLayoutInfo != null && (preLayoutInfo.left != postLayoutInfo.left || preLayoutInfo.top != postLayoutInfo.top)) {
                    // item move, handle normally
                    super.animateAppearance(viewHolder, preLayoutInfo, postLayoutInfo)
                }
                else {
                    // do not animate new item appearance
                    // this is mainly to avoid animating the whole list when fragment view is recreated.
                    dispatchAddFinished(viewHolder)
                    false
                }
            }
        }

        adapter.setSpeechList(viewModelHome.noteAudios)

        //setting pinning value
        if (viewModelHome.selectedNotes.isNotEmpty()){
            val clickedNote = viewModelHome.getClickedNote()
            viewModelNote.pinned = clickedNote.pinned
            viewModelNote.locked = clickedNote.lock
            viewModelNote.status = clickedNote.status
        }

        //saving selected color to the note object
        viewModelNote.colorNote = viewModelHome.colorNote

        //get the data from view
        if (viewModelHome.title != null)
            binding.titleEditText.setText(viewModelHome.title!!)

        //save the image from home viewModel in the AddEditViewModel to add the image to the database
        //saving the image in viewModel
        if (viewModelHome.imageNote.isNotEmpty())
            viewModelNote.noteImage = viewModelHome.imageNote

        Log.i("titleEditText: ", binding.titleEditText.text.toString())
        Log.i("noteImageViewModel: ", viewModelNote.noteImage.toString())

        if (viewModelHome.selectedFolder != null)
            viewModelNote.folderId = viewModelHome.selectedFolder!!.folder.id
    }

    private fun saveValuesInitially() {

        viewModel.getAudiosList().observe(viewLifecycleOwner){ audiosList ->

            if (audiosList.isNotEmpty()){

                //now concat the uris in string and put in viewModel to save the audio note
                viewModelHome.noteAudios = audiosList

                //setting pinning value
                if (viewModelHome.selectedNotes.isNotEmpty()){
                    val clickedNote = viewModelHome.getClickedNote()
                    viewModelHome.pinned = clickedNote.pinned
                    viewModelHome.locked = clickedNote.lock
                    viewModelHome.status = clickedNote.status
                }

                //saving selected color to the note object
                if (saveBackgroundColor)
                    viewModelHome.colorNote = ColorNote(selectedBackground)
                else if (SharedPreference.selectedBackground != null && saveBackgroundColor)
                    viewModelHome.colorNote = ColorNote(SharedPreference.selectedBackground!!)

                //get the data from view
                viewModelHome.title = binding.titleEditText.text.toString()

                //save the image from home viewModel in the AddEditViewModel to add the image to the database
                //saving the image in viewModel
                if (viewModelHome.imageNote.isNotEmpty())
                    viewModelHome.imageNote = viewModelHome.imageNote

                Log.i("titleEditText: ", binding.titleEditText.text.toString())
                Log.i("noteImageViewModel: ", viewModelNote.noteImage.toString())

                if (viewModelHome.selectedFolder != null)
                    viewModelHome.folderId = viewModelHome.selectedFolder!!.folder.id
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun observers(adapter: SpeechListAdapter) {

        //observing the list of speeches here
        viewModel.getAudiosList().observe(viewLifecycleOwner){ audios ->

            val rcv = binding.voiceNoteList
            rcv.setHasFixedSize(true)
            val layoutManager = object : LinearLayoutManager(requireContext(), VERTICAL,false) {
                override fun canScrollVertically() = false
                override fun canScrollHorizontally() = false
            }
            rcv.adapter = adapter
            rcv.layoutManager = layoutManager
            rcv.itemAnimator = object : DefaultItemAnimator() {
                override fun animateAppearance(viewHolder: RecyclerView.ViewHolder, preLayoutInfo: ItemHolderInfo?, postLayoutInfo: ItemHolderInfo): Boolean {

                    return if (preLayoutInfo != null && (preLayoutInfo.left != postLayoutInfo.left || preLayoutInfo.top != postLayoutInfo.top)) {
                        // item move, handle normally
                        super.animateAppearance(viewHolder, preLayoutInfo, postLayoutInfo)
                    }
                    else {
                        // do not animate new item appearance
                        // this is mainly to avoid animating the whole list when fragment view is recreated.
                        dispatchAddFinished(viewHolder)
                        false
                    }
                }
            }

            //set the list to the adapter
            adapter.setSpeechList(audios)
            adapter.notifyDataSetChanged()
        }

        viewModelMain.saveNoteOnBackPressEvent.observeEvent(viewLifecycleOwner){

            viewModel.getAudiosList().observe(viewLifecycleOwner){ audiosList ->

                if (audiosList.isNotEmpty()){

                    //Now, if this runs then this means that there are some audios recorded
                    //So, then prepare the list
                    val urisAudio = ArrayList<String>()
                    for (audio in audiosList)
                        urisAudio.add(audio.audio)

                    //now concat the uris in string and put in viewModel to save the audio note
                    viewModelNote.noteAudios = urisAudio.joinToString(separator = " ")

                    //setting pinning value
                    if (viewModelHome.selectedNotes.isNotEmpty()){
                        val clickedNote = viewModelHome.getClickedNote()
                        viewModelNote.pinned = clickedNote.pinned
                        viewModelNote.locked = clickedNote.lock
                        viewModelNote.status = clickedNote.status
                    }

                    //saving selected color to the note object
                    if (saveBackgroundColor)
                        viewModelNote.colorNote = ColorNote(selectedBackground)
                    else if (SharedPreference.selectedBackground != null && saveBackgroundColor)
                        viewModelNote.colorNote = ColorNote(SharedPreference.selectedBackground!!)

                    //get the data from view
                    viewModelNote.title = binding.titleEditText.text.toString()

                    //save the image from home viewModel in the AddEditViewModel to add the image to the database
                    //saving the image in viewModel
                    if (viewModelHome.imageNote.isNotEmpty())
                        viewModelNote.noteImage = viewModelHome.imageNote

                    Log.i("titleEditText: ", binding.titleEditText.text.toString())
                    Log.i("noteImageViewModel: ", viewModelNote.noteImage.toString())

                    if (viewModelHome.selectedFolder != null)
                        viewModelNote.folderId = viewModelHome.selectedFolder!!.folder.id

                    //save the data
                    requireView().hideKeyboard()
                    viewModelNote.saveNote()
                    //set the destination so that data could be updated
                    viewModelHome.setDestination(HomeDestination.Status(NoteStatus.ACTIVE))
                    viewModelHome.cameraNote = false

                    Handler().postDelayed({

                        viewModelNote.exit()
                        viewModelHome.selectedNoteIds.clear()
                        viewModelHome.clearSelection()
                    }, 280)
                }
            }
        }

        //the reminder will be called from home fragment or any other screen from where the reminder will be needed to be called
        viewModelHome.showReminderDialogEvent.observeEvent(viewLifecycleOwner) { noteIds ->
            //getting the controller
            val host = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_main_fragment_container) as NavHostFragment
            val navController = host.navController
            navController.setGraph(R.navigation.main_navigation)

            navController.navigateSafe(MainNavigationDirections.actionReminder(noteIds.toLongArray()))
        }

        viewModelHome.statusChangeEvent.observeEvent(viewLifecycleOwner) { statusChange ->
            viewModelHome.onStatusChange(statusChange)
        }

        //new note event
        viewModelNote.newNoteEvent.observeEvent(viewLifecycleOwner){ newNote ->
            //add new note to the selection
            viewModelHome.addNewNoteToSelection(newNote)
        }

        //displaying the data if it is saved
        viewModelHome.saveInstanceOfNoteEvent.observeEvent(viewLifecycleOwner){
            //display the content, images and title if they are present in viewModel
            saveValuesInitially()
        }
    }

    private fun normalizeKeyBoardView() {

        binding.keyboardView.scannerKeyboardViewButton.visibility = View.GONE
        binding.keyboardView.undoRedo.visibility = View.GONE
        binding.keyboardView.formatKeyboardViewButton.visibility = View.GONE
        binding.keyboardView.scannerKeyboardViewButton.visibility = View.GONE
    }

    private fun keyBoardListeners() {

        binding.keyboardView.addItemsKeyboardViewButton.setOnClickListener {

            bottomSheetDialogList = ArrayList()

            bottomSheetDialogList.add(BottomDialogItem(ContextCompat.getDrawable(requireContext(), R.drawable.camera_icon)!!, getString(R.string.take_photo)))
            bottomSheetDialogList.add(
                BottomDialogItem(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.add_photo_icon
                    )!!, getString(R.string.add_image)
                )
            )
            bottomSheetDialogList.add(
                BottomDialogItem(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.draw_icon_
                    )!!, getString(R.string.drawing)
                )
            )
            bottomSheetDialogList.add(
                BottomDialogItem(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.mike_button_
                    )!!, getString(R.string.recording)
                )
            )
            bottomSheetDialogList.add(
                BottomDialogItem(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.check_box
                    )!!, getString(R.string.check_boxes)
                )
            )


            //bottom sheet dialog add items
            val addEditNoteBottomSheetDialog = AddEditNoteBottomSheetDialog(bottomSheetDialogList, false,this, "speech")
            addEditNoteBottomSheetDialog.isCancelable = true
            addEditNoteBottomSheetDialog.show(
                requireActivity().supportFragmentManager, addEditNoteBottomSheetDialog.tag)
        }

        binding.keyboardView.colorKeyboardViewButton.setOnClickListener {

            //color select palette
            colorPaletteDialog.isCancelable = false
            colorPaletteDialog.show(
                requireActivity().supportFragmentManager,
                colorPaletteDialog.tag
            )
        }

        binding.keyboardView.menuKeyboardViewButton.setOnClickListener {

            bottomSheetDialogList = ArrayList()

            bottomSheetDialogList.add(
                BottomDialogItem(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.delete_icon_
                    )!!, "Delete"
                )
            )
            bottomSheetDialogList.add(
                BottomDialogItem(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.copy_icon
                    )!!, "Make a copy"
                )
            )
            bottomSheetDialogList.add(
                BottomDialogItem(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.share_icon
                    )!!, "Send"
                )
            )
            bottomSheetDialogList.add(
                BottomDialogItem(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.label_icon_white
                    )!!, "Labels"
                )
            )
            bottomSheetDialogList.add(
                BottomDialogItem(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.drive_file_move
                    )!!, "Move to folder"
                )
            )

            //bottom sheet dialog add items
            val addEditNoteBottomSheetDialog = AddEditNoteBottomSheetDialog(bottomSheetDialogList, true,this, "speech")
            addEditNoteBottomSheetDialog.isCancelable = true
            addEditNoteBottomSheetDialog.show(requireActivity()
                .supportFragmentManager, addEditNoteBottomSheetDialog.tag)
        }
    }

    override fun selectedColor(color: Int) {

        //setting the selected color
        binding.rootLayoutSpeech.setBackgroundColor(color)

        //save the gradient to preference
        SharedPreference.selectedBackground = color.toString()
        selectedBackground = color.toString()
    }

    override fun selectedGradient(gradient: GradientNote?) {

        //set the gradient color
        if (gradient != null) {

            val gradientColors = intArrayOf(
                gradient.color1,  // Start color
                gradient.color2   // End color
            )
            val gradientDrawable = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, gradientColors)
            binding.rootLayoutSpeech.background = gradientDrawable

            //save the gradient to preference

            SharedPreference.selectedBackground = MainUtils.bitmapToString(MainUtils.gradientToBitmap(gradientDrawable))
            selectedBackground = MainUtils.bitmapToString(MainUtils.gradientToBitmap(gradientDrawable))
            Log.i("gradientItemClickedPalette: ", MainUtils.bitmapToString(MainUtils.gradientToBitmap(gradientDrawable)).length.toString())

        }
        else {

            binding.rootLayoutSpeech.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.theme_color
                )
            )

            //save the gradient to preference
            SharedPreference.selectedBackground = ""
            selectedBackground = ""
        }
    }

    override fun selectedWallpaper(wallpaper: Drawable?) {

        //setting the selected color
        binding.rootLayoutSpeech.background = wallpaper

        //save the wallpaper to the preferences
        Log.i("selectedWallpaper: ", wallpaper.toString())
        if (wallpaper != null) {
            SharedPreference.selectedBackground = MainUtils.drawableToString(wallpaper)
            selectedBackground = MainUtils.drawableToString(wallpaper)
        } else {
            SharedPreference.selectedBackground = ""
            selectedBackground = ""
        }
    }

    override fun closeDialog(save: Boolean) {

        if (save) {
            //save the background color to the preference and database
            saveBackgroundColor = true
            colorPaletteDialog.dismissNow()
        } else {
            //resetting the background
            saveBackgroundColor = false
            binding.rootLayoutSpeech.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.theme_color))
            colorPaletteDialog.dismissNow()
        }
    }

    override fun deleteItem(speech: Speech) {

        //remove the item from the list and update the observer
        viewModel.deleteSpeechItem(speech)
    }

    override fun onDetach() {
        super.onDetach()

        if (viewModelMain.noteImageNotClicked){
            //normalizing view
            viewModelMain.bottomFragmentItemDetachEventEvent()
            viewModelMain.noteImageNotClicked = false
            viewModelMain.moveFolderNote = false
        }
    }

    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(galleryIntent)
    }

    override fun openGalleryFromDialog() {
        openGallery()
    }

    override fun noteImageClicked(image: String, position: Int) {

        viewModelMain.noteImageNotClicked = false

        //Check the type of the image that what type of image is it? Gallery image or canvas image
        if (drawing.isEmpty()) {

            //this means that image loaded in imageNote is loaded from gallery So, open the gallery
            //after take runtime image collection permission and add the image to the parent layout background
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                    //opening the gallery
                    openGallery()
                }
                else {
                    // Request the permissions
                    ActivityCompat.requestPermissions(
                        requireActivity(),
                        arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                        REQUEST_CODE_STORAGE
                    )
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
        }
        else {

            if (editNote || viewModelHome.selectedNotes.isEmpty()){
                //this means user has clicked old drawing image
                //fire the event to handle the view
                viewModelMain.bottomBarItemsListenerEvent("draw_note")
                //this means that image loaded in imageNote is drawn in canvas and the note is new
                //Navigate to the drawing fragment
                val host = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_main_fragment_container) as NavHostFragment
                val navController = host.navController
                navController.setGraph(R.navigation.main_navigation)
                //navigation
                navController.navigateSafe(AddEditFragmentDirections.actionAddEditFragmentToDrawFragment())
                //fire the event for already created drawing
                viewModelMain.drawingClickEvent(drawing)
            }
            else{

                //fire the event to handle the view
                viewModelMain.bottomBarItemsListenerEvent("draw_note")
                //this means that image loaded in imageNote is drawn in canvas and the note is new
                //Navigate to the drawing fragment
                val host = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_main_fragment_container) as NavHostFragment
                val navController = host.navController
                navController.setGraph(R.navigation.main_navigation)
                //navigation
                navController.popBackStack()
                //fire the event for already created drawing
                viewModelMain.drawingClickEvent(drawing)
            }
        }
    }
}