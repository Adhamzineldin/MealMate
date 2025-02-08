package com.maayn.mealmate.presentation.auth

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.userProfileChangeRequest
import com.maayn.mealmate.R
import com.maayn.mealmate.databinding.FragmentSignupBinding

class SignupFragment : Fragment() {

    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()

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

        if (isNameFilled && isEmailFilled && isPasswordFilled && isConfirmPasswordFilled) {
            binding.signUpButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary_color_5))  // Change to your desired color
            binding.signUpButton.isEnabled = true
            binding.signUpButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            binding.signUpButton.iconTint = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white))
        } else {
            binding.signUpButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.neutral_color_2))
            binding.signUpButton.isEnabled = false
            binding.signUpButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.neutral_color_4))
            binding.signUpButton.iconTint = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.neutral_color_4))



        }
    }

    private fun validateInputs(name: String, email: String, password: String, confirmPassword: String): Boolean {
        if (name.isEmpty()) {
            showError("Name cannot be empty.")
            return false
        }

        if (email.isEmpty()) {
            showError("Email cannot be empty.")
            return false
        }

        if (password.isEmpty()) {
            showError("Password cannot be empty.")
            return false
        }

        if (confirmPassword.isEmpty()) {
            showError("Confirm password cannot be empty.")
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

        if (password != confirmPassword) {
            showError("Passwords do not match.")
            return false
        }

        return true
    }

    private fun registerUser(name: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // After creating the user, update their profile with the name
                    val user = auth.currentUser
                    val profileUpdates = userProfileChangeRequest {
                        displayName = name // Set the user's display name
                    }

                    user?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener { profileUpdateTask ->
                            if (profileUpdateTask.isSuccessful) {
                                navigateToHome() // Navigate after updating the profile
                            } else {
                                showError("Profile update failed. Please try again.")
                            }
                        }

                } else {
                    val exception = task.exception
                    if (exception != null) {
                        when {
                            exception is FirebaseAuthUserCollisionException -> {
                                showError("This email is already registered. Please sign in.")
                            }
                            else -> {
                                showError("Registration failed. Please try again.")
                            }
                        }
                    }
                }
            }
    }


    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToHome() {
        findNavController().navigate(R.id.action_signupFragment_to_homeFragment)

        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_home
    }

    override fun onDestroyView() {
        super.onDestroyView()

    }
}
