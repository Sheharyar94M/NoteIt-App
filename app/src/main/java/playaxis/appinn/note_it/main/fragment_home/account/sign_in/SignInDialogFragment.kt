package playaxis.appinn.note_it.main.fragment_home.account.sign_in

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import playaxis.appinn.note_it.R
import playaxis.appinn.note_it.databinding.SigninDialogBottomLayoutBinding
import playaxis.appinn.note_it.main.fragment_home.account.sign_up.SignUpDialogFragment
import playaxis.appinn.note_it.preferences.SharedPreference
import playaxis.appinn.note_it.utils.MainUtils


class SignInDialogFragment(private val signInEvent: SignInEvents) : BottomSheetDialogFragment(), SignUpDialogFragment.SignUpDialogEvents {

    private lateinit var binding: SigninDialogBottomLayoutBinding
    private lateinit var firebaseAuth: FirebaseAuth
    lateinit var googleSignInClient: GoogleSignInClient

    private lateinit var signup: SignUpDialogFragment

    // You can do the assignment inside onAttach or onCreate, i.e, before the activity is displayed
    private val signInActivityResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->

        // When request code is equal to 100 initialize task
        val signInAccountTask: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(result.data)

        // check condition
        if (signInAccountTask.isSuccessful) {
            // When google sign in successful initialize string
            MainUtils.showToast(requireActivity(),"Logged In")

            // Initialize sign in account
            try {
                // Initialize sign in account
                val googleSignInAccount = signInAccountTask.getResult(ApiException::class.java)
                // Check condition
                if (googleSignInAccount != null) {
                    // When sign in account is not equal to null initialize auth credential
                    val authCredential: AuthCredential = GoogleAuthProvider.getCredential(
                        googleSignInAccount.idToken, null
                    )
                    // Check credential
                    firebaseAuth.signInWithCredential(authCredential)
                        .addOnSuccessListener {

                            //logged in with google
                            if (it.user != null){
                                SharedPreference.email = it.user!!.email
                                SharedPreference.username = it.user!!.displayName

                                SharedPreference.isLoggedIn = true
                                signInEvent.close()
                            }
                        }
                        .addOnFailureListener {
                            MainUtils.showToast(requireActivity(),it.message.toString())
                        }
                }
            } catch (e: ApiException) {
                e.printStackTrace()
                Log.i( "loginWithGoogle: ",e.message.toString())
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = SigninDialogBottomLayoutBinding.inflate(inflater, container, false)

        //auth object
        firebaseAuth = FirebaseAuth.getInstance()

        // Initialize sign in options the client-id is copied form google-services.json file
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.client_id))
            .requestEmail()
            .build()

        // Initialize sign in client
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), googleSignInOptions)

        //signup dialog
        signup = SignUpDialogFragment(this)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.signinButton.setOnClickListener{

            var email = ""
            var password = ""

            //Username check
            if (binding.userName.text.toString().isNotEmpty()){
                email = binding.userName.text.toString()
            }
            else{
                binding.userName.error = "Enter username"
            }

            //Password check
            if (binding.password.text.toString().isNotEmpty()){
                password = binding.password.text.toString()
            }
            else{
                binding.password.error = "Enter password"
            }

            firebaseAuth.signInWithEmailAndPassword(email,password)
                .addOnSuccessListener {

                    //Login successFul
                    MainUtils.showToast(requireActivity(),"Logged In")
                    SharedPreference.isLoggedIn = true

                    //save the username and email
                    SharedPreference.email = email
                    signInEvent.close()
                }
                .addOnFailureListener {
                    MainUtils.showToast(requireActivity(), "Unable to login! Check password or username! If everything looks good! Then try to Signup!")
                }
        }

        //sign-in with google
        binding.signinWithGoogle.setOnClickListener {
            signInWithGoogle()
        }

        binding.forgetPassword.setOnClickListener {

            var email = ""

            //Username check
            if (binding.userName.text.toString().isNotEmpty()){
                email = binding.userName.text.toString()

                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnSuccessListener { task ->
                        MainUtils.showToast(requireActivity(),"The reset link has sent to your email!")
                        signInEvent.close()
                    }
            }
            else{
                binding.userName.error = "Enter username"
            }
        }

        binding.signupLink.setOnClickListener {

            //bottom sheet dialog add items
            signup.isCancelable = true
            signup.show(requireActivity().supportFragmentManager, signup.tag)
        }
    }

    private fun signInWithGoogle() {

        val intent: Intent = googleSignInClient.signInIntent
        // Start activity for result
        signInActivityResult.launch(intent)
    }

    override fun switchToSignIn() {
        signup.dismissNow()
    }

    interface SignInEvents{
        fun close()
    }
}