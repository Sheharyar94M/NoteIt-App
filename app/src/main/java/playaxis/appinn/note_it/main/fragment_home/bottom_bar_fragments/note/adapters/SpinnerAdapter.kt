package playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.note.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import playaxis.appinn.note_it.R
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.note.helper_model.FontItem

class SpinnerAdapter(context: Context, resource: Int, fontNames: ArrayList<FontItem>) : ArrayAdapter<FontItem>(context, resource, fontNames) {

    private val mContext: Context
    private val mFontNames: ArrayList<FontItem>

    init {
        mContext = context
        mFontNames = fontNames
    }

    @SuppressLint("SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val view = super.getView(position, convertView, parent)
        if (view is TextView) {

            //setting default item
            if (mFontNames[position].typeface == null){

                view.text = "Select font"
                view.setTextColor(ContextCompat.getColor(context, R.color.dialog_text_color))
                view.setTypeface(null, Typeface.NORMAL)
            }
            else{

                val typeface = mFontNames[position].typeface
                view.text = "Font$position"
                view.setTextColor(ContextCompat.getColor(context, R.color.dialog_text_color))
                (view).setTypeface(typeface)
            }
        }
        return view
    }

    @SuppressLint("SetTextI18n")
    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View = super.getDropDownView(position, convertView, parent)
        if (view is TextView) {

            //setting default item
            if (mFontNames[position].typeface == null){

                view.text = "Select font"
                view.setTextColor(ContextCompat.getColor(context, R.color.dialog_text_color))
                view.setTypeface(null, Typeface.NORMAL)
            }
            else{

                val typeface = mFontNames[position].typeface
                view.text = "Font$position"
                view.setTextColor(ContextCompat.getColor(context, R.color.dialog_text_color))
                (view).setTypeface(typeface)
            }
        }
        return view
    }
}