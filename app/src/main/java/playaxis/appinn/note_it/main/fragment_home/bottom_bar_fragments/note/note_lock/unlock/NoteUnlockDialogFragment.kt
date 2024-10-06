package playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.note.note_lock.unlock

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import playaxis.appinn.note_it.R
import playaxis.appinn.note_it.databinding.UnlockNoteDialogBinding
import playaxis.appinn.note_it.main.fragment_home.HomeViewModel
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.note.note_lock.change_password.ChangePasswordDialog
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.NoteItem
import playaxis.appinn.note_it.main.utils.observeEvent
import playaxis.appinn.note_it.main.viewModels.MainViewModel
import playaxis.appinn.note_it.preferences.SharedPreference
import playaxis.appinn.note_it.repository.model.entities.Note
import playaxis.appinn.note_it.utils.MainUtils
import java.util.concurrent.Executor

class NoteUnlockDialogFragment(private var dialogEvent: CloseUnLockerEvent) : DialogFragment(), ChangePasswordDialog.ChangePasswordDialogEvents {

    private lateinit var binding: UnlockNoteDialogBinding
    private val viewModelHome: HomeViewModel by activityViewModels()
    private val viewModelMain: MainViewModel by activityViewModels()

    private var position = 0
    lateinit var lockedNote: NoteItem

    private lateinit var info: String
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    private lateinit var changePasswordDialog: ChangePasswordDialog

    var showRemoveLockDialog = false

