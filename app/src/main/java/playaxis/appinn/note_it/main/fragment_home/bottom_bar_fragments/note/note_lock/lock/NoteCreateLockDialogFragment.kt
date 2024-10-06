package playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.note.note_lock.lock

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import playaxis.appinn.note_it.R
import playaxis.appinn.note_it.databinding.CreatePasswordDialogBinding
import playaxis.appinn.note_it.utils.MainUtils

class NoteCreateLockDialogFragment(
    private var closeDialog: CloseLockerEvent,
    private var addLockToNote:AddLockToNoteEvent): DialogFragment() {

    private lateinit var binding: CreatePasswordDialogBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.TransparentDialogFragment)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        // Inflate the layout for this fragment
        binding = CreatePasswordDialogBinding.inflate(inflater, container, false)

        //setting editText focused
        binding.newPassword.requestFocus()
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cancelButton.setOnClickListener {
            closeDialog.closeLocker()
        }

        //save the note password
        binding.saveButton.setOnClickListener {
            var password = ""
            var confirm_password = ""

            //check the editTexts are empty or not
            //password
            if (binding.newPassword.text.toString().isNotEmpty()){
                password = binding.newPassword.text.toString()
            }
            else{
                binding.newPassword.error = "Enter password"
            }

            //confirm password
            if (binding.confirmPassword.text.toString().isNotEmpty()){
                confirm_password = binding.confirmPassword.text.toString()
            }
            else{
                binding.confirmPassword.error = "Confirm password"
            }

            //applying lock to the note
            if (password.isNotEmpty() && confirm_password.isNotEmpty()){

                if (password == confirm_password){
                    binding.newPassword.setText("")
                    binding.confirmPassword.setText("")
                    //now update the object of the note on which the lock is applied
                    addLockToNote.addLockToNote(password)
                }
                else{
                    //set error of not equal passwords
                    MainUtils.showToast(requireActivity(),"Password do not match with Confirm password!")
                }
            }
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

    interface CloseLockerEvent{
        fun closeLocker()
    }

    interface AddLockToNoteEvent{
        fun addLockToNote(password: String)
    }
}