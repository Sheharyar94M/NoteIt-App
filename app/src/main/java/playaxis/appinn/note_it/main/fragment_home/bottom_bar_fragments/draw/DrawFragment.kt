package playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.draw

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import playaxis.appinn.note_it.R
import playaxis.appinn.note_it.databinding.FragmentDrawBinding
import playaxis.appinn.note_it.extensions.navigateSafe
import playaxis.appinn.note_it.main.fragment_home.HomeViewModel
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.draw.adapter.ColorListAdapter
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.draw.canvas.DrawingView
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.draw.helper.ColorObserve
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.draw.helper.SelectedPenSize
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.draw.interfaces.TouchEventListeners
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.draw.vectorfinder.VectorChildFinder
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.draw.vectorfinder.VectorDrawableCompat
import playaxis.appinn.note_it.main.utils.Event
import playaxis.appinn.note_it.main.utils.observeEvent
import playaxis.appinn.note_it.main.utils.send
import playaxis.appinn.note_it.main.viewModels.MainViewModel
import playaxis.appinn.note_it.repository.model.entities.ColorNote
import playaxis.appinn.note_it.utils.MainUtils

class DrawFragment : Fragment(), ColorListAdapter.ColorsItemSelectedEvent, TouchEventListeners {

    private lateinit var binding: FragmentDrawBinding

    private lateinit var colors: ArrayList<ColorNote>
    private lateinit var adapterColors: ColorListAdapter

    private var pen1SelectionEvent = MutableLiveData<Event<ColorObserve>>()
    private var pen2SelectionEvent = MutableLiveData<Event<ColorObserve>>()
    private var pen3SelectionEvent = MutableLiveData<Event<ColorObserve>>()

    private var pen1color: ColorObserve? = null
    private var pen2color: ColorObserve? = null
    private var pen3color: ColorObserve? = null

    //pen selection identifier
    private var pen1 = false
    private var pen2 = false
    private var pen3 = false

    //eraser step selected
    private var eraserStep = false

    //eraser sizes
    private var penDefaultSize = 5

    //selected eraser size
    private lateinit var penSizeSelected: SelectedPenSize
    private lateinit var markerSizeSelected: SelectedPenSize
    private lateinit var highlighterSizeSelected: SelectedPenSize
    private val viewModelMain: MainViewModel by activityViewModels()
    private val viewModelHome: HomeViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentDrawBinding.inflate(inflater, container, false)

        normalizeToolbar()
        MainUtils.statusBarColor(
            requireActivity(),
            ContextCompat.getColor(requireContext(), R.color.search_bar_background)
        )

        binding.drawingBoard.touchEventListeners = this

        colors = ArrayList()
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

        adapterColors = ColorListAdapter(
            viewLifecycleOwner,
            this,
            pen1SelectionEvent,
            pen2SelectionEvent,
            pen3SelectionEvent
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //loading new drawing if they exists
        viewModelMain.drawingClickEvent.observeEvent(viewLifecycleOwner) { drawing ->
            binding.drawingBoard.restoreDrawing(drawing)
        }

        //bottom bar listeners
        bottomBarListeners()
        //toolbar listeners
        toolbarListeners()
    }

