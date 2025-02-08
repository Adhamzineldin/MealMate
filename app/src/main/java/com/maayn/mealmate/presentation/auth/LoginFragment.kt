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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.maayn.mealmate.R
import com.maayn.mealmate.databinding.FragmentLoginBinding
import com.maayn.mealmate.presentation.home.HomeFragment

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var googleSignInClient: GoogleSignInClient
    private val auth = FirebaseAuth.getInstance()

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            // If successful, get the ID token and call firebaseAuthWithGoogle
            account?.idToken?.let { firebaseAuthWithGoogle(it) }
        } catch (e: ApiException) {
            // Handle error
            Toast.makeText(requireContext(), "Google Sign-In failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        if (auth.currentUser != null) {
            navigateToHome()
            return // Prevent further execution
        }
        setupGoogleSignIn()
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

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text.toString()
            val password = binding.passwordInput.text.toString()
            if (validateInputs(email, password)) {
                loginUser(email, password)
            }
        }

        binding.guestButton.setOnClickListener{
            navigateToHome()
        }

        binding.googleButton.setOnClickListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }

        binding.signUpLink.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signupFragment)
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

        binding.emailInput.addTextChangedListener(textWatcher)
        binding.passwordInput.addTextChangedListener(textWatcher)
    }

    private fun checkInputs() {
        val isEmailFilled = binding.emailInput.text.toString().isNotEmpty()
        val isPasswordFilled = binding.passwordInput.text.toString().isNotEmpty()

        if (isEmailFilled && isPasswordFilled) {
            binding.loginButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary_color_5))  // Change to your desired color
            binding.loginButton.isEnabled = true
            binding.loginButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            binding.loginButton.iconTint = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white))
        } else {
            binding.loginButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.neutral_color_2))
            binding.loginButton.isEnabled = false
            binding.loginButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.neutral_color_4))
            binding.loginButton.iconTint = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.neutral_color_4))
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        if (email.isEmpty() || password.isEmpty()) {
            showError("Email and password cannot be empty.")
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Please enter a valid email address.")
            return false
        }

        if (password.length < 8) {
            showError("Password must be at least 8 characters long.")
            return false
        }

        return true
    }

    private fun loginUser(email: String, password: String) {

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    navigateToHome()
                } else {
                    val exception = task.exception
                    if (exception is FirebaseAuthInvalidUserException) {
                        showError("User not found. Please sign up.")
                    } else if (exception is FirebaseAuthInvalidCredentialsException) {
                        showError("Invalid credentials. Please check your email and password.")
                    } else {
                        showError("An error occurred. Please try again.")
                    }
                }
            }
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    navigateToHome()
                } else {
                    Toast.makeText(requireContext(), "Sign-in failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }




    private fun navigateToHome() {

        // Navigate to the HomeFragment
        findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_home
    }

    override fun onDestroyView() {
        super.onDestroyView()

    }
}
