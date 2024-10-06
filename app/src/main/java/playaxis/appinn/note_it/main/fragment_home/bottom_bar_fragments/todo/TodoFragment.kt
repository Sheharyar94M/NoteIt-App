package playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.todo

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.iterator
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.transition.Transition
import androidx.transition.TransitionListenerAdapter
import com.google.android.material.transition.MaterialContainerTransform
import playaxis.appinn.note_it.MainNavigationDirections
import playaxis.appinn.note_it.R
import playaxis.appinn.note_it.application.QuickNotepad
import playaxis.appinn.note_it.databinding.FragmentTodoBinding
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
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.todo.adapter.TodoListAdapter
import playaxis.appinn.note_it.main.utils.observeEvent
import playaxis.appinn.note_it.main.viewModels.MainViewModel
import playaxis.appinn.note_it.main.viewModels.viewModel
import playaxis.appinn.note_it.navigation.HomeDestination
import playaxis.appinn.note_it.preferences.SharedPreference
import playaxis.appinn.note_it.repository.model.entities.ColorNote
import playaxis.appinn.note_it.repository.model.entities.GradientNote
import playaxis.appinn.note_it.repository.model.entities.Note
import playaxis.appinn.note_it.repository.model.entities.NoteStatus
import playaxis.appinn.note_it.repository.model.entities.NoteType
import playaxis.appinn.note_it.utils.MainUtils
import javax.inject.Inject