    // Initialize the ActivityResultLauncher
    private var enrollLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Handle success
            biometricPrompt.authenticate(promptInfo)
            info = "Biometric registration Successful!"
        }
        else {
            // Handle failure or cancellation
            info = "Biometric registration failed!"
            Toast.makeText(requireActivity(),"Biometric registration failed!",Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.TransparentDialogFragment)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        binding = UnlockNoteDialogBinding.inflate(inflater, container, false)

        //setting editText focused
        binding.password.requestFocus()
        binding.password.text?.clear()
        binding.password.invalidate()

        executor = ContextCompat.getMainExecutor(requireContext())
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Finger Scan Unlock")
            .setDescription("You can unlock your note by placing you already scanned finger on finger sensor!")
            .setNegativeButtonText("Use password")
            .build()

        //Biometric prompt result
        biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {

            //Auth Error
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence, ) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(requireContext(), "Authentication error: $errString", Toast.LENGTH_SHORT).show()
            }

            //Auth Success
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult, ) {
                super.onAuthenticationSucceeded(result)

                //unlock the note
                binding.fingerPrintAnim.playAnimation()
            }

            //Auth Failed
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(requireContext(), "Place your right registered finger in the middle of sensor", Toast.LENGTH_SHORT).show()
            }
        })

        //check that device supports BIOMETRIC or not
        checkDeviceHasBiometric()

        changePasswordDialog = ChangePasswordDialog(this)

        //setting up view accordingly
        setUpView()
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    private fun setUpView() {

        if (showRemoveLockDialog){

            binding.heading.text = "Remove Lock"
            binding.passwordHeading.text = "Confirm password"
            binding.forgetChangePasswordContainer.visibility = View.GONE
            binding.unlockButton.text = "Remove"
        }
        else{

            binding.heading.text = "UnLock"
            binding.passwordHeading.text = "Enter password"
            binding.forgetChangePasswordContainer.visibility = View.VISIBLE
            binding.unlockButton.text = "Unlock"
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //listen when the animation is completed
        binding.fingerPrintAnim.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator, isReverse: Boolean) {
                super.onAnimationEnd(animation, isReverse)

                if (showRemoveLockDialog){

                    //removing the lock permanently
                    val lockedNote = ArrayList<Note>()
                    for (noteSelected in viewModelHome.selectedNotes) {

                        noteSelected.lock = ""
                        lockedNote.add(noteSelected)

                        //update the selection not
                        viewModelHome.updateSelectedNotes(lockedNote)
                        //then save the password to preference
                        SharedPreference.noteLock = false
                    }

                    //removed lock from the note
                    MainUtils.showToast(requireActivity(),"Lock Removed!")
                    binding.password.setText("")
                    //clearing selection
                    viewModelHome.clearSelection()
                    viewModelMain.bottomFragmentItemDetachEventEvent()
                }
                else{
                    //open the note
                    MainUtils.showToast(requireActivity(),"Note Unlocked!")
                    //unlocking the note
                    dialogEvent.unLockNote(position,lockedNote)
                    binding.password.setText("")
                    binding.password.invalidate()
                    binding.password.clearFocus()
                }

                dialogEvent.closeUnLocker()
            }
        })

        binding.fingerPrintAnim.setOnClickListener {
            //prompt
            biometricPrompt.authenticate(promptInfo)
        }
        //writing values
        viewModelHome.noteLockedContentEvent.observeEvent(viewLifecycleOwner) { pos, item ->
            position = pos
            lockedNote = item
        }

        binding.cancelButton.setOnClickListener {
            dialogEvent.closeUnLocker()
        }

        binding.unlockButton.setOnClickListener {

            //verify the password
            //observing and setting values
            if (binding.password.text.toString().isNotEmpty()) {
                if (lockedNote.note.lock == binding.password.text.toString()) {

                    if (showRemoveLockDialog){

                        //removing the lock permanently
                        val lockedNote = ArrayList<Note>()
                        for (noteSelected in viewModelHome.selectedNotes) {

                            noteSelected.lock = ""
                            lockedNote.add(noteSelected)

                            //removing text from editText
                            binding.password.setText("")
                            //update the selection not
                            viewModelHome.updateSelectedNotes(lockedNote)
                            //then save the password to preference
                            SharedPreference.noteLock = false
                        }

                        //removed lock from the note
                        MainUtils.showToast(requireActivity(),"Lock Removed!")
                        //clearing selection
                        viewModelHome.clearSelection()
                        viewModelMain.bottomFragmentItemDetachEventEvent()
                    }
                    else{
                        //open the note
                        MainUtils.showToast(requireActivity(),"Note Unlocked!")
                        binding.password.setText("")
                        //unlocking the note
                        dialogEvent.unLockNote(position,lockedNote)
                    }

                    dialogEvent.closeUnLocker()
                }
                else
                    binding.password.error = "Wrong password"
            }
            else
                binding.password.error = "Enter password"
        }

        //forget password listener
        binding.forgetPassword.setOnClickListener {

            //forget password for firebase
        }

        //change password listener
        binding.changePasswordButton.setOnClickListener {

            changePasswordDialog.isCancelable = false
            changePasswordDialog.show(requireActivity().supportFragmentManager, changePasswordDialog.tag)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun checkDeviceHasBiometric() {
        val biometricManager = BiometricManager.from(requireContext())
        when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)) {

            BiometricManager.BIOMETRIC_SUCCESS -> {
                binding.fingerPrintAnim.visibility = View.VISIBLE
                Log.i("MY_APP_TAG", "App can authenticate using biometrics.")
                info = "Can authenticate using biometrics."

                //start recognizing
                biometricPrompt.authenticate(promptInfo)
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Log.e("MY_APP_TAG", "No biometric features available on this device.")
                info = "No biometric features available on this device."
                binding.fingerPrintAnim.visibility = View.GONE
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Log.e("MY_APP_TAG", "Biometric features are currently unavailable.")
                info = "Biometric verification is currently unavailable."
                binding.fingerPrintAnim.visibility = View.GONE
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                binding.fingerPrintAnim.visibility = View.VISIBLE
                // Prompts the user to create credentials that your app accepts.
                val enrollIntent =
                    Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                        putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                    }
                enrollLauncher.launch(enrollIntent)
            }
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                binding.fingerPrintAnim.visibility = View.VISIBLE
                info = "Biometric needs to be updated! Try to updated the facility! But its working for now!"
            }
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                binding.fingerPrintAnim.visibility = View.GONE
                info = "Biometric is not supported on this device!"
                biometricPrompt.cancelAuthentication()
            }
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                binding.fingerPrintAnim.visibility = View.VISIBLE
                info = "Biometric UnKnown!"
                biometricPrompt.cancelAuthentication()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        setDialogSize()
    }

    override fun onDestroy() {
        super.onDestroy()

//        //made binding null
//        binding = null
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
    interface CloseUnLockerEvent {
        fun closeUnLocker()
        fun unLockNote(position: Int, lockedNote: NoteItem)
        fun updatePasswordOfNote(newPassword: String, noteItem: NoteItem)
    }

    override fun closeDialog() = changePasswordDialog.dismissNow()

    override fun updateObjectWithNewPassword(newPassword: String, noteItem: NoteItem) {
        dialogEvent.updatePasswordOfNote(newPassword,noteItem)
        //close the dialog when the password is updated
        closeDialog()
    }
}