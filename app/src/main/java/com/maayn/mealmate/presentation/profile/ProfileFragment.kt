package com.maayn.mealmate.presentation.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.maayn.mealmate.R

class ProfileFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    // View binding properties
    private lateinit var btnLogout: MaterialButton
    private lateinit var tvUsername: TextView
    private lateinit var tvEmail: TextView
    private lateinit var btnSettings: ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupFirebaseAuth()
        setupGoogleSignIn()
        loadUserData()
        setupClickListeners()
    }

    private fun initializeViews(view: View) {
        btnLogout = view.findViewById(R.id.btnLogout)
        tvUsername = view.findViewById(R.id.tvUsername)
        tvEmail = view.findViewById(R.id.tvEmail)
        btnSettings = view.findViewById(R.id.btnSettings)
    }

    private fun setupFirebaseAuth() {
        auth = FirebaseAuth.getInstance()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
    }

    private fun loadUserData() {
        auth.currentUser?.let { user ->
            tvUsername.text = user.displayName ?: "Guest"
            tvEmail.text = user.email ?: "No email"
        }
    }

    private fun setupClickListeners() {
        btnLogout.setOnClickListener {
            signOut()
        }

        btnSettings.setOnClickListener {
            // Navigate to settings fragment
            // findNavController().navigate(R.id.action_profileFragment_to_settingsFragment)
        }
    }

    private fun signOut() {
        auth.signOut()
        googleSignInClient.revokeAccess().addOnCompleteListener(requireActivity()) {
            findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
        }
    }
}