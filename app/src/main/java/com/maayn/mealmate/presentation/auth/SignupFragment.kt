package com.maayn.mealmate.presentation.auth

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.facebook.*
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.*
import com.maayn.mealmate.R
import com.maayn.mealmate.databinding.FragmentSignupBinding

class SignupFragment : Fragment() {

    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var callbackManager: CallbackManager

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let { firebaseAuthWithGoogle(it) }
        } catch (e: ApiException) {
            showError("Google Sign-Up failed: ${e.message}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupGoogleSignIn()
        setupFacebookSignIn()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        setupTextWatchers()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
    }

    private fun setupFacebookSignIn() {
        callbackManager = CallbackManager.Factory.create()

        LoginManager.getInstance().registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    handleFacebookAccessToken(result.accessToken)
                }

                override fun onCancel() {
                    showError("Facebook sign-up canceled.")
                }

                override fun onError(error: FacebookException) {
                    showError("Facebook sign-up failed: ${error.message}")
                }
            })
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.signUpButton.setOnClickListener {
            val name = binding.nameInput.text.toString()
            val email = binding.emailInput.text.toString()
            val password = binding.passwordInput.text.toString()
            val confirmPassword = binding.confirmPasswordInput.text.toString()
            if (validateInputs(name, email, password, confirmPassword)) {
                registerUser(name, email, password)
            }
        }

        binding.loginLink.setOnClickListener {
            findNavController().navigate(R.id.action_signupFragment_to_loginFragment)
        }

        binding.googleButton.setOnClickListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }

        binding.facebookButton.setOnClickListener {
            LoginManager.getInstance().logInWithReadPermissions(
                this, listOf("email", "public_profile")
            )
        }
    }

    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                checkInputs()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        binding.nameInput.addTextChangedListener(textWatcher)
        binding.emailInput.addTextChangedListener(textWatcher)
        binding.passwordInput.addTextChangedListener(textWatcher)
        binding.confirmPasswordInput.addTextChangedListener(textWatcher)
    }

    private fun checkInputs() {
        val isNameFilled = binding.nameInput.text.toString().isNotEmpty()
        val isEmailFilled = binding.emailInput.text.toString().isNotEmpty()
        val isPasswordFilled = binding.passwordInput.text.toString().isNotEmpty()
        val isConfirmPasswordFilled = binding.confirmPasswordInput.text.toString().isNotEmpty()

        binding.signUpButton.isEnabled = isNameFilled && isEmailFilled && isPasswordFilled && isConfirmPasswordFilled
        val colorRes = if (binding.signUpButton.isEnabled) R.color.primary_color_5 else R.color.neutral_color_2
        binding.signUpButton.setBackgroundColor(ContextCompat.getColor(requireContext(), colorRes))
    }

    private fun validateInputs(name: String, email: String, password: String, confirmPassword: String): Boolean {
        return when {
            name.isEmpty() -> {
                showError("Name cannot be empty."); false
            }
            email.isEmpty() -> {
                showError("Email cannot be empty."); false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                showError("Enter a valid email address."); false
            }
            password.length < 8 -> {
                showError("Password must be at least 8 characters long."); false
            }
            password != confirmPassword -> {
                showError("Passwords do not match."); false
            }
            else -> true
        }
    }

    private fun registerUser(name: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    auth.currentUser?.updateProfile(userProfileChangeRequest { displayName = name })
                        ?.addOnCompleteListener {
                            navigateToHome()
                        }
                } else {
                    when (task.exception) {
                        is FirebaseAuthUserCollisionException -> showError("This email is already registered. Please sign in.")
                        else -> showError("Registration failed. Try again.")
                    }
                }
            }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    navigateToHome()
                } else {
                    showError("Google sign-up failed: ${task.exception?.message}")
                }
            }
    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    navigateToHome()
                } else {
                    showError("Facebook sign-up failed: ${task.exception?.message}")
                }
            }
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToHome() {
        findNavController().navigate(R.id.action_signupFragment_to_homeFragment)
        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation).selectedItemId = R.id.nav_home
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
