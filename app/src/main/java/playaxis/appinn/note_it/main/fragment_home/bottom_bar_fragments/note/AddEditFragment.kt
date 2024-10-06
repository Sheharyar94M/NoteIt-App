package playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.note

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.AbsoluteSizeSpan
import android.text.style.BulletSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import playaxis.appinn.note_it.MainNavigationDirections
import playaxis.appinn.note_it.R
import playaxis.appinn.note_it.application.QuickNotepad
import playaxis.appinn.note_it.databinding.FragmentAddEditBinding
import playaxis.appinn.note_it.extensions.hideKeyboard
import playaxis.appinn.note_it.extensions.navigateSafe
import playaxis.appinn.note_it.main.fragment_home.HomeViewModel
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.dialogs.AddEditNoteBottomSheetDialog
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.dialogs.ColorPaletteDialog
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.dialogs.interfaces.ColorSelectedInterface
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.dialogs.interfaces.GradientSelectedInterface
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.dialogs.interfaces.WallpaperSelectedInterface
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.draw.canvas.DrawingView
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.note.adapters.ColorListAdapter
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.note.adapters.NoteImageListAdapter
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.note.adapters.SpinnerAdapter
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.note.helper_model.BottomDialogItem
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.note.helper_model.CustomTypefaceSpan
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.note.helper_model.FontItem
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.note.viewmodels.AddEditNoteViewModel
import playaxis.appinn.note_it.main.utils.Event
import playaxis.appinn.note_it.main.utils.StatusChange
import playaxis.appinn.note_it.main.utils.observeEvent
import playaxis.appinn.note_it.main.utils.send
import playaxis.appinn.note_it.main.viewModels.MainViewModel
import playaxis.appinn.note_it.main.viewModels.viewModel
import playaxis.appinn.note_it.navigation.HomeDestination
import playaxis.appinn.note_it.preferences.SharedPreference
import playaxis.appinn.note_it.repository.model.entities.ColorNote
import playaxis.appinn.note_it.repository.model.entities.GradientNote
import playaxis.appinn.note_it.repository.model.entities.Note
import playaxis.appinn.note_it.repository.model.entities.NoteStatus
import playaxis.appinn.note_it.repository.model.entities.NoteTextFormat
import playaxis.appinn.note_it.repository.model.entities.NoteType
import playaxis.appinn.note_it.utils.MainUtils
import javax.inject.Inject

