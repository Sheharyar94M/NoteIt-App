package playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.dialogs

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import playaxis.appinn.note_it.R
import playaxis.appinn.note_it.application.QuickNotepad
import playaxis.appinn.note_it.databinding.ColorPaletteDialogViewBinding
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.dialogs.adapters.ColorsListAdapter
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.dialogs.adapters.GradientColorsListAdapter
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.dialogs.adapters.WallpapersListAdapter
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.dialogs.interfaces.ColorSelectedInterface
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.dialogs.interfaces.GradientSelectedInterface
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.dialogs.interfaces.WallpaperSelectedInterface
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.note.viewmodels.AddEditNoteViewModel
import playaxis.appinn.note_it.main.viewModels.viewModel
import playaxis.appinn.note_it.repository.model.entities.GradientNote
import playaxis.appinn.note_it.utils.MainUtils
import javax.inject.Inject

class ColorPaletteDialog(
    private var colorSelected: ColorSelectedInterface,
    private var gradientSelected: GradientSelectedInterface,
    private var wallpaperSelected: WallpaperSelectedInterface,
    private var closeDialog: CloseDialogEvent
): DialogFragment(), ColorsListAdapter.ColorItemCLickEvent,
    WallpapersListAdapter.WallpaperItemCLickEvent,
    GradientColorsListAdapter.GradientItemCLickEvent {

    private lateinit var binding: ColorPaletteDialogViewBinding

    @Inject
    lateinit var viewModelFactory: AddEditNoteViewModel.Factory
    val viewModel by viewModel { viewModelFactory.create(it) }


    private lateinit var colorAdapter: ColorsListAdapter
    private lateinit var gradientAdapter: GradientColorsListAdapter
    private lateinit var wallpapersListAdapter: WallpapersListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NORMAL, R.style.TransparentDialogFragment)
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        //injection initialization
        (QuickNotepad.appContext as QuickNotepad).appComponent.inject(this)

        binding = ColorPaletteDialogViewBinding.inflate(inflater, container, false)

        //adapters
        colorAdapter = ColorsListAdapter(this)
        gradientAdapter = GradientColorsListAdapter(this)
        wallpapersListAdapter = WallpapersListAdapter(this)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //default selection
        defaultSelection()

        //color setup
        binding.colorButtonDialog.setOnClickListener {

            //set the button view and set data to the recyclerview
            normalizeViews("color")
            //getting data
            viewModel.getColors().observe(this){colors ->

                Log.i("colors: ",colors.toString())
                //set recyclerview of the colors and set the data
                colorAdapter.setColorList(colors)

                binding.colorsGradientsWallpapersList.layoutManager = GridLayoutManager(requireContext(),2,GridLayoutManager.HORIZONTAL,false)
                binding.colorsGradientsWallpapersList.setHasFixedSize(true)
                binding.colorsGradientsWallpapersList.adapter =colorAdapter
            }
        }

        //gradient setup
        binding.gradientButtonDialog.setOnClickListener {

            //set the button view and set data to the recyclerview
            normalizeViews("gradient")
            //getting data
            viewModel.getGradients().observe(this){gradients ->

                //set recyclerview of the colors and set the data
                gradientAdapter.setGradient1List(gradients)

                binding.colorsGradientsWallpapersList.layoutManager = GridLayoutManager(requireContext(),2,GridLayoutManager.HORIZONTAL,false)
                binding.colorsGradientsWallpapersList.setHasFixedSize(true)
                binding.colorsGradientsWallpapersList.adapter = gradientAdapter
            }
        }

        //wallpaper setup
        binding.wallpaperButtonDialog.setOnClickListener {

            //set the button view and set data to the recyclerview
            normalizeViews("wallpaper")
            //getting data
            viewModel.getWallpapers().observe(this){wallpapers ->

                //set recyclerview of the colors and set the data
                wallpapersListAdapter.setWallpaperList(wallpapers)

                binding.colorsGradientsWallpapersList.layoutManager = GridLayoutManager(requireContext(),2,GridLayoutManager.HORIZONTAL,false)
                binding.colorsGradientsWallpapersList.setHasFixedSize(true)
                binding.colorsGradientsWallpapersList.adapter = wallpapersListAdapter
            }
        }

        //canceling the dialog
        binding.cancelButton.setOnClickListener {
            closeDialog.closeDialog(false)
        }

        //setting is already applied and now simply remove the dialog
        binding.saveButton.setOnClickListener {
            closeDialog.closeDialog(true)
        }
    }

    private fun defaultSelection() {

        //set the button view and set data to the recyclerview
        normalizeViews("color")
        //getting data
        viewModel.getColors().observe(this){colors ->

            Log.i("colors: ",colors.toString())
            //set recyclerview of the colors and set the data
            colorAdapter.setColorList(colors)

            binding.colorsGradientsWallpapersList.layoutManager = GridLayoutManager(requireContext(),2,GridLayoutManager.HORIZONTAL,false)
            binding.colorsGradientsWallpapersList.setHasFixedSize(true)
            binding.colorsGradientsWallpapersList.adapter =colorAdapter
        }
    }

    private fun normalizeViews(name:String) {

        when(name){

            "color" ->{
                binding.colorButtonDialog.setTextColor(ContextCompat.getColor(requireContext(), R.color.selected_tab_color))
                binding.colorButtonDialog.background = ContextCompat.getDrawable(requireContext(),R.drawable.selected_indicator_dialog_view)
                binding.gradientButtonDialog.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                binding.gradientButtonDialog.setBackgroundResource(0)
                binding.wallpaperButtonDialog.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                binding.wallpaperButtonDialog.setBackgroundResource(0)
            }
            "gradient" ->{
                binding.colorButtonDialog.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                binding.colorButtonDialog.setBackgroundResource(0)
                binding.gradientButtonDialog.setTextColor(ContextCompat.getColor(requireContext(), R.color.selected_tab_color))
                binding.gradientButtonDialog.background = ContextCompat.getDrawable(requireContext(),R.drawable.selected_indicator_dialog_view)
                binding.wallpaperButtonDialog.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                binding.wallpaperButtonDialog.setBackgroundResource(0)
            }
            "wallpaper" ->{
                binding.colorButtonDialog.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                binding.colorButtonDialog.setBackgroundResource(0)
                binding.gradientButtonDialog.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                binding.gradientButtonDialog.setBackgroundResource(0)
                binding.wallpaperButtonDialog.setTextColor(ContextCompat.getColor(requireContext(), R.color.selected_tab_color))
                binding.wallpaperButtonDialog.background = ContextCompat.getDrawable(requireContext(),R.drawable.selected_indicator_dialog_view)
            }
        }
    }

    override fun colorItemClicked(color: Int, color_position: Int) {

        //selected color is here and set the background of the layout
        colorAdapter.changeSelectionStatus(color_position)
        colorSelected.selectedColor(color)

        Log.i("colorItemClickedPalette: ", color.toString().length.toString())
    }
    override fun gradientItemClicked(gradient: GradientNote?, gradient_position: Int) {
        //selected gradient colors are here and set the background of the layout
        gradientAdapter.changeSelectionStatus(gradient_position)
        gradientSelected.selectedGradient(gradient)

    }
    override fun wallpaperItemClicked(wallpaper: Drawable?, wallpaper_position: Int) {

        //selected wallpaper is here and set the wallpaper
        wallpapersListAdapter.changeSelectionStatus(wallpaper_position)
        wallpaperSelected.selectedWallpaper(wallpaper)

        Log.i("wallpaperItemClickedPalette: ", MainUtils.drawableToString(wallpaper).length.toString())
    }

    override fun onResume() {
        super.onResume()

        setDialogSize()
    }

    //setting dialog size to maximum
    private fun setDialogSize() {
        dialog?.let { dialog ->
            val windowManager = requireActivity().windowManager
            val display = windowManager.defaultDisplay
            val size = android.graphics.Point()
            display.getSize(size)
            val screenWidth = size.x
            val screenHeight = size.y

            val dialogWidth = (screenWidth * 0.9).toInt()
            val dialogHeight = (screenHeight * 0.39).toInt()

            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(dialog.window?.attributes)
            layoutParams.width = dialogWidth
            layoutParams.height = dialogHeight
            dialog.window?.attributes = layoutParams
            dialog.window?.setGravity(Gravity.BOTTOM)
        }
    }

    interface CloseDialogEvent{
        fun closeDialog(save: Boolean)
    }
}