class TodoFragment : Fragment(), ColorPaletteDialog.CloseDialogEvent, ColorSelectedInterface,
    GradientSelectedInterface, WallpaperSelectedInterface,
    NoteImageListAdapter.NoteImageClickedEvent, AddEditNoteBottomSheetDialog.OpenGalleryEvent {

    private val viewModelMain: MainViewModel by activityViewModels()
    private val viewModelHome: HomeViewModel by activityViewModels()
    private lateinit var binding: FragmentTodoBinding

    @Inject
    lateinit var viewModelAddEditNoteFactory: AddEditNoteViewModel.Factory
    private val viewModel by viewModel { viewModelAddEditNoteFactory.create(it) }

    private lateinit var bottomSheetDialogList: ArrayList<BottomDialogItem>
    private lateinit var colorPaletteDialog: ColorPaletteDialog
    private var drawing = ArrayList<DrawingView.CustomPath>()

    private val args: TodoFragmentArgs by navArgs()

    private var selectedBackground = ""
    private var saveBackgroundColor = false
    private lateinit var adapterImages: NoteImageListAdapter
    private var editNote: Boolean = false
    //clicked note
    private lateinit var noteClicked: Note
    private var REQUEST_CODE_STORAGE = 3

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedElementEnterTransition = MaterialContainerTransform(requireContext(), true).apply {
            fadeMode = MaterialContainerTransform.FADE_MODE_CROSS
            duration = resources.getInteger(com.google.android.material.R.integer.material_motion_duration_long_1).toLong()
        }

        sharedElementReturnTransition = MaterialContainerTransform(requireContext(), false).apply {
            scrimColor = Color.TRANSPARENT
            fadeMode = MaterialContainerTransform.FADE_MODE_CROSS
            duration = resources.getInteger(com.google.android.material.R.integer.material_motion_duration_long_1).toLong()
        }

        // Send an event via the sharedViewModel when the transition has finished playing
        (sharedElementReturnTransition as MaterialContainerTransform).addListener(object :
            TransitionListenerAdapter() {
            override fun onTransitionEnd(transition: Transition) {
                viewModelHome.sharedElementTransitionFinished()
            }
        })

        //injection initialization
        (QuickNotepad.appContext as QuickNotepad).appComponent.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        binding = FragmentTodoBinding.inflate(inflater, container, false)

        bottomSheetDialogList = ArrayList()
        colorPaletteDialog = ColorPaletteDialog(this, this, this, this)
        adapterImages = NoteImageListAdapter(this)

        if (arguments != null) {

            if (args.noteId == 0L && !viewModelHome.editing) {
                editNote = false
                //setting the change selection true for the new note
                viewModelHome.changeSelection = true

                //this is new note
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
            else {

                editNote = true
                //setting the change selection true for the new note
                viewModelHome.changeSelection = true

                noteClicked = viewModelHome.getClickedNote()
                //set the fonts also to the text
                binding.titleEditText.setText(noteClicked.title)

                //setting the selected color from the clicked item
                if (noteClicked.colorNote != null) {
                    if (noteClicked.colorNote!!.color.isNotEmpty()) {
                        if (noteClicked.colorNote!!.color.length in 1..9) {

                            //this means there is color in the background of the note item
                            binding.fragmentEditTodoLayout.setBackgroundColor(noteClicked.colorNote!!.color.toInt())
                        }
                        else {

                            //this means there is image in the background of the note item
                            binding.fragmentEditTodoLayout.background = MainUtils.stringToDrawable(noteClicked.colorNote!!.color)
                        }
                    }
                }

                Log.i("noteClicked: ", noteClicked.noteImages.toString())

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
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.moveFolderNote = false

        //calling start function of AddEditViewModel because the work is same for note list and note text
        viewModel.start(
            args.noteId,
            args.labelId,
            args.changeReminder,
            NoteType.LIST,
            args.title,
            args.content
        )

        //initializing the adapter
        val adapterTodo = TodoListAdapter(requireContext(), viewModel)

        if (viewModelMain.moveFolderNote){
            //setting values from viewModel
            setValuesSavedInitially(adapterTodo)
        }
        else{
            //switching to list type
            viewModel.toggleNoteType()
        }

        normalizeKeyBoardView()
        keyBoardViewListeners()

        //initialize recyclerview Todo_list
        val rcvTodo = binding.todoList
        rcvTodo.setHasFixedSize(true)
        val layoutManagerTodo = object: LinearLayoutManager(context){
            override fun canScrollVertically() = false
            override fun canScrollHorizontally() = false
        }
        rcvTodo.adapter = adapterTodo
        rcvTodo.layoutManager = layoutManagerTodo
        rcvTodo.itemAnimator = object : DefaultItemAnimator() {
            override fun animateAppearance(
                viewHolder: RecyclerView.ViewHolder,
                preLayoutInfo: ItemHolderInfo?,
                postLayoutInfo: ItemHolderInfo
            ): Boolean {
                return if (preLayoutInfo != null && (preLayoutInfo.left != postLayoutInfo.left || preLayoutInfo.top != postLayoutInfo.top)
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

        //setting adapter and registering observers
        setupViewModelObservers(adapterTodo)

        //menu listener
        binding.menuViewButton.setOnClickListener {
            showMenu(it)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setupViewModelObservers(adapter: TodoListAdapter) {
        val host = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_main_fragment_container) as NavHostFragment
        val navController = host.navController
        navController.setGraph(R.navigation.main_navigation)

        //setting list to the adapter
        viewModel.editItems.observe(viewLifecycleOwner,adapter::submitList)

        viewModel.focusEvent.observeEvent(viewLifecycleOwner, adapter::setItemFocus)

        viewModel.noteCreateEvent.observeEvent(viewLifecycleOwner) { noteId ->
            viewModelHome.noteCreated(noteId)
        }

        viewModel.statusChangeEvent.observeEvent(viewLifecycleOwner, viewModelHome::onStatusChange)

        viewModel.showLabelsFragmentEvent.observeEvent(viewLifecycleOwner) { noteId ->
            navController.navigateSafe(
                MainNavigationDirections.mainToCreateEditLabelFragment(
                    longArrayOf(noteId)
                )
            )
        }

        viewModelHome.reminderChangeEvent.observeEvent(viewLifecycleOwner) { reminder ->
            viewModel.onReminderChange(reminder)
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

        //save the item list event is here
        viewModelMain.saveNoteOnBackPressEvent.observeEvent(viewLifecycleOwner) {

            //save the note to database

            //setting pin status of object
            if (viewModelHome.selectedNotes.isNotEmpty()){
                val clickedNote = viewModelHome.getClickedNote()
                viewModel.pinned = clickedNote.pinned
                viewModel.locked = clickedNote.lock
                viewModel.status = clickedNote.status
            }

            //saving selected color to the note object
            if (saveBackgroundColor){
                viewModel.colorNote = ColorNote(selectedBackground)
            }
            else if (SharedPreference.selectedBackground != null && saveBackgroundColor){
                viewModel.colorNote = ColorNote(SharedPreference.selectedBackground!!)
            }

            //get the data from view
            viewModel.title = binding.titleEditText.text.toString()

            //save the image from home viewModel in the AddEditViewModel to add the image to the database
            //saving the image in viewModel
            if (viewModelHome.imageNote.isNotEmpty())
                viewModel.noteImage = viewModelHome.imageNote

            Log.i("titleEditText: ", binding.titleEditText.text.toString())
            Log.i("noteImageViewModel: ", viewModel.noteImage.toString())

            if (viewModelHome.selectedFolder != null)
                viewModel.folderId = viewModelHome.selectedFolder!!.folder.id

            //save the data
            requireView().hideKeyboard()
            viewModel.saveNote()
            //set the destination so that data could be updated
            viewModelHome.setDestination(HomeDestination.Status(NoteStatus.ACTIVE))
            viewModelHome.cameraNote = false

            Handler().postDelayed({

                viewModel.exit()
                viewModelHome.selectedNoteIds.clear()
                viewModelHome.clearSelection()
            }, 250)
        }

        //new note event
        viewModel.newNoteEvent.observeEvent(viewLifecycleOwner){ newNote ->
            //add new note to the selection
            viewModelHome.addNewNoteToSelection(newNote)
        }

        //displaying the data if it is saved
        viewModelHome.saveInstanceOfNoteEvent.observeEvent(viewLifecycleOwner){
            //display the content, images and title if they are present in viewModel
            saveValuesInitially()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setValuesSavedInitially(adapter: TodoListAdapter) {

        //setting pin status of object
        if (viewModelHome.selectedNotes.isNotEmpty()){
            val clickedNote = viewModelHome.getClickedNote()
            viewModel.pinned = clickedNote.pinned
            viewModel.locked = clickedNote.lock
            viewModel.status = clickedNote.status
        }

        //saving selected color to the note object
        if (saveBackgroundColor){
            viewModel.colorNote = ColorNote(selectedBackground)
        }
        else if (SharedPreference.selectedBackground != null && saveBackgroundColor){
            viewModel.colorNote = ColorNote(SharedPreference.selectedBackground!!)
        }

        //get the data from view
        viewModel.title = binding.titleEditText.text.toString()

        //save the image from home viewModel in the AddEditViewModel to add the image to the database
        //saving the image in viewModel
        if (viewModelHome.imageNote.isNotEmpty())
            viewModel.noteImage = viewModelHome.imageNote

        Log.i("titleEditText: ", binding.titleEditText.text.toString())
        Log.i("noteImageViewModel: ", viewModel.noteImage.toString())

        if (viewModelHome.selectedFolder != null)
            viewModel.folderId = viewModelHome.selectedFolder!!.folder.id

        //setting the values
        if (viewModelHome.imageNote.isNotEmpty()) {

            binding.noteImageList.visibility = View.VISIBLE
            //recyclerview
            val rcv = binding.noteImageList
            rcv.setHasFixedSize(true)
            val adapter = adapterImages
            if (viewModelHome.imageNote.size == 1) {
                val layoutManager = object : StaggeredGridLayoutManager(1, VERTICAL) {
                    override fun canScrollVertically() = false
                    override fun canScrollHorizontally() = false
                }

                rcv.adapter = adapter
                rcv.layoutManager = layoutManager
                //setting the list
                adapterImages.setImagesList(viewModelHome.imageNote)
            }
            else if (viewModelHome.imageNote.size < 3) {

                val layoutManager = object : StaggeredGridLayoutManager(2, VERTICAL) {
                    override fun canScrollVertically() = false
                    override fun canScrollHorizontally() = false
                }

                rcv.adapter = adapter
                rcv.layoutManager = layoutManager
                //setting the list
                adapterImages.setImagesList(viewModelHome.imageNote)
            }
            else {

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
            binding.noteImageList.visibility = View.GONE

        if (viewModelHome.title != null)
            binding.titleEditText.setText(viewModelHome.title)

        //set the list items values
        viewModelHome.editItemsCopy.observe(viewLifecycleOwner){
            //setting the list to the viewModel and to the adapter
            viewModel.setItemsList(it)
            viewModel.updateListItems()
            adapter.submitList(it)
            viewModelMain.moveFolderNote = false
            viewModel.moveFolderNote = true
            //remove this observer
            viewModelHome.editItemsCopy.removeObservers(viewLifecycleOwner)
        }
    }

    private fun saveValuesInitially() {

        //setting pin status of object
        if (viewModelHome.selectedNotes.isNotEmpty()) {
            val clickedNote = viewModelHome.getClickedNote()
            viewModel.pinned = clickedNote.pinned
            viewModelHome.pinned = clickedNote.pinned
            viewModel.locked = clickedNote.lock
            viewModelHome.locked = clickedNote.lock
            viewModel.status = clickedNote.status
            viewModelHome.status = clickedNote.status
        }

        //saving selected color to the note object
        if (saveBackgroundColor){
            viewModel.colorNote = ColorNote(selectedBackground)
            viewModelHome.colorNote = ColorNote(selectedBackground)
        }
        else if (SharedPreference.selectedBackground != null && saveBackgroundColor){

            viewModel.colorNote = ColorNote(SharedPreference.selectedBackground!!)
            viewModelHome.colorNote = ColorNote(SharedPreference.selectedBackground!!)
        }

        Log.i("titleEditText: ", binding.titleEditText.text.toString())
        Log.i("noteImageViewModel: ", viewModel.noteImage.toString())

        //saving the data to viewModels
        viewModel.title = binding.titleEditText.text.toString()
        viewModelHome.title = binding.titleEditText.text.toString()

        //setting the folder to note if exists
        if (viewModelHome.selectedFolder != null){

            viewModel.folderId = viewModelHome.selectedFolder!!.folder.id
            viewModelHome.folderId = viewModelHome.selectedFolder!!.folder.id
        }

        //save the list here
        viewModel.editItems.observe(viewLifecycleOwner){ list ->
            viewModelHome.editItemsCopy(list)
            viewModel.setItemsList(list)
        }
    }

    private fun normalizeKeyBoardView() {
        binding.keyboardView.undoRedo.visibility = View.GONE
        binding.keyboardView.scannerKeyboardViewButton.visibility = View.GONE
        binding.keyboardView.formatKeyboardViewButton.visibility = View.GONE
        binding.keyboardView.scannerKeyboardViewButton.visibility = View.GONE
    }

    private fun keyBoardViewListeners() {

        binding.keyboardView.addItemsKeyboardViewButton.setOnClickListener {

            bottomSheetDialogList = ArrayList()

            bottomSheetDialogList.add(
                BottomDialogItem(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.camera_icon
                    )!!, getString(R.string.take_photo)
                )
            )
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
//            bottomSheetDialogList.add(
//                BottomDialogItem(
//                    ContextCompat.getDrawable(
//                        requireContext(),
//                        R.drawable.mike_button_
//                    )!!, getString(R.string.recording)
//                )
//            )
//            bottomSheetDialogList.add(
//                BottomDialogItem(
//                    ContextCompat.getDrawable(
//                        requireContext(),
//                        R.drawable.check_box
//                    )!!, getString(R.string.check_boxes)
//                )
//            )

            //bottom sheet dialog add items
            val addEditNoteBottomSheetDialog = AddEditNoteBottomSheetDialog(bottomSheetDialogList, false,this, "list")
            addEditNoteBottomSheetDialog.isCancelable = true
            addEditNoteBottomSheetDialog.show(
                requireActivity().supportFragmentManager,
                addEditNoteBottomSheetDialog.tag
            )
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
            val addEditNoteBottomSheetDialog =
                AddEditNoteBottomSheetDialog(bottomSheetDialogList, menu = true,this, "list")
            addEditNoteBottomSheetDialog.isCancelable = true
            addEditNoteBottomSheetDialog.show(
                requireActivity().supportFragmentManager,
                addEditNoteBottomSheetDialog.tag
            )
        }
    }

    private fun showMenu(view: View) {
        val popupMenu = PopupMenu(requireContext(), view, Gravity.END, 0, R.style.CustomPopupMenu)

        val inflater = popupMenu.menuInflater
        inflater.inflate(R.menu.todo_menu, popupMenu.menu)

        //setting color of the text items
        for (item in popupMenu.menu.iterator()) {

            val spannable = SpannableString(item.title.toString())
            spannable.setSpan(
                ForegroundColorSpan(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.dialog_text_color
                    )
                ), 0, spannable.length, 0
            )
            item.title = spannable
        }

        // Optionally, set a listener for menu item clicks
        popupMenu.setOnMenuItemClickListener(object : PopupMenu.OnMenuItemClickListener {
            override fun onMenuItemClick(item: MenuItem): Boolean {
                // Handle menu item clicks here
                when (item.itemId) {
                    R.id.check_items -> {

                        //uncheck all checked items
                        viewModel.uncheckAllItems()
                        return true
                    }

                    R.id.delete_items_done -> {

                        //delete all checked items
                        viewModel.deleteCheckedItems()
                        return true
                    }
                }
                return false
            }
        })
        popupMenu.setForceShowIcon(true)
        popupMenu.show()
    }

    override fun selectedColor(color: Int) {

        //setting the selected color
        binding.fragmentEditTodoLayout.setBackgroundColor(color)

        //save the color to preference
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
            val gradientDrawable =
                GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, gradientColors)
            binding.fragmentEditTodoLayout.background = gradientDrawable

            //save the gradient to preference

            SharedPreference.selectedBackground =
                MainUtils.bitmapToString(MainUtils.gradientToBitmap(gradientDrawable))
            selectedBackground =
                MainUtils.bitmapToString(MainUtils.gradientToBitmap(gradientDrawable))
            Log.i(
                "gradientItemClickedPalette: ",
                MainUtils.bitmapToString(MainUtils.gradientToBitmap(gradientDrawable)).length.toString()
            )

        } else {

            binding.fragmentEditTodoLayout.setBackgroundColor(
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
        binding.fragmentEditTodoLayout.background = wallpaper

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
            saveBackgroundColor = true
            binding.fragmentEditTodoLayout.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.theme_color
                )
            )
            colorPaletteDialog.dismissNow()
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

    override fun onDetach() {
        super.onDetach()

        if (viewModelMain.noteImageNotClicked) {
            //normalizing view
            viewModelMain.bottomFragmentItemDetachEventEvent()
            viewModelMain.noteImageNotClicked = false
        }
    }

    companion object {
        private const val REMOVE_CHECKED_CONFIRM_DIALOG_TAG = "remove_checked_confirm_dialog"
    }
}