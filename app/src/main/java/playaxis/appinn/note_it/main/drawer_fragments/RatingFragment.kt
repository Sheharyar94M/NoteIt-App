package playaxis.appinn.note_it.main.drawer_fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import playaxis.appinn.note_it.R
import playaxis.appinn.note_it.databinding.FragmentRatingBinding


class RatingFragment : DialogFragment() {

    private lateinit var binding: FragmentRatingBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.TransparentDialogFragment)

        setCancelable(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentRatingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Create a SpannableStringBuilder
        val builder = SpannableStringBuilder()

        // Append the text with different colors
        val color1: Int = ContextCompat.getColor(requireContext(), R.color.dialog_text_color)
        val text1 = "We are sure that you like our app, and want to appreciate us! Rate us "
        builder.append(text1)
        builder.setSpan(
            ForegroundColorSpan(color1),
            builder.length - text1.length,
            builder.length,
            0
        )

        val color2: Int = android.graphics.Color.parseColor("#F6B93B")
        val text2 = "5 stars!"
        builder.append(text2)
        builder.setSpan(
            ForegroundColorSpan(color2),
            builder.length - text2.length,
            builder.length,
            0
        )

        binding.contentText.text = builder

        //setting up rating bar
        val ratingBarB = binding.ratingBar
        ratingBarB.setNumStars(5)
        ratingBarB.setMinimumStars(0F)
        ratingBarB.stepSize = 1f
        ratingBarB.setIsIndicator(false)
        ratingBarB.isClickable = true
        ratingBarB.isScrollable = true
        ratingBarB.isClearRatingEnabled = true
        ratingBarB.setEmptyDrawableRes(R.drawable.empty_star)
        ratingBarB.setFilledDrawableRes(R.drawable.filled_star)

        //get the rating
        ratingBarB.setOnRatingChangeListener { ratingBar, rating, fromUser ->

            openPlayStoreForRating()
        }

        binding.conclusionText.setOnClickListener {
            this.dismissNow()
        }
    }

    private fun openPlayStoreForRating() {
        try {

            val uri = Uri.parse("market://details?id=" + requireActivity().packageName)
            val rateIntent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(rateIntent)
        } catch (e: ActivityNotFoundException) {

            val uri = Uri.parse("https://play.google.com/store/apps/details?id=" + requireActivity().packageName)
            val rateIntent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(rateIntent)
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