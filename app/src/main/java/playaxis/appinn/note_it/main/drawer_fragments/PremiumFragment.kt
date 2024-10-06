package playaxis.appinn.note_it.main.drawer_fragments

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import playaxis.appinn.note_it.R
import playaxis.appinn.note_it.databinding.FragmentPremiumBinding

class PremiumFragment : DialogFragment() {

    private lateinit var binding: FragmentPremiumBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.TransparentDialogFragment)

        setCancelable(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentPremiumBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.closeButton.setOnClickListener {
            this.dismissNow()
        }
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
            val dialogHeight = (screenHeight * 0.9).toInt()

            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(dialog.window?.attributes)
            layoutParams.width = dialogWidth
            layoutParams.height = dialogHeight
            layoutParams.gravity = Gravity.CENTER
            dialog.window?.attributes = layoutParams
        }
    }
}