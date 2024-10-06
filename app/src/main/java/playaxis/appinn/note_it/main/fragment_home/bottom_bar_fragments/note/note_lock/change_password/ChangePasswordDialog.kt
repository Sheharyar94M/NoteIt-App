package playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.note.note_lock.change_password

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import playaxis.appinn.note_it.R
import playaxis.appinn.note_it.databinding.ChangePasswordDialogBinding
import playaxis.appinn.note_it.main.fragment_home.HomeViewModel
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.NoteItem

class ChangePasswordDialog(private var changePasswordDialogEvents: ChangePasswordDialogEvents): DialogFragment() {

    private lateinit var binding: ChangePasswordDialogBinding
    private val viewModelHome: HomeViewModel by activityViewModels()

    private var noteItem: NoteItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.TransparentDialogFragment)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        // Inflate the layout for this fragment
        binding = ChangePasswordDialogBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //close the dialog
        binding.cancelButton.setOnClickListener {
            changePasswordDialogEvents.closeDialog()
        }

        //update the object and close the dialog
        binding.saveButton.setOnClickListener {

            var currentPassword = ""
            var newPassword = ""
            var confirmPassword = ""

            //check first if any field is empty or not
            //current password
            if (binding.currentPassword.text.toString().isNotEmpty()){
                currentPassword = binding.currentPassword.text.toString()
            }
            else{
                binding.currentPassword.error = "Need previous password"
            }

            //new password
            if (binding.newPassword.text.toString().isNotEmpty()){

                newPassword = binding.newPassword.text.toString()
            }
            else{
                binding.currentPassword.error = "Enter new password"
            }

            //confirm password
            if (binding.confirmPassword.text.toString().isNotEmpty()){

                confirmPassword = binding.confirmPassword.text.toString()
            }
            else{

                binding.confirmPassword.error = "Confirm new password"
            }

            //check that either the current password is equal to entered current password or not
            if (currentPassword == noteItem!!.note.lock){

                //now match the new password with the confirm password
                if (newPassword == confirmPassword){

                    //If this check is executed as positive this means current password entered is correct and the new password also matches the
                    //confirm password
                    //So now update the object
                    changePasswordDialogEvents.updateObjectWithNewPassword(newPassword, noteItem!!)
                }
                else
                    binding.confirmPassword.error = "passwords do not match"
            }
            else
                binding.currentPassword.error = "Incorrect current password!"
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
            val dialogHeight = (screenHeight * 0.6).toInt()

            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(dialog.window?.attributes)
            layoutParams.width = dialogWidth
            layoutParams.height = dialogHeight
            dialog.window?.attributes = layoutParams
            dialog.window?.setGravity(Gravity.CENTER)
        }
    }

    interface ChangePasswordDialogEvents{
        fun closeDialog()
        fun updateObjectWithNewPassword(newPassword: String, noteItem: NoteItem)
    }
}