class AddEditFragment : Fragment(), ColorPaletteDialog.CloseDialogEvent, ColorSelectedInterface,
    GradientSelectedInterface, WallpaperSelectedInterface, ColorListAdapter.ColorsItemSelectedEvent,
    NoteImageListAdapter.NoteImageClickedEvent, AddEditNoteBottomSheetDialog.OpenGalleryEvent {

    private lateinit var binding: FragmentAddEditBinding

    private val viewModelHome by activityViewModels<HomeViewModel>()
    private val viewModelMain by activityViewModels<MainViewModel>()

    @Inject
    lateinit var viewModelAddEditNoteFactory: AddEditNoteViewModel.Factory
    private val viewModel by viewModel { viewModelAddEditNoteFactory.create(it) }

    private val args: AddEditFragmentArgs by navArgs()

    private lateinit var bottomSheetDialogList: ArrayList<BottomDialogItem>
    private lateinit var colorPaletteDialog: ColorPaletteDialog
    private lateinit var adapterColors: ColorListAdapter

    // Define the list of fonts
    private val fonts = ArrayList<FontItem>()

    //data of note format
    private var selectedFont: FontItem? = null
    private var selectedPreviousFont: FontItem? = null
    private var fontPosition = -1
    private var fontPreviousPosition = -1
    private var isHeading1Enabled = false
    private var isHeading2Enabled = false
    private var isBoldEnabled = false
    private var isItalicEnabled = false
    private var isUnderlineEnabled = false
    private var isBulletEnabled = false
    private var isTextSpannable = false
    private var firstRunFontApply = false
    private var selectedColor: ColorNote? = null
    private var selectedBackground = ""
    private var saveBackgroundColor = false
    private var editNote: Boolean = false

    //text formatting applied event
    private var selectionFormatting = MutableLiveData<Event<Unit>>()

    private val REQ_CODE_CAMERA = 100
    private var REQUEST_CODE_STORAGE = 3
    private var drawing = ArrayList<DrawingView.CustomPath>()

    //clicked note
    private lateinit var noteClicked: Note
    private lateinit var adapterImages: NoteImageListAdapter

    // Initialize the ActivityResultLauncher in your fragment
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->

            val host =
                requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_main_fragment_container) as NavHostFragment
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
                    if (viewModelHome.imageNote.size == 1) {
                        val layoutManager = object : StaggeredGridLayoutManager(1, VERTICAL) {
                            override fun canScrollVertically() = false
                            override fun canScrollHorizontally() = false
                        }

                        rcv.adapter = adapter
                        rcv.layoutManager = layoutManager
                        //setting the list
                        adapterImages.setImagesList(viewModelHome.imageNote)
                    } else if (viewModelHome.imageNote.size < 3) {

                        val layoutManager = object : StaggeredGridLayoutManager(2, VERTICAL) {
                            override fun canScrollVertically() = false
                            override fun canScrollHorizontally() = false
                        }

                        rcv.adapter = adapter
                        rcv.layoutManager = layoutManager
                        //setting the list
                        adapterImages.setImagesList(viewModelHome.imageNote)
                    } else {

                        val layoutManager = object : StaggeredGridLayoutManager(3, VERTICAL) {
                            override fun canScrollVertically() = false
                            override fun canScrollHorizontally() = false
                        }

                        rcv.adapter = adapter
                        rcv.layoutManager = layoutManager
                        //setting the list
                        adapterImages.setImagesList(viewModelHome.imageNote)
                    }
                } else
                    viewModelHome.imageNote.clear()
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        binding = FragmentAddEditBinding.inflate(inflater, container, false)

        //injection initialization
        (QuickNotepad.appContext as QuickNotepad).appComponent.inject(this)

        MainUtils.statusBarColor(
            requireActivity(),
            ContextCompat.getColor(requireContext(), R.color.search_bar_background)
        )

        bottomSheetDialogList = ArrayList()

        binding.titleEditText.isEnabled = true
        binding.contentEdt.isEnabled = true

        adapterImages = NoteImageListAdapter(this)
        adapterColors = ColorListAdapter(this, viewLifecycleOwner)

        //disabling scroll
        binding.noteImageList.isScrollContainer = false

        Log.i("noteId: ", args.noteId.toString())
        //setting id of note to update the note
        viewModel.noteClickedId = args.noteId

        //format listeners
        setupFormatView()

        if (arguments != null) {

            if (args.noteId == 0L && !viewModelHome.editing) {
                editNote = false
                //setting the change selection true for the new note
                viewModelHome.changeSelection = true

                //this is new note
                //set the recyclerview for the image list (this will give the list of the image items in the note)
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
                    } else if (viewModelHome.imageNote.size < 3) {

                        val layoutManager = object : StaggeredGridLayoutManager(2, VERTICAL) {
                            override fun canScrollVertically() = false
                            override fun canScrollHorizontally() = false
                        }

                        rcv.adapter = adapter
                        rcv.layoutManager = layoutManager
                        //setting the list
                        adapterImages.setImagesList(viewModelHome.imageNote)
                    } else {

                        val layoutManager = object : StaggeredGridLayoutManager(3, VERTICAL) {
                            override fun canScrollVertically() = false
                            override fun canScrollHorizontally() = false
                        }

                        rcv.adapter = adapter
                        rcv.layoutManager = layoutManager
                        //setting the list
                        adapterImages.setImagesList(viewModelHome.imageNote)
                    }
                } else
                    binding.noteImageList.visibility = View.GONE
            }
            else {

                editNote = true
                viewModelHome.changeSelection = false

                noteClicked = viewModelHome.getClickedNote()

                //set the fonts also to the text
                binding.titleEditText.setText(noteClicked.title)

                //checking if the content is spannable or not
                if (noteClicked.noteTextFormat.spannable)
                    binding.contentEdt.text = MainUtils.deserializeSpannableString(requireContext(), noteClicked)
                else
                    binding.contentEdt.setText(noteClicked.content)


                //setting the selected color from the clicked item
                if (noteClicked.colorNote != null) {
                    if (noteClicked.colorNote!!.color.isNotEmpty()) {
                        if (noteClicked.colorNote!!.color.length in 1..9) {
                            //this means there is color in the background of the note item
                            binding.rootLayout.setBackgroundColor(noteClicked.colorNote!!.color.toInt())
                        } else {
                            //this means there is image in the background of the note item
                            binding.rootLayout.background =
                                MainUtils.stringToDrawable(noteClicked.colorNote!!.color)
                        }
                    }
                }

                Log.i("noteClicked: ", noteClicked.noteImages.toString())

                //set the image list in viewModel
                viewModelHome.imageNote = noteClicked.noteImages as ArrayList<String>

                //set the recyclerview for the image list (this will give the list of the image items in the note)
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
                    } else if (viewModelHome.imageNote.size < 3) {

                        val layoutManager = object : StaggeredGridLayoutManager(2, VERTICAL) {
                            override fun canScrollVertically() = false
                            override fun canScrollHorizontally() = false
                        }

                        rcv.adapter = adapter
                        rcv.layoutManager = layoutManager
                        //setting the list
                        adapterImages.setImagesList(viewModelHome.imageNote)
                    } else {

                        val layoutManager = object : StaggeredGridLayoutManager(3, VERTICAL) {
                            override fun canScrollVertically() = false
                            override fun canScrollHorizontally() = false
                        }

                        rcv.adapter = adapter
                        rcv.layoutManager = layoutManager
                        //setting the list
                        adapterImages.setImagesList(viewModelHome.imageNote)
                    }
                } else
                    binding.noteImageList.visibility = View.GONE
            }
        }

        Log.i("noteEdit: ", editNote.toString())

        colorPaletteDialog = ColorPaletteDialog(this, this, this, this)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.contentEdt.requestFocus()
        binding.contentEdt.setSelection(binding.contentEdt.text!!.length)

        if (viewModelMain.moveFolderNote) {
            //setting values from viewModel
            setValuesSavedInitially()
            //setting identifier
            viewModelMain.moveFolderNote = false
        }

        //listeners
        keyBoardViewListeners()
        //normalization keyBoard view
        normalizeKeyBoardView()

        viewModel.start(
            args.noteId,
            args.labelId,
            args.changeReminder,
            NoteType.TEXT,
            args.title,
            args.content
        )

        //switching to list type
        viewModel.toggleNoteType()

        //setting value from OCR text
        viewModelMain.recognizedTextLiveData.observeEvent(viewLifecycleOwner) { recognizedString ->
            binding.contentEdt.requestFocus()
            binding.contentEdt.post {
                binding.contentEdt.setText(recognizedString)
            }
            Log.i("recognizedStringLive: ", recognizedString)
        }

        //adding a text-watcher to underline the text
        // Add TextWatcher to EditText
        binding.contentEdt.addTextChangedListener(object : TextWatcher {

            var start = 0
            var end = 0

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                this.start = start
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                this.end = start + count
            }
            override fun afterTextChanged(s: Editable?) {

                binding.fontView.fontView.visibility = View.GONE
                if (s != null && start != end){
                    applyFormatting(s, start, end)
                }
            }
        })

        //applying font settings to the selected Text
        selectionFormatting.observeEvent(viewLifecycleOwner) {

            //apply the fonts  but first get the points and selected text
            val startSelection = binding.contentEdt.selectionStart
            val endSelection = binding.contentEdt.selectionEnd

            //getting the selection editable
            val selection = binding.contentEdt.text as Editable
            //applying the formatting
            applyFormatting(selection, startSelection, endSelection)

            //after this make the font previously set
            selectedFont = selectedPreviousFont
            fontPosition = fontPreviousPosition
            //selecting the item from spinner
            binding.fontView.fontsDropdown.setSelection(fontPosition,true)
        }

        /**Making font view invisible on listeners and even if the focus changes**/
        binding.titleEditText.onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
            binding.fontView.fontView.visibility = View.GONE
        }
        binding.contentEdt.onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
            binding.fontView.fontView.visibility = View.GONE
        }

        binding.titleEditText.setOnClickListener {
            binding.fontView.fontView.visibility = View.GONE
        }

        binding.contentEdt.setOnClickListener {
            binding.fontView.fontView.visibility = View.GONE
        }

        //observers
        observers()
    }

    private fun setValuesSavedInitially() {

        //set the value that edit note drawing is new
        viewModelHome.drawingEdit = false

        //setting pin value
        if (viewModelHome.selectedNotes.isNotEmpty()) {
            val clickedNote = viewModelHome.getClickedNote()
            viewModel.pinned = clickedNote.pinned
            viewModel.locked = clickedNote.lock
            viewModel.status = clickedNote.status
        }

        //saving selected color to the note object
        if (saveBackgroundColor)
            viewModel.colorNote = ColorNote(selectedBackground)
        else if (SharedPreference.selectedBackground != null && saveBackgroundColor)
            viewModel.colorNote = ColorNote(SharedPreference.selectedBackground!!)

        //save the image from home viewModel in the AddEditViewModel to add the image to the database
        //saving the image in viewModel
        if (viewModelHome.imageNote.isNotEmpty())
            viewModel.noteImage = viewModelHome.imageNote

        Log.i("titleEditText: ", binding.titleEditText.text.toString())
        Log.i("noteImageViewModel: ", viewModel.noteImage.toString())

        if (viewModelHome.title != null)
            viewModel.title = viewModelHome.title!!

        //content can be spannable or not spannable so checking and serializing the content accordingly
        viewModel.noteTextFormat = NoteTextFormat(isTextSpannable, fontPosition)
        viewModel.content = viewModelHome.content!!

        //setting the folder to note if exists
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
            } else if (viewModelHome.imageNote.size < 3) {

                val layoutManager = object : StaggeredGridLayoutManager(2, VERTICAL) {
                    override fun canScrollVertically() = false
                    override fun canScrollHorizontally() = false
                }

                rcv.adapter = adapter
                rcv.layoutManager = layoutManager
                //setting the list
                adapterImages.setImagesList(viewModelHome.imageNote)
            } else {

                val layoutManager = object : StaggeredGridLayoutManager(3, VERTICAL) {
                    override fun canScrollVertically() = false
                    override fun canScrollHorizontally() = false
                }

                rcv.adapter = adapter
                rcv.layoutManager = layoutManager
                //setting the list
                adapterImages.setImagesList(viewModelHome.imageNote)
            }
        } else
            binding.noteImageList.visibility = View.GONE

        if (viewModelHome.title != null)
            binding.titleEditText.setText(viewModelHome.title)

        if (viewModelHome.content != null)
            binding.contentEdt.setText(viewModelHome.content)
    }

    private fun saveValuesInitially() {

        //setting pin value
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
        if (saveBackgroundColor) {
            viewModel.colorNote = ColorNote(selectedBackground)
            viewModelHome.colorNote = ColorNote(selectedBackground)
        }
        else if (SharedPreference.selectedBackground != null && saveBackgroundColor) {

            viewModel.colorNote = ColorNote(SharedPreference.selectedBackground!!)
            viewModelHome.colorNote = ColorNote(SharedPreference.selectedBackground!!)
        }

        Log.i("titleEditText: ", binding.titleEditText.text.toString())
        Log.i("noteImageViewModel: ", viewModel.noteImage.toString())

        //saving the data to viewModels
        viewModel.title = binding.titleEditText.text.toString()
        viewModelHome.title = binding.titleEditText.text.toString()

        //content can be spannable or not spannable so checking and serializing the content accordingly
        if (isTextSpannable) {

            //this value will be false if colors or formatting is not applied other wise it will be true
            viewModel.noteTextFormat = NoteTextFormat(isTextSpannable, fontPosition)
            viewModelHome.noteTextFormat = NoteTextFormat(isTextSpannable, fontPosition)
            viewModel.content =
                MainUtils.serializeSpannableString(binding.contentEdt.text as SpannableStringBuilder)
            viewModelHome.content =
                MainUtils.serializeSpannableString(binding.contentEdt.text as SpannableStringBuilder)
        } else {
            if (viewModelHome.selectedNotes.isNotEmpty()) {
                val clickedNote = viewModelHome.getClickedNote()
                viewModel.noteTextFormat = NoteTextFormat(
                    clickedNote.noteTextFormat.spannable,
                    clickedNote.noteTextFormat.fontPosition
                )

                viewModelHome.noteTextFormat = NoteTextFormat(
                    clickedNote.noteTextFormat.spannable,
                    clickedNote.noteTextFormat.fontPosition
                )
                if (clickedNote.noteTextFormat.spannable) {

                    viewModel.content =
                        MainUtils.serializeSpannableString(binding.contentEdt.text as SpannableStringBuilder)
                    viewModelHome.content =
                        MainUtils.serializeSpannableString(binding.contentEdt.text as SpannableStringBuilder)
                } else {
                    viewModel.content = binding.contentEdt.text.toString()
                    viewModelHome.content = binding.contentEdt.text.toString()
                }
            }
        }

        //setting the folder to note if exists
        if (viewModelHome.selectedFolder != null) {

            viewModel.folderId = viewModelHome.selectedFolder!!.folder.id
            viewModelHome.folderId = viewModelHome.selectedFolder!!.folder.id
        }
    }

    private fun applyFormatting(editable: Editable, start: Int, end: Int) {
        if (isHeading1Enabled) {
            isTextSpannable = true
            editable.setSpan(
                AbsoluteSizeSpan(24, true),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        if (isHeading2Enabled) {
            isTextSpannable = true
            editable.setSpan(
                AbsoluteSizeSpan(20, true),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        if (isBoldEnabled) {
            isTextSpannable = true
            editable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        if (isItalicEnabled) {
            isTextSpannable = true
            editable.setSpan(
                StyleSpan(Typeface.ITALIC),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        if (isUnderlineEnabled) {
            isTextSpannable = true
            editable.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        if (isBulletEnabled) {
            isTextSpannable = true
            editable.setSpan(BulletSpan(16), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        //color check
        selectedColor?.let {
            editable.setSpan(
                ForegroundColorSpan(selectedColor!!.color.toInt()),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            isTextSpannable = true
        }
        //font check
        selectedFont?.let { font ->
            if (font.typeface == null) {
                // Reset to default font
                editable.setSpan(
                    CustomTypefaceSpan(
                        "poppins_regular.ttf",
                        ResourcesCompat.getFont(requireContext(), R.font.poppins_regular)!!
                    ), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            else {
                // Apply selected font
                val typeface = Typeface.createFromAsset(requireActivity().assets, "font$fontPosition.ttf")
                editable.setSpan(
                    CustomTypefaceSpan("font$fontPosition.ttf", typeface),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }

    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(galleryIntent)
    }

    private fun observers() {

        viewModelMain.saveNoteOnBackPressEvent.observeEvent(viewLifecycleOwner) {

            //set the value that edit note drawing is new
            viewModelHome.drawingEdit = false

            //setting pin value
            if (viewModelHome.selectedNotes.isNotEmpty()) {
                val clickedNote = viewModelHome.getClickedNote()
                viewModel.pinned = clickedNote.pinned
                viewModel.locked = clickedNote.lock
                viewModel.status = clickedNote.status
            }

            //saving selected color to the note object
            if (saveBackgroundColor)
                viewModel.colorNote = ColorNote(selectedBackground)
            else if (SharedPreference.selectedBackground != null && saveBackgroundColor)
                viewModel.colorNote = ColorNote(SharedPreference.selectedBackground!!)

            //save the image from home viewModel in the AddEditViewModel to add the image to the database
            //saving the image in viewModel
            if (viewModelHome.imageNote.isNotEmpty())
                viewModel.noteImage = viewModelHome.imageNote

            Log.i("titleEditText: ", binding.titleEditText.text.toString())
            Log.i("noteImageViewModel: ", viewModel.noteImage.toString())

            viewModel.title = binding.titleEditText.text.toString()

            //content can be spannable or not spannable so checking and serializing the content accordingly
            if (isTextSpannable) {

                //this value will be false if colors or formatting is not applied other wise it will be true
                viewModel.noteTextFormat = NoteTextFormat(isTextSpannable, fontPosition)
                viewModel.content =
                    MainUtils.serializeSpannableString(binding.contentEdt.text as SpannableStringBuilder)
            } else {

                if (viewModelHome.selectedNotes.isNotEmpty()) {
                    val clickedNote = viewModelHome.getClickedNote()
                    viewModel.noteTextFormat = NoteTextFormat(
                        clickedNote.noteTextFormat.spannable,
                        clickedNote.noteTextFormat.fontPosition
                    )
                    if (clickedNote.noteTextFormat.spannable)
                        viewModel.content =
                            MainUtils.serializeSpannableString(binding.contentEdt.text as SpannableStringBuilder)
                    else
                        viewModel.content = binding.contentEdt.text.toString()
                }
            }

            //setting the folder to note if exists
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
                viewModelHome.clearSelection()
                viewModelHome.clear()
            }, 270)
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

        viewModelHome.statusChangeEvent.observeEvent(viewLifecycleOwner) { statusChange ->
            showMessageForStatusChange(statusChange)
        }

        //new note event
        viewModel.newNoteEvent.observeEvent(viewLifecycleOwner) { newNote ->
            //add new note to the selection
            viewModelHome.addNewNoteToSelection(newNote)
        }

        viewModelHome.toolbarListenerEvent.observeEvent(viewLifecycleOwner) {
            binding.fontView.fontView.visibility = View.GONE
        }

        //displaying the data if it is saved
        viewModelHome.saveInstanceOfNoteEvent.observeEvent(viewLifecycleOwner) {
            //display the content, images and title if they are present in viewModel
            saveValuesInitially()
        }
    }

    @SuppressLint("WrongConstant")
    private fun showMessageForStatusChange(statusChange: StatusChange) {
        val messageId = when (statusChange.newStatus) {
            NoteStatus.ACTIVE -> if (statusChange.oldStatus == NoteStatus.DELETED)
                R.plurals.edit_message_move_restore
            else
                R.plurals.edit_message_move_unarchive

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

    private fun setupFormatView() {
        setupFonts()
        setupColorsList()
    }

    private fun setupColorsList() {

        val colors = ArrayList<ColorNote>()
        colors.add(
            ColorNote(
                ContextCompat.getColor(requireContext(), R.color.color_1).toString(),
                isSelected = true
            )
        )
        colors.add(ColorNote(ContextCompat.getColor(requireContext(), R.color.color_2).toString()))
        colors.add(ColorNote(ContextCompat.getColor(requireContext(), R.color.color_3).toString()))
        colors.add(ColorNote(ContextCompat.getColor(requireContext(), R.color.color_4).toString()))
        colors.add(ColorNote(ContextCompat.getColor(requireContext(), R.color.color_5).toString()))
        colors.add(ColorNote(ContextCompat.getColor(requireContext(), R.color.color_6).toString()))
        colors.add(ColorNote(ContextCompat.getColor(requireContext(), R.color.color_7).toString()))
        colors.add(ColorNote(ContextCompat.getColor(requireContext(), R.color.color_8).toString()))
        colors.add(ColorNote(ContextCompat.getColor(requireContext(), R.color.color_9).toString()))
        colors.add(ColorNote(ContextCompat.getColor(requireContext(), R.color.color_10).toString()))
        colors.add(ColorNote(ContextCompat.getColor(requireContext(), R.color.color_11).toString()))

        adapterColors.setColors(colors)

        binding.fontView.colorList.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.fontView.colorList.setHasFixedSize(false)
        binding.fontView.colorList.adapter = adapterColors
    }

    private fun setupFonts() {

        fonts.add(FontItem(null, false))
        fonts.add(FontItem(ResourcesCompat.getFont(requireContext(), R.font.font1)))
        fonts.add(FontItem(ResourcesCompat.getFont(requireContext(), R.font.font2)))
        fonts.add(FontItem(ResourcesCompat.getFont(requireContext(), R.font.font3)))
        fonts.add(FontItem(ResourcesCompat.getFont(requireContext(), R.font.font4)))
        fonts.add(FontItem(ResourcesCompat.getFont(requireContext(), R.font.font5)))
        fonts.add(FontItem(ResourcesCompat.getFont(requireContext(), R.font.font6)))
        fonts.add(FontItem(ResourcesCompat.getFont(requireContext(), R.font.font7)))
        fonts.add(FontItem(ResourcesCompat.getFont(requireContext(), R.font.font8)))
        fonts.add(FontItem(ResourcesCompat.getFont(requireContext(), R.font.font9)))
        fonts.add(FontItem(ResourcesCompat.getFont(requireContext(), R.font.font10)))
        fonts.add(FontItem(ResourcesCompat.getFont(requireContext(), R.font.font11)))
        fonts.add(FontItem(ResourcesCompat.getFont(requireContext(), R.font.font12)))

        // Create an ArrayAdapter using the font names array and a default spinner layout
        val adapter = SpinnerAdapter(requireContext(), android.R.layout.simple_spinner_item, fonts)
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // Apply the adapter to the spinner
        binding.fontView.fontsDropdown.setAdapter(adapter)

        binding.fontView.fontsDropdown.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

                    if (firstRunFontApply) {

                        selectedFont = FontItem(fonts[position].typeface, true)
                        fontPosition = position
                        isTextSpannable = true

                        val startSelection = binding.contentEdt.selectionStart
                        val endSelection = binding.contentEdt.selectionEnd

                        if (startSelection != endSelection) {
                            //event which will tell that the event is performed
                            selectionFormatting.send()
                        }
                        else{
                            selectedPreviousFont = selectedFont
                            fontPreviousPosition = fontPosition
                        }
                    }

                    //first run
                    firstRunFontApply = true
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        //add the list to the sharedPreference
        val gson = Gson()
        SharedPreference.editFontsList = gson.toJson(fonts)
    }

    private fun normalizeKeyBoardView() {
        binding.keyboardView.undoRedo.visibility = View.GONE
    }

    private fun keyBoardViewListeners() {

        binding.keyboardView.addItemsKeyboardViewButton.setOnClickListener {

            //because it should not be visible here
            binding.fontView.fontView.visibility = View.GONE

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
//                    )!!, ContextCompat.getString(requireContext(), R.string.recording)
//                )
//            )
//            bottomSheetDialogList.add(
//                BottomDialogItem(
//                    ContextCompat.getDrawable(
//                        requireContext(),
//                        R.drawable.check_box
//                    )!!, ContextCompat.getString(requireContext(), R.string.to_do_list)
//                )
//            )


            //bottom sheet dialog add items
            val addEditNoteBottomSheetDialog =
                AddEditNoteBottomSheetDialog(bottomSheetDialogList, false, this, "text")
            addEditNoteBottomSheetDialog.isCancelable = true
            addEditNoteBottomSheetDialog.show(
                requireActivity().supportFragmentManager,
                addEditNoteBottomSheetDialog.tag
            )
        }

        binding.keyboardView.colorKeyboardViewButton.setOnClickListener {

            //because it should not be visible here
            binding.fontView.fontView.visibility = View.GONE

            //color select palette
            colorPaletteDialog.isCancelable = false
            colorPaletteDialog.show(
                requireActivity().supportFragmentManager,
                colorPaletteDialog.tag
            )
        }

        binding.keyboardView.menuKeyboardViewButton.setOnClickListener {

            //because it should not be visible here
            binding.fontView.fontView.visibility = View.GONE

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
                AddEditNoteBottomSheetDialog(bottomSheetDialogList, menu = true, this, "text")
            addEditNoteBottomSheetDialog.isCancelable = true
            addEditNoteBottomSheetDialog.show(
                requireActivity().supportFragmentManager,
                addEditNoteBottomSheetDialog.tag
            )
        }

        binding.keyboardView.formatKeyboardViewButton.setOnClickListener {

            //show the dialog for the editing
            if (binding.fontView.fontView.visibility == View.GONE)
                binding.fontView.fontView.visibility = View.VISIBLE
            else {
                binding.fontView.fontView.visibility = View.GONE
            }
        }

        keyBoardFormatViewListeners()

        binding.keyboardView.scannerKeyboardViewButton.setOnClickListener {

            //because it should not be visible here
            binding.fontView.fontView.visibility = View.GONE

            //call the scanner fragment to scan the image and make a note of it
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                //ask for permission
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.CAMERA),
                    REQ_CODE_CAMERA
                )

                //setting value for navigating it to camera and scan after permission
                viewModelHome.scanScreen = true
            } else {
                //call camera fragment
                val host =
                    requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_main_fragment_container) as NavHostFragment
                val navController = host.navController
                navController.setGraph(R.navigation.main_navigation)

                //send the event to normalize the toolbar
                viewModelHome.cameraToolbarNormalizeEvent()
                //to camera fragment
                navController.navigateSafe(
                    AddEditFragmentDirections.actionAddEditFragmentToCameraFragment(
                        true
                    )
                )
            }
        }
    }

    private fun keyBoardFormatViewListeners() {

        binding.fontView.heading1.setOnClickListener {
            //toggle heading 1
            isHeading1Enabled = !isHeading1Enabled

            //turning off other buttons
            isHeading2Enabled = false
            isBoldEnabled = false
            isItalicEnabled = false
            isUnderlineEnabled = false
            isBulletEnabled = false
            //event which will tell that the event is performed
            selectionFormatting.send()

            //setting the view for selected
            if (isHeading1Enabled) {
                binding.fontView.heading1.setBackgroundColor(
                    ContextCompat.getColor(
                        QuickNotepad.appContext,
                        R.color.dialog_text_color
                    )
                )
                binding.fontView.heading1.setImageDrawable(
                    ContextCompat.getDrawable(
                        QuickNotepad.appContext,
                        R.drawable.format_h1_black
                    )
                )
            }
            else {
                binding.fontView.heading1.setBackgroundColor(0)
                binding.fontView.heading1.setImageDrawable(
                    ContextCompat.getDrawable(
                        QuickNotepad.appContext,
                        R.drawable.format_h1
                    )
                )
            }
            //setting the view for un-selected
            binding.fontView.heading2.setBackgroundColor(0)
            binding.fontView.heading2.setImageDrawable(
                ContextCompat.getDrawable(
                    QuickNotepad.appContext,
                    R.drawable.format_h2
                )
            )
            binding.fontView.bold.setBackgroundColor(0)
            binding.fontView.bold.setImageDrawable(
                ContextCompat.getDrawable(
                    QuickNotepad.appContext,
                    R.drawable.format_bold
                )
            )
            binding.fontView.italic.setBackgroundColor(0)
            binding.fontView.italic.setImageDrawable(
                ContextCompat.getDrawable(
                    QuickNotepad.appContext,
                    R.drawable.format_italic
                )
            )
            binding.fontView.underline.setBackgroundColor(0)
            binding.fontView.underline.setImageDrawable(
                ContextCompat.getDrawable(
                    QuickNotepad.appContext,
                    R.drawable.format_underlined
                )
            )
            binding.fontView.bullets.setBackgroundColor(0)
            binding.fontView.bullets.setImageDrawable(
                ContextCompat.getDrawable(
                    QuickNotepad.appContext,
                    R.drawable.format_list_bulleted
                )
            )
        }

        binding.fontView.heading2.setOnClickListener {

            //toggle heading 2
            isHeading2Enabled = !isHeading2Enabled

            //turning off other buttons
            isHeading1Enabled = false
            isBoldEnabled = false
            isItalicEnabled = false
            isUnderlineEnabled = false
            isBulletEnabled = false
            //event which will tell that the event is performed
            selectionFormatting.send()

            //setting the view for selected
            if (isHeading2Enabled) {
                binding.fontView.heading2.setBackgroundColor(
                    ContextCompat.getColor(
                        QuickNotepad.appContext,
                        R.color.dialog_text_color
                    )
                )
                binding.fontView.heading2.setImageDrawable(
                    ContextCompat.getDrawable(
                        QuickNotepad.appContext,
                        R.drawable.format_h2_black
                    )
                )
            }
            else {
                binding.fontView.heading2.setBackgroundColor(0)
                binding.fontView.heading2.setImageDrawable(
                    ContextCompat.getDrawable(
                        QuickNotepad.appContext,
                        R.drawable.format_h2
                    )
                )
            }
            //setting the view for un-selected
            binding.fontView.heading1.setBackgroundColor(0)
            binding.fontView.heading1.setImageDrawable(
                ContextCompat.getDrawable(
                    QuickNotepad.appContext,
                    R.drawable.format_h1
                )
            )
            binding.fontView.bold.setBackgroundColor(0)
            binding.fontView.bold.setImageDrawable(
                ContextCompat.getDrawable(
                    QuickNotepad.appContext,
                    R.drawable.format_bold
                )
            )
            binding.fontView.italic.setBackgroundColor(0)
            binding.fontView.italic.setImageDrawable(
                ContextCompat.getDrawable(
                    QuickNotepad.appContext,
                    R.drawable.format_italic
                )
            )
            binding.fontView.underline.setBackgroundColor(0)
            binding.fontView.underline.setImageDrawable(
                ContextCompat.getDrawable(
                    QuickNotepad.appContext,
                    R.drawable.format_underlined
                )
            )
            binding.fontView.bullets.setBackgroundColor(0)
            binding.fontView.bullets.setImageDrawable(
                ContextCompat.getDrawable(
                    QuickNotepad.appContext,
                    R.drawable.format_list_bulleted
                )
            )
        }

        binding.fontView.bold.setOnClickListener {

            //toggle bold
            isBoldEnabled = !isBoldEnabled

            //turning off other buttons
            isHeading1Enabled = false
            isHeading2Enabled = false
            isItalicEnabled = false
            isUnderlineEnabled = false
            isBulletEnabled = false
            //event which will tell that the event is performed
            selectionFormatting.send()

            //setting the view for selected
            if (isBoldEnabled) {
                binding.fontView.bold.setBackgroundColor(
                    ContextCompat.getColor(
                        QuickNotepad.appContext,
                        R.color.dialog_text_color
                    )
                )
                binding.fontView.bold.setImageDrawable(
                    ContextCompat.getDrawable(
                        QuickNotepad.appContext,
                        R.drawable.format_bold_black
                    )
                )
            }
            else {
                binding.fontView.bold.setBackgroundColor(0)
                binding.fontView.bold.setImageDrawable(
                    ContextCompat.getDrawable(
                        QuickNotepad.appContext,
                        R.drawable.format_bold
                    )
                )
            }
            //setting the view for un-selected
            binding.fontView.heading1.setBackgroundColor(0)
            binding.fontView.heading1.setImageDrawable(
                ContextCompat.getDrawable(
                    QuickNotepad.appContext,
                    R.drawable.format_h1
                )
            )
            binding.fontView.heading2.setBackgroundColor(0)
            binding.fontView.heading2.setImageDrawable(
                ContextCompat.getDrawable(
                    QuickNotepad.appContext,
                    R.drawable.format_h2
                )
            )
            binding.fontView.italic.setBackgroundColor(0)
            binding.fontView.italic.setImageDrawable(
                ContextCompat.getDrawable(
                    QuickNotepad.appContext,
                    R.drawable.format_italic
                )
            )
            binding.fontView.underline.setBackgroundColor(0)
            binding.fontView.underline.setImageDrawable(
                ContextCompat.getDrawable(
                    QuickNotepad.appContext,
                    R.drawable.format_underlined
                )
            )
            binding.fontView.bullets.setBackgroundColor(0)
            binding.fontView.bullets.setImageDrawable(
                ContextCompat.getDrawable(
                    QuickNotepad.appContext,
                    R.drawable.format_list_bulleted
                )
            )
        }

        binding.fontView.italic.setOnClickListener {

            //toggle italic
            isItalicEnabled = !isItalicEnabled

            //turning off other buttons
            isHeading1Enabled = false
            isHeading2Enabled = false
            isBoldEnabled = false
            isUnderlineEnabled = false
            isBulletEnabled = false
            //event which will tell that the event is performed
            selectionFormatting.send()

            //setting the view for selected
            if (isItalicEnabled) {
                binding.fontView.italic.setBackgroundColor(
                    ContextCompat.getColor(
                        QuickNotepad.appContext,
                        R.color.dialog_text_color
                    )
                )
                binding.fontView.italic.setImageDrawable(
                    ContextCompat.getDrawable(
                        QuickNotepad.appContext,
                        R.drawable.format_italic_black
                    )
                )
            } else {
                binding.fontView.italic.setBackgroundColor(0)
                binding.fontView.italic.setImageDrawable(
                    ContextCompat.getDrawable(
                        QuickNotepad.appContext,
                        R.drawable.format_italic
                    )
                )
            }
            //setting the view for un-selected
            binding.fontView.heading1.setBackgroundColor(0)
            binding.fontView.heading1.setImageDrawable(
                ContextCompat.getDrawable(
                    QuickNotepad.appContext,
                    R.drawable.format_h1
                )
            )
            binding.fontView.heading2.setBackgroundColor(0)
            binding.fontView.heading2.setImageDrawable(
                ContextCompat.getDrawable(
                    QuickNotepad.appContext,
                    R.drawable.format_h2
                )
            )
            binding.fontView.bold.setBackgroundColor(0)
            binding.fontView.bold.setImageDrawable(
                ContextCompat.getDrawable(
                    QuickNotepad.appContext,
                    R.drawable.format_bold
                )
            )
            binding.fontView.underline.setBackgroundColor(0)
            binding.fontView.underline.setImageDrawable(
                ContextCompat.getDrawable(
                    QuickNotepad.appContext,
                    R.drawable.format_underlined
                )
            )
            binding.fontView.bullets.setBackgroundColor(0)
            binding.fontView.bullets.setImageDrawable(
                ContextCompat.getDrawable(
                    QuickNotepad.appContext,
                    R.drawable.format_list_bulleted
                )
            )
        }

        binding.fontView.underline.setOnClickListener {

            //toggle underline
            isUnderlineEnabled = !isUnderlineEnabled

            //turning off other buttons
            isHeading1Enabled = false
            isHeading2Enabled = false
            isBoldEnabled = false
            isItalicEnabled = false
            isBulletEnabled = false
            //event which will tell that the event is performed
            selectionFormatting.send()

            //setting the view for selected
            if (isUnderlineEnabled) {
                binding.fontView.underline.setBackgroundColor(
                    ContextCompat.getColor(
                        QuickNotepad.appContext,
                        R.color.dialog_text_color
                    )
                )
                binding.fontView.underline.setImageDrawable(
                    ContextCompat.getDrawable(
                        QuickNotepad.appContext,
                        R.drawable.format_underlined_black
                    )
                )
            }
            else {
                binding.fontView.underline.setBackgroundColor(0)
                binding.fontView.underline.setImageDrawable(
                    ContextCompat.getDrawable(
                        QuickNotepad.appContext,
                        R.drawable.format_underlined
                    )
                )
            }
            //setting the view for un-selected
            binding.fontView.heading1.setBackgroundColor(0)
            binding.fontView.heading1.setImageDrawable(
                ContextCompat.getDrawable(
                    QuickNotepad.appContext,
                    R.drawable.format_h1
                )
            )
            binding.fontView.heading2.setBackgroundColor(0)
            binding.fontView.heading2.setImageDrawable(
                ContextCompat.getDrawable(
                    QuickNotepad.appContext,
                    R.drawable.format_h2
                )
            )
            binding.fontView.bold.setBackgroundColor(0)
            binding.fontView.bold.setImageDrawable(
                ContextCompat.getDrawable(
                    QuickNotepad.appContext,
                    R.drawable.format_bold
                )
            )
            binding.fontView.italic.setBackgroundColor(0)
            binding.fontView.italic.setImageDrawable(
                ContextCompat.getDrawable(
                    QuickNotepad.appContext,
                    R.drawable.format_italic
                )
            )
            binding.fontView.bullets.setBackgroundColor(0)
            binding.fontView.bullets.setImageDrawable(
                ContextCompat.getDrawable(
                    QuickNotepad.appContext,
                    R.drawable.format_list_bulleted
                )
            )
        }

        binding.fontView.bullets.setOnClickListener {

            //toggle bullets
            isBulletEnabled = !isBulletEnabled

            //turning off other buttons
            isHeading1Enabled = false
            isHeading2Enabled = false
            isBoldEnabled = false
            isItalicEnabled = false
            isUnderlineEnabled = false
            //event which will tell that the event is performed
            selectionFormatting.send()

            //setting the view for selected
            if (isBulletEnabled) {
                binding.fontView.bullets.setBackgroundColor(
                    ContextCompat.getColor(
                        QuickNotepad.appContext,
                        R.color.dialog_text_color
                    )
                )
                binding.fontView.bullets.setImageDrawable(
                    ContextCompat.getDrawable(
                        QuickNotepad.appContext,
                        R.drawable.format_list_bulleted_black
                    )
                )
            } else {
                binding.fontView.bullets.setBackgroundColor(0)
                binding.fontView.bullets.setImageDrawable(
                    ContextCompat.getDrawable(
                        QuickNotepad.appContext,
                        R.drawable.format_list_bulleted
                    )
                )
            }

            //setting the view for un-selected
            binding.fontView.heading1.setBackgroundColor(0)
            binding.fontView.heading1.setImageDrawable(
                ContextCompat.getDrawable(
                    QuickNotepad.appContext,
                    R.drawable.format_h1
                )
            )
            binding.fontView.heading2.setBackgroundColor(0)
            binding.fontView.heading2.setImageDrawable(
                ContextCompat.getDrawable(
                    QuickNotepad.appContext,
                    R.drawable.format_h2
                )
            )
            binding.fontView.bold.setBackgroundColor(0)
            binding.fontView.bold.setImageDrawable(
                ContextCompat.getDrawable(
                    QuickNotepad.appContext,
                    R.drawable.format_bold
                )
            )
            binding.fontView.italic.setBackgroundColor(0)
            binding.fontView.italic.setImageDrawable(
                ContextCompat.getDrawable(
                    QuickNotepad.appContext,
                    R.drawable.format_italic
                )
            )
            binding.fontView.underline.setBackgroundColor(0)
            binding.fontView.underline.setImageDrawable(
                ContextCompat.getDrawable(
                    QuickNotepad.appContext,
                    R.drawable.format_underlined
                )
            )
        }
    }

    override fun selectedColor(color: Int) {

        //setting the selected color
        binding.rootLayout.setBackgroundColor(color)

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
            val gradientDrawable =
                GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, gradientColors)
            binding.rootLayout.background = gradientDrawable

            //save the gradient to preference

            SharedPreference.selectedBackground =
                MainUtils.bitmapToString(MainUtils.gradientToBitmap(gradientDrawable))
            selectedBackground =
                MainUtils.bitmapToString(MainUtils.gradientToBitmap(gradientDrawable))
            Log.i(
                "gradientItemClickedPalette: ",
                MainUtils.bitmapToString(MainUtils.gradientToBitmap(gradientDrawable)).length.toString()
            )

        }
        else {

            binding.rootLayout.setBackgroundColor(
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
        binding.rootLayout.background = wallpaper

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
            binding.rootLayout.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.theme_color
                )
            )
            colorPaletteDialog.dismissNow()
        }
    }

    //color item selected by the user (/***This is font view***/)
    override fun colorItemClicked(color: ColorNote?, position: Int) {

        adapterColors.changeSelectionStatus(position)
        binding.fontView.colorList.scrollToPosition(position)

        //saving the selected color state
        selectedColor = color
        isTextSpannable = true
        //event which will tell that the event is performed
        selectionFormatting.send()
    }

    override fun onDetach() {
        super.onDetach()

        if (viewModelMain.noteImageNotClicked) {
            //normalizing view
            viewModelMain.bottomFragmentItemDetachEventEvent()
            viewModelMain.noteImageNotClicked = false
            viewModelHome.clearSelection()
        }
    }

    override fun noteImageClicked(image: String, position: Int) {

        viewModelMain.noteImageNotClicked = false
        binding.fontView.fontView.visibility = View.GONE

        //Check the type of the image that what type of image is it? Gallery image or canvas image
        if (drawing.isEmpty()) {

            //this means that image loaded in imageNote is loaded from gallery So, open the gallery
            //after take runtime image collection permission and add the image to the parent layout background
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.READ_MEDIA_IMAGES
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    //opening the gallery
                    openGallery()
                } else {
                    // Request the permissions
                    ActivityCompat.requestPermissions(
                        requireActivity(),
                        arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                        REQUEST_CODE_STORAGE
                    )
                }
            } else {

                if (MainUtils.hasStoragePermission(requireContext())) {
                    //opening the gallery
                    openGallery()
                } else {
                    //requesting the Gallery permission
                    MainUtils.getStoragePermission(requireActivity())
                }
            }
        } else {

            if (editNote || viewModelHome.selectedNotes.isEmpty()) {
                //this means user has clicked old drawing image
                //fire the event to handle the view
                viewModelMain.bottomBarItemsListenerEvent("draw_note")
                //this means that image loaded in imageNote is drawn in canvas and the note is new
                //Navigate to the drawing fragment
                val host =
                    requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_main_fragment_container) as NavHostFragment
                val navController = host.navController
                navController.setGraph(R.navigation.main_navigation)
                //navigation
                navController.navigateSafe(AddEditFragmentDirections.actionAddEditFragmentToDrawFragment())
                //fire the event for already created drawing
                viewModelMain.drawingClickEvent(drawing)
            } else {

                //fire the event to handle the view
                viewModelMain.bottomBarItemsListenerEvent("draw_note")
                //this means that image loaded in imageNote is drawn in canvas and the note is new
                //Navigate to the drawing fragment
                val host =
                    requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_main_fragment_container) as NavHostFragment
                val navController = host.navController
                navController.setGraph(R.navigation.main_navigation)
                //navigation
                navController.popBackStack()
                //fire the event for already created drawing
                viewModelMain.drawingClickEvent(drawing)
            }
        }
    }

    override fun openGalleryFromDialog() {
        openGallery()
    }

    companion object {
        private const val STATUS_CHANGE_SNACKBAR_DURATION = 7500
    }
}