    private fun toolbarListeners() {

        binding.toolbarLayout.backButton.setOnClickListener {

            val host =
                requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_main_fragment_container) as NavHostFragment
            val navController = host.navController
            navController.setGraph(R.navigation.main_navigation)

            val previousFragId = navController.previousBackStackEntry!!.destination.id
            val previousFragment = navController.graph.findNode(previousFragId)

            //going back and sending the drawn image to the
            if (binding.drawingBoard.getDrawing().isNotEmpty()) {

                //add the drawings in the list and set the list to the adapter in AddEditNoteFragment
                Log.i("drawingBoard: ", binding.drawingBoard.getDrawing()[0].color.toString())
                //Saving the drawings in preference for later use
                viewModelHome.imageNote.add(MainUtils.serializeDrawing(binding.drawingBoard.getDrawing()))

                if (previousFragment!!.id == R.id.addEditFragment ||
                    previousFragment.id == R.id.todoFragment || previousFragment.id == R.id.speechFragment
                ) {

                    requireActivity().runOnUiThread {
                        //If this condition run then the drawing is old because its not coming direct from draw fragment
                        //finishing the current fragment
                        navController.popBackStack()
                        //fire the event to handle the view
                        Handler().postDelayed({
                            //fire the event to handle the view
                            viewModelMain.bottomBarItemsListenerEvent("add_note")
                        }, 500)
                        //set the value that edit note drawing is edited
                        viewModelHome.drawingEdit = true
                    }
                } else {

                    requireActivity().runOnUiThread {

                        //If this condition run then the drawing is new because its coming direct from draw fragment
                        //towards AddEditFragment
                        navController.navigateSafe(DrawFragmentDirections.actionDrawFragmentToAddEditFragment())
                        Handler().postDelayed({
                            //fire the event to handle the view
                            viewModelMain.bottomBarItemsListenerEvent("add_note")
                        }, 500)

                        //set the value that edit note drawing is new
                        viewModelHome.drawingEdit = false
                    }
                }
            } else {
                viewModelHome.imageNote.clear()
                //finishing the current fragment
                navController.popBackStack()
                //set the value that edit note drawing is new
                viewModelHome.drawingEdit = false
            }
        }
        binding.toolbarLayout.undo.setOnClickListener {

            //undo the draw
            binding.drawingBoard.undo()
            if (binding.drawingBoard.imagePathSize() > 0)
                binding.toolbarLayout.redo.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.redo_white
                    )
                )
            else
                binding.toolbarLayout.undo.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.undo
                    )
                )
        }
        binding.toolbarLayout.redo.setOnClickListener {

            //redo the draw
            binding.drawingBoard.redo()

            if (binding.drawingBoard.undoPathSize() > 0)
                binding.toolbarLayout.undo.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.undo_white
                    )
                )
            else {
                binding.toolbarLayout.redo.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.redo
                    )
                )
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility", "NotifyDataSetChanged", "CutPasteId")
    private fun bottomBarListeners() {

        //default views and selections
        defaultPen()
        sizeListeners()

        //pen 1
        binding.drawingUtilityViewLayoutInclude.drawingItems.pen1.setOnClickListener {

            binding.drawingUtilityViewLayoutInclude.colorItems.colorItems.visibility = View.VISIBLE
            binding.drawingUtilityViewLayoutInclude.earserSizes.eraserSizes.visibility = View.VISIBLE

            //initialize the list of the colors
            initializeColorsList()
            //enabling drawing
            binding.drawingBoard.enableDrawing(true)
            //setting drawing pen type
            binding.drawingBoard.selectPenType(DrawingView.HIGHLIGHTER_PEN)

            //setting alpha value
            binding.drawingBoard.setBrushAlpha(50)
            //setting the size and selecting the
            binding.drawingBoard.setSizeForBrush(highlighterSizeSelected.penSize)
            normalizeErasersView(highlighterSizeSelected.image)

            //deselect other items in drawing items
            pen1 = true
            pen2 = false
            pen3 = false
            eraserStep = false
            //remove the clicked item path disabled
            binding.drawingBoard.pathClickEvent = false

            if (pen1color != null) {

                //setting the color and scrolling the list to that position
                selectItem(
                    binding.drawingUtilityViewLayoutInclude.drawingItems.pen1,
                    ContextCompat.getColor(requireContext(), R.color.wave_progress_color),
                    pen1color!!.colorSelected.color.toInt()
                )
                //set the brush color
                binding.drawingBoard.setBrushColor(pen1color!!.colorSelected.color.toInt())
                //this is for the selection of the items
                pen1SelectionEvent.send(pen1color!!)
            }
            else{

                //init pen1color
                pen1color = ColorObserve(colors[0],0)

                //setting the color and scrolling the list to that position
                selectItem(
                    binding.drawingUtilityViewLayoutInclude.drawingItems.pen1,
                    ContextCompat.getColor(requireContext(), R.color.wave_progress_color),
                    pen1color!!.colorSelected.color.toInt()
                )
                //set the brush color
                binding.drawingBoard.setBrushColor(pen1color!!.colorSelected.color.toInt())
                //this is for the selection of the items
                pen1SelectionEvent.send(pen1color!!)
            }

            deSelectItem(binding.drawingUtilityViewLayoutInclude.drawingItems.pen2)
            deSelectItem(binding.drawingUtilityViewLayoutInclude.drawingItems.pen3)
            deSelectItem(binding.drawingUtilityViewLayoutInclude.drawingItems.eraserDraw)
            deSelectItem(binding.drawingUtilityViewLayoutInclude.drawingItems.eraserStep)
        }
        //pen 2
        binding.drawingUtilityViewLayoutInclude.drawingItems.pen2.setOnClickListener {

            binding.drawingUtilityViewLayoutInclude.colorItems.colorItems.visibility = View.VISIBLE
            binding.drawingUtilityViewLayoutInclude.earserSizes.eraserSizes.visibility = View.VISIBLE

            //initialize the list of the colors
            initializeColorsList()
            //enabling drawing
            binding.drawingBoard.enableDrawing(true)

            //setting the alpha value
            binding.drawingBoard.setBrushAlpha(255)
            //setting the size and selecting the
            binding.drawingBoard.setSizeForBrush(markerSizeSelected.penSize)
            normalizeErasersView(markerSizeSelected.image)

            //deselect other items in drawing items
            pen1 = false
            pen2 = true
            pen3 = false
            eraserStep = false
            //remove the clicked item path disabled
            binding.drawingBoard.pathClickEvent = false
            //setting drawing pen type
            binding.drawingBoard.selectPenType(DrawingView.MARKER_PEN)

            //setting the color and scrolling the list to that position
            if (pen2color != null){

                selectItem(
                    binding.drawingUtilityViewLayoutInclude.drawingItems.pen2,
                    ContextCompat.getColor(requireContext(), R.color.wave_progress_color),
                    pen2color!!.colorSelected.color.toInt()
                )
                //set the brush color
                binding.drawingBoard.setBrushColor(pen2color!!.colorSelected.color.toInt())
                //this is for the selection of the items
                pen2SelectionEvent.send(pen2color!!)
            }
            else{

                //init pen2color
                pen2color = ColorObserve(colors[0],0)

                selectItem(
                    binding.drawingUtilityViewLayoutInclude.drawingItems.pen2,
                    ContextCompat.getColor(requireContext(), R.color.wave_progress_color),
                    pen2color!!.colorSelected.color.toInt()
                )
                //set the brush color
                binding.drawingBoard.setBrushColor(pen2color!!.colorSelected.color.toInt())
                //this is for the selection of the items
                pen2SelectionEvent.send(pen2color!!)
            }

            deSelectItem(binding.drawingUtilityViewLayoutInclude.drawingItems.pen1)
            deSelectItem(binding.drawingUtilityViewLayoutInclude.drawingItems.pen3)
            deSelectItem(binding.drawingUtilityViewLayoutInclude.drawingItems.eraserDraw)
            deSelectItem(binding.drawingUtilityViewLayoutInclude.drawingItems.eraserStep)
        }
        //pen 3
        binding.drawingUtilityViewLayoutInclude.drawingItems.pen3.setOnClickListener {

            binding.drawingUtilityViewLayoutInclude.colorItems.colorItems.visibility = View.VISIBLE
            binding.drawingUtilityViewLayoutInclude.earserSizes.eraserSizes.visibility = View.VISIBLE

            //initialize the list of the colors
            initializeColorsList()
            //enabling drawing
            binding.drawingBoard.enableDrawing(true)
            //setting the alpha value
            binding.drawingBoard.setBrushAlpha(255)
            //setting the size and selecting the
            binding.drawingBoard.setSizeForBrush(penSizeSelected.penSize)
            normalizeErasersView(penSizeSelected.image)

            //deselect other items in drawing items
            pen1 = false
            pen2 = false
            pen3 = true
            eraserStep = false
            //remove the clicked item path disabled
            binding.drawingBoard.pathClickEvent = false
            //setting drawing pen type
            binding.drawingBoard.selectPenType(DrawingView.INK_PEN)

            if (pen3color != null){

                //setting the color and scrolling the list to that position
                selectItem(
                    binding.drawingUtilityViewLayoutInclude.drawingItems.pen3,
                    ContextCompat.getColor(requireContext(), R.color.wave_progress_color),
                    pen3color!!.colorSelected.color.toInt()
                )
                //set the brush color
                binding.drawingBoard.setBrushColor(pen3color!!.colorSelected.color.toInt())
                //this is for the selection of the items
                pen3SelectionEvent.send(pen3color!!)
            }
            else{

                //init pen3color
                pen3color = ColorObserve(colors[0],0)

                //setting the color and scrolling the list to that position
                selectItem(
                    binding.drawingUtilityViewLayoutInclude.drawingItems.pen3,
                    ContextCompat.getColor(requireContext(), R.color.wave_progress_color),
                    pen3color!!.colorSelected.color.toInt()
                )
                //set the brush color
                binding.drawingBoard.setBrushColor(pen3color!!.colorSelected.color.toInt())
                //this is for the selection of the items
                pen3SelectionEvent.send(pen3color!!)
            }

            deSelectItem(binding.drawingUtilityViewLayoutInclude.drawingItems.pen1)
            deSelectItem(binding.drawingUtilityViewLayoutInclude.drawingItems.pen2)
            deSelectItem(binding.drawingUtilityViewLayoutInclude.drawingItems.eraserDraw)
            deSelectItem(binding.drawingUtilityViewLayoutInclude.drawingItems.eraserStep)
        }

        //eraser step
        binding.drawingUtilityViewLayoutInclude.drawingItems.eraserDraw.setOnClickListener {

            //make color list invisible and size visible
            binding.drawingUtilityViewLayoutInclude.earserSizes.eraserSizes.visibility = View.GONE
            binding.drawingUtilityViewLayoutInclude.colorItems.colorItems.visibility = View.GONE

            //select the rubber item
            selectItem(
                binding.drawingUtilityViewLayoutInclude.drawingItems.eraserDraw,
                ContextCompat.getColor(requireContext(), R.color.wave_progress_color),
                0
            )

            //deselect other items
            deSelectItem(binding.drawingUtilityViewLayoutInclude.drawingItems.pen1)
            deSelectItem(binding.drawingUtilityViewLayoutInclude.drawingItems.pen2)
            deSelectItem(binding.drawingUtilityViewLayoutInclude.drawingItems.pen3)
            deSelectItem(binding.drawingUtilityViewLayoutInclude.drawingItems.eraserStep)

            eraserStep = true
            //disabling drawing
            binding.drawingBoard.enableDrawing(false)

            //remove the clicked item path
            binding.drawingBoard.pathClickEvent = true
        }
        //eraser draw (rubber)
        binding.drawingUtilityViewLayoutInclude.drawingItems.eraserStep.setOnClickListener {

            //make color list invisible and size visible
            binding.drawingUtilityViewLayoutInclude.earserSizes.eraserSizes.visibility = View.VISIBLE
            binding.drawingUtilityViewLayoutInclude.colorItems.colorItems.visibility = View.GONE
            //disabling drawing
            binding.drawingBoard.enableDrawing(true)

            //select the rubber item
            selectItem(
                binding.drawingUtilityViewLayoutInclude.drawingItems.eraserStep,
                ContextCompat.getColor(requireContext(), R.color.wave_progress_color),
                0
            )

            //deselect other items
            deSelectItem(binding.drawingUtilityViewLayoutInclude.drawingItems.pen1)
            deSelectItem(binding.drawingUtilityViewLayoutInclude.drawingItems.pen2)
            deSelectItem(binding.drawingUtilityViewLayoutInclude.drawingItems.pen3)
            deSelectItem(binding.drawingUtilityViewLayoutInclude.drawingItems.eraserDraw)

            //apply eraser
            binding.drawingBoard.erase(Color.WHITE)
            if (pen1)
                binding.drawingBoard.setSizeForBrush(highlighterSizeSelected.penSize)
            else if (pen2)
                binding.drawingBoard.setSizeForBrush(markerSizeSelected.penSize)
            else if (pen3)
                binding.drawingBoard.setSizeForBrush(penSizeSelected.penSize)

            eraserStep = false
        }
        //clear canvas
        binding.drawingUtilityViewLayoutInclude.clearCanvas.setOnClickListener {
            //clearing the canvas
            binding.drawingBoard.clearDrawingBoard()
        }
    }

    private fun defaultPen() {

        binding.drawingUtilityViewLayoutInclude.colorItems.colorItems.visibility = View.VISIBLE
        binding.drawingUtilityViewLayoutInclude.earserSizes.eraserSizes.visibility = View.VISIBLE

        //initialize the list of the colors
        initializeColorsList()
        //enabling drawing
        binding.drawingBoard.enableDrawing(true)

        //setting the alpha value
        binding.drawingBoard.setBrushAlpha(255)

        //initialize the defaults
        penSizeSelected = SelectedPenSize(penDefaultSize,binding.drawingUtilityViewLayoutInclude.earserSizes.eraser2)
        markerSizeSelected = SelectedPenSize(penDefaultSize,binding.drawingUtilityViewLayoutInclude.earserSizes.eraser2)
        highlighterSizeSelected = SelectedPenSize(penDefaultSize,binding.drawingUtilityViewLayoutInclude.earserSizes.eraser2)

        //set the default color and size
        binding.drawingBoard.setBrushColor(colors[0].color.toInt())
        binding.drawingBoard.setSizeForBrush(penDefaultSize)
        normalizeErasersView(binding.drawingUtilityViewLayoutInclude.earserSizes.eraser2)

        selectItem(
            binding.drawingUtilityViewLayoutInclude.drawingItems.pen3,
            ContextCompat.getColor(requireContext(), R.color.wave_progress_color),
            colors[0].color.toInt()
        )

        //deselect other items in drawing items
        pen1 = false
        pen2 = false
        pen3 = true
        eraserStep = false
        //remove the clicked item path disabled
        binding.drawingBoard.pathClickEvent = false
        deSelectItem(binding.drawingUtilityViewLayoutInclude.drawingItems.pen1)
        deSelectItem(binding.drawingUtilityViewLayoutInclude.drawingItems.pen2)
        deSelectItem(binding.drawingUtilityViewLayoutInclude.drawingItems.eraserDraw)
        deSelectItem(binding.drawingUtilityViewLayoutInclude.drawingItems.eraserStep)
    }

    private fun sizeListeners() {

        binding.drawingUtilityViewLayoutInclude.earserSizes.eraser1.setOnClickListener {
            //set the size to the eraser and pen nibs
            normalizeErasersView(binding.drawingUtilityViewLayoutInclude.earserSizes.eraser1)
            binding.drawingBoard.setSizeForBrush(2)
            if (pen1)
                highlighterSizeSelected = SelectedPenSize(2,binding.drawingUtilityViewLayoutInclude.earserSizes.eraser1)
            else if (pen2)
                markerSizeSelected = SelectedPenSize(2,binding.drawingUtilityViewLayoutInclude.earserSizes.eraser1)
            else if (pen3)
                penSizeSelected = SelectedPenSize(2,binding.drawingUtilityViewLayoutInclude.earserSizes.eraser1)
        }

        binding.drawingUtilityViewLayoutInclude.earserSizes.eraser2.setOnClickListener {
            //set the size to the eraser and pen nibs
            normalizeErasersView(binding.drawingUtilityViewLayoutInclude.earserSizes.eraser2)
            binding.drawingBoard.setSizeForBrush(5)
            if (pen1)
                highlighterSizeSelected = SelectedPenSize(5,binding.drawingUtilityViewLayoutInclude.earserSizes.eraser2)
            else if (pen2)
                markerSizeSelected = SelectedPenSize(5,binding.drawingUtilityViewLayoutInclude.earserSizes.eraser2)
            else if (pen3)
                penSizeSelected = SelectedPenSize(5,binding.drawingUtilityViewLayoutInclude.earserSizes.eraser2)
        }

        binding.drawingUtilityViewLayoutInclude.earserSizes.eraser3.setOnClickListener {
            //set the size to the eraser and pen nibs
            normalizeErasersView(binding.drawingUtilityViewLayoutInclude.earserSizes.eraser3)
            binding.drawingBoard.setSizeForBrush(8)
            if (pen1)
                highlighterSizeSelected = SelectedPenSize(8,binding.drawingUtilityViewLayoutInclude.earserSizes.eraser3)
            else if (pen2)
                markerSizeSelected = SelectedPenSize(8,binding.drawingUtilityViewLayoutInclude.earserSizes.eraser3)
            else if (pen3)
                penSizeSelected = SelectedPenSize(8,binding.drawingUtilityViewLayoutInclude.earserSizes.eraser3)
        }

        binding.drawingUtilityViewLayoutInclude.earserSizes.eraser4.setOnClickListener {
            //set the size to the eraser and pen nibs
            normalizeErasersView(binding.drawingUtilityViewLayoutInclude.earserSizes.eraser4)
            binding.drawingBoard.setSizeForBrush(12)
            if (pen1)
                highlighterSizeSelected = SelectedPenSize(12,binding.drawingUtilityViewLayoutInclude.earserSizes.eraser4)
            else if (pen2)
                markerSizeSelected = SelectedPenSize(12,binding.drawingUtilityViewLayoutInclude.earserSizes.eraser4)
            else if (pen3)
                penSizeSelected = SelectedPenSize(12,binding.drawingUtilityViewLayoutInclude.earserSizes.eraser4)
        }

        binding.drawingUtilityViewLayoutInclude.earserSizes.eraser5.setOnClickListener {
            //set the size to the eraser and pen nibs
            normalizeErasersView(binding.drawingUtilityViewLayoutInclude.earserSizes.eraser5)
            binding.drawingBoard.setSizeForBrush(16)
            if (pen1)
                highlighterSizeSelected = SelectedPenSize(16,binding.drawingUtilityViewLayoutInclude.earserSizes.eraser5)
            else if (pen2)
                markerSizeSelected = SelectedPenSize(16,binding.drawingUtilityViewLayoutInclude.earserSizes.eraser5)
            else if (pen3)
                penSizeSelected = SelectedPenSize(16,binding.drawingUtilityViewLayoutInclude.earserSizes.eraser5)
        }

        binding.drawingUtilityViewLayoutInclude.earserSizes.eraser6.setOnClickListener {
            //set the size to the eraser and pen nibs
            normalizeErasersView(binding.drawingUtilityViewLayoutInclude.earserSizes.eraser6)
            binding.drawingBoard.setSizeForBrush(20)
            if (pen1)
                highlighterSizeSelected = SelectedPenSize(20,binding.drawingUtilityViewLayoutInclude.earserSizes.eraser6)
            else if (pen2)
                markerSizeSelected = SelectedPenSize(20,binding.drawingUtilityViewLayoutInclude.earserSizes.eraser6)
            else if (pen3)
                penSizeSelected = SelectedPenSize(20,binding.drawingUtilityViewLayoutInclude.earserSizes.eraser6)
        }

        binding.drawingUtilityViewLayoutInclude.earserSizes.eraser7.setOnClickListener {
            //set the size to the eraser and pen nibs
            normalizeErasersView(binding.drawingUtilityViewLayoutInclude.earserSizes.eraser7)
            binding.drawingBoard.setSizeForBrush(24)
            if (pen1)
                highlighterSizeSelected = SelectedPenSize(24,binding.drawingUtilityViewLayoutInclude.earserSizes.eraser7)
            else if (pen2)
                markerSizeSelected = SelectedPenSize(24,binding.drawingUtilityViewLayoutInclude.earserSizes.eraser7)
            else if (pen3)
                penSizeSelected = SelectedPenSize(24,binding.drawingUtilityViewLayoutInclude.earserSizes.eraser7)
        }

        binding.drawingUtilityViewLayoutInclude.earserSizes.eraser8.setOnClickListener {
            //set the size to the eraser and pen nibs
            normalizeErasersView(binding.drawingUtilityViewLayoutInclude.earserSizes.eraser8)
            binding.drawingBoard.setSizeForBrush(28)
            if (pen1)
                highlighterSizeSelected = SelectedPenSize(28,binding.drawingUtilityViewLayoutInclude.earserSizes.eraser8)
            else if (pen2)
                markerSizeSelected = SelectedPenSize(28,binding.drawingUtilityViewLayoutInclude.earserSizes.eraser8)
            else if (pen3)
                penSizeSelected = SelectedPenSize(28,binding.drawingUtilityViewLayoutInclude.earserSizes.eraser8)
        }

        binding.drawingUtilityViewLayoutInclude.earserSizes.eraser9.setOnClickListener {
            //set the size to the eraser and pen nibs
            normalizeErasersView(binding.drawingUtilityViewLayoutInclude.earserSizes.eraser9)
            binding.drawingBoard.setSizeForBrush(32)
            if (pen1)
                highlighterSizeSelected = SelectedPenSize(32,binding.drawingUtilityViewLayoutInclude.earserSizes.eraser9)
            else if (pen2)
                markerSizeSelected = SelectedPenSize(32,binding.drawingUtilityViewLayoutInclude.earserSizes.eraser9)
            else if (pen3)
                penSizeSelected = SelectedPenSize(32,binding.drawingUtilityViewLayoutInclude.earserSizes.eraser9)
        }
    }

    private fun normalizeErasersView(eraser: AppCompatImageView) {
        when (eraser.id) {
            R.id.eraser1 -> {

                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser1.visibility =
                    View.VISIBLE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser2.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser3.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser4.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser5.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser6.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser7.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser8.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser9.visibility =
                    View.GONE
            }

            R.id.eraser2 -> {

                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser1.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser2.visibility =
                    View.VISIBLE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser3.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser4.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser5.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser6.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser7.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser8.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser9.visibility =
                    View.GONE
            }

            R.id.eraser3 -> {

                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser1.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser2.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser3.visibility =
                    View.VISIBLE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser4.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser5.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser6.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser7.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser8.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser9.visibility =
                    View.GONE
            }

            R.id.eraser4 -> {

                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser1.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser2.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser3.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser4.visibility =
                    View.VISIBLE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser5.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser6.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser7.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser8.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser9.visibility =
                    View.GONE
            }

            R.id.eraser5 -> {

                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser1.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser2.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser3.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser4.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser5.visibility =
                    View.VISIBLE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser6.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser7.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser8.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser9.visibility =
                    View.GONE
            }

            R.id.eraser6 -> {

                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser1.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser2.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser3.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser4.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser5.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser6.visibility =
                    View.VISIBLE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser7.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser8.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser9.visibility =
                    View.GONE
            }

            R.id.eraser7 -> {

                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser1.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser2.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser3.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser4.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser5.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser6.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser7.visibility =
                    View.VISIBLE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser8.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser9.visibility =
                    View.GONE
            }

            R.id.eraser8 -> {

                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser1.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser2.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser3.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser4.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser5.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser6.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser7.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser8.visibility =
                    View.VISIBLE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser9.visibility =
                    View.GONE
            }

            R.id.eraser9 -> {

                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser1.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser2.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser3.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser4.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser5.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser6.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser7.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser8.visibility =
                    View.GONE
                binding.drawingUtilityViewLayoutInclude.earserSizes.selectionIconEraser9.visibility =
                    View.VISIBLE
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initializeColorsList() {

        binding.drawingUtilityViewLayoutInclude.colorItems.colorList.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        binding.drawingUtilityViewLayoutInclude.colorItems.colorList.setHasFixedSize(false)
        binding.drawingUtilityViewLayoutInclude.colorItems.colorList.adapter = adapterColors

        //setting data to the adapters
        adapterColors.setColors(colors)
        adapterColors.notifyDataSetChanged()
    }

    private fun selectItem(item: AppCompatImageView, selectionDefault: Int, userSelected: Int) {

        item.isSelected = true

        when (item.id) {
            R.id.pen1 -> {

                val vector =
                    VectorChildFinder(
                        requireContext(),
                        R.drawable.pen1,
                        item
                    )

                val upperNib: VectorDrawableCompat.VFullPath = vector.findPathByName("upper_nib")
                val lowerNib: VectorDrawableCompat.VFullPath = vector.findPathByName("lower_nib")
                upperNib.fillColor = userSelected
                lowerNib.fillColor = selectionDefault

            }

            R.id.pen2 -> {

                val vector =
                    VectorChildFinder(
                        requireContext(),
                        R.drawable.pen2,
                        item
                    )

                val upperNib: VectorDrawableCompat.VFullPath = vector.findPathByName("upper_nib")
                val lowerNib: VectorDrawableCompat.VFullPath = vector.findPathByName("lower_nib")
                upperNib.fillColor = userSelected
                lowerNib.fillColor = selectionDefault
            }

            R.id.pen3 -> {

                val vector =
                    VectorChildFinder(
                        requireContext(),
                        R.drawable.pen3,
                        item
                    )

                val upperNib: VectorDrawableCompat.VFullPath = vector.findPathByName("upper_nib")
                val lowerNib: VectorDrawableCompat.VFullPath = vector.findPathByName("lower_nib")
                upperNib.fillColor = userSelected
                lowerNib.fillColor = selectionDefault
            }

            R.id.eraser_draw -> {

                val vector =
                    VectorChildFinder(
                        requireContext(),
                        R.drawable.eraser_draw,
                        item
                    )
                val eraser1: VectorDrawableCompat.VFullPath = vector.findPathByName("eraser1")
                eraser1.fillColor = selectionDefault
            }

            R.id.eraser_step -> {

                val vector =
                    VectorChildFinder(
                        requireContext(),
                        R.drawable.erase_step,
                        item
                    )
                val eraser1: VectorDrawableCompat.VFullPath = vector.findPathByName("eraser2")
                eraser1.fillColor = selectionDefault
            }
        }
    }

    private fun deSelectItem(item: AppCompatImageView) {

        item.isSelected = false

        when (item.id) {
            R.id.pen1 -> {

                val vector =
                    VectorChildFinder(
                        requireContext(),
                        R.drawable.pen1,
                        item
                    )

                val upperNib: VectorDrawableCompat.VFullPath = vector.findPathByName("upper_nib")
                val lowerNib: VectorDrawableCompat.VFullPath = vector.findPathByName("lower_nib")

                upperNib.fillColor =
                    ContextCompat.getColor(requireContext(), R.color.dialog_text_color)
                lowerNib.fillColor =
                    ContextCompat.getColor(requireContext(), R.color.dialog_text_color)
            }

            R.id.pen2 -> {

                val vector =
                    VectorChildFinder(
                        requireContext(),
                        R.drawable.pen2,
                        item
                    )

                val upperNib: VectorDrawableCompat.VFullPath = vector.findPathByName("upper_nib")
                val lowerNib: VectorDrawableCompat.VFullPath = vector.findPathByName("lower_nib")
                upperNib.fillColor =
                    ContextCompat.getColor(requireContext(), R.color.dialog_text_color)
                lowerNib.fillColor =
                    ContextCompat.getColor(requireContext(), R.color.dialog_text_color)
            }

            R.id.pen3 -> {

                val vector =
                    VectorChildFinder(
                        requireContext(),
                        R.drawable.pen3,
                        item
                    )

                val upperNib: VectorDrawableCompat.VFullPath = vector.findPathByName("upper_nib")
                val lowerNib: VectorDrawableCompat.VFullPath = vector.findPathByName("lower_nib")
                upperNib.fillColor =
                    ContextCompat.getColor(requireContext(), R.color.dialog_text_color)
                lowerNib.fillColor =
                    ContextCompat.getColor(requireContext(), R.color.dialog_text_color)
            }

            R.id.eraser_draw -> {

                val vector =
                    VectorChildFinder(
                        requireContext(),
                        R.drawable.eraser_draw,
                        item
                    )
                val eraser1: VectorDrawableCompat.VFullPath = vector.findPathByName("eraser1")
                eraser1.fillColor =
                    ContextCompat.getColor(requireContext(), R.color.dialog_text_color)
            }

            R.id.eraser_draw -> {

                val vector =
                    VectorChildFinder(
                        requireContext(),
                        R.drawable.erase_step,
                        item
                    )
                val eraser1: VectorDrawableCompat.VFullPath = vector.findPathByName("eraser2")
                eraser1.fillColor =
                    ContextCompat.getColor(requireContext(), R.color.dialog_text_color)
            }
        }
    }

    private fun normalizeToolbar() {
        binding.toolbarLayout.toolbarLayout.visibility = View.VISIBLE
        binding.toolbarLayout.lockButton.visibility = View.GONE
        binding.toolbarLayout.colorButton.visibility = View.GONE
        binding.toolbarLayout.pinButton.visibility = View.GONE
        binding.toolbarLayout.reminderButton.visibility = View.GONE
        binding.toolbarLayout.moveToFolderButtonAction.visibility = View.GONE
        binding.toolbarLayout.labelButton.visibility = View.GONE
        binding.toolbarLayout.archiveButton.visibility = View.GONE
        binding.toolbarLayout.menuIcon.visibility = View.GONE
        binding.toolbarLayout.restoreAction.visibility = View.GONE
        binding.toolbarLayout.deleteAction.visibility = View.GONE
    }

    override fun colorItemClicked(color: ColorNote, position: Int) {

        adapterColors.changeSelectionStatus(position)
        binding.drawingUtilityViewLayoutInclude.colorItems.colorList.scrollToPosition(position)

        //set the changed color value to set the color to the image
        if (pen1) {
            //saving selected color
            pen1color = ColorObserve(color, position)
            //selecting item
            selectItem(
                binding.drawingUtilityViewLayoutInclude.drawingItems.pen1,
                ContextCompat.getColor(requireContext(), R.color.wave_progress_color),
                color.color.toInt()
            )
            //set pen color
            binding.drawingBoard.setBrushColor(color.color.toInt())
        }
        else if (pen2) {
            //saving selected color
            pen2color = ColorObserve(color, position)
            selectItem(
                binding.drawingUtilityViewLayoutInclude.drawingItems.pen2,
                ContextCompat.getColor(requireContext(), R.color.wave_progress_color),
                color.color.toInt()
            )
            //set pen color
            binding.drawingBoard.setBrushColor(color.color.toInt())
        }
        else if (pen3) {
            //saving selected color
            pen3color = ColorObserve(color, position)
            selectItem(
                binding.drawingUtilityViewLayoutInclude.drawingItems.pen3,
                ContextCompat.getColor(requireContext(), R.color.wave_progress_color),
                color.color.toInt()
            )
            //set pen color
            binding.drawingBoard.setBrushColor(color.color.toInt())
        }
    }

    override fun onTouchEvent(event: MotionEvent?) {
        binding.toolbarLayout.undo.setImageDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.undo_white
            )
        )

        when (event!!.action) {

            MotionEvent.ACTION_DOWN -> {
                binding.motionLayout.transitionToState(R.id.end)
            }

            MotionEvent.ACTION_UP -> {
                binding.motionLayout.transitionToState(R.id.start)
            }
        }
    }

    override fun onDetach() {
        super.onDetach()

        val host =
            requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_main_fragment_container) as NavHostFragment
        val navController = host.navController
        navController.setGraph(R.navigation.main_navigation)

        try {
            val previousFragId = navController.previousBackStackEntry!!.destination.id
            val previousFragment = navController.graph.findNode(previousFragId)

            if (previousFragment == null)
                viewModelMain.bottomFragmentItemDetachEventEvent()
        } catch (e: Exception) {
            viewModelMain.bottomFragmentItemDetachEventEvent()
            Log.i("onDetach: ", e.message.toString())
        }

    }
}