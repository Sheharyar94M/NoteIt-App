package playaxis.appinn.note_it.main.fragment_home.account.sign_up

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import playaxis.appinn.note_it.databinding.SignupDialogBottomLayoutBinding
import playaxis.appinn.note_it.preferences.SharedPreference
import playaxis.appinn.note_it.utils.MainUtils

class SignUpDialogFragment(private val dialogEvents: SignUpDialogEvents) : BottomSheetDialogFragment() {

    private lateinit var binding: SignupDialogBottomLayoutBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        binding = SignupDialogBottomLayoutBinding.inflate(inflater, container, false)

        firebaseAuth = FirebaseAuth.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //this will take back to sign in
        binding.signinLink.setOnClickListener {
            dialogEvents.switchToSignIn()
        }

        //sign up now
        binding.signupButton.setOnClickListener {

            //create variables for getting values from fields
            var firstName = ""
            var lastName = ""
            var userName = ""
            var password = ""
            var confirmPassword = ""

            //check whether the fields are empty or not
            //first name
            if (binding.firstName.text.toString().isNotEmpty()){
                firstName = binding.firstName.text.toString()
            }
            else{
                binding.firstName.error = "Give some name!"
            }

            //last name
            if (binding.lastName.text.toString().isNotEmpty()){
                lastName = binding.lastName.text.toString()
            }
            else{
                binding.lastName.error = "Give some name!"
            }

            //user name
            if (binding.userName.text.toString().isNotEmpty()){
                userName = binding.userName.text.toString()
            }
            else{
                binding.userName.error = "Give some email!"
            }

            //password
            if (binding.password.text.toString().isNotEmpty()){
                password = binding.password.text.toString()
            }
            else{
                binding.password.error = "Enter some password!"
            }

            //confirm password
            if (binding.confirmPassword.text.toString().isNotEmpty()){
                confirmPassword = binding.confirmPassword.text.toString()
            }

            if (confirmPassword == password){

                if (userName.contains("@") && userName.contains(".")){

                    //signup request
                    lifecycleScope.launch(Dispatchers.IO) {

                        firebaseAuth.createUserWithEmailAndPassword(userName,password)
                            .addOnSuccessListener {

                                //signed up successfully, So saving username and email to preferences
                                SharedPreference.email = userName
                                SharedPreference.username = "$firstName $lastName"
                                dialogEvents.switchToSignIn()

                                MainUtils.showToast(requireActivity(),"Account created!")
                            }
                            .addOnFailureListener {
                                MainUtils.showToast(requireActivity(),it.message.toString())
                            }
                    }
                }
                else{
                    MainUtils.showToast(requireActivity(),"Invalid Username!")
                }
            }
            else{
                MainUtils.showToast(requireActivity(),"Passwords do not match!")
            }
        }
    }

    interface SignUpDialogEvents{
        fun switchToSignIn()
    }
}