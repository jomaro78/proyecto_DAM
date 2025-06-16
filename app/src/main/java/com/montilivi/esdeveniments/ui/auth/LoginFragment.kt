package com.montilivi.esdeveniments.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.snackbar.Snackbar
import com.montilivi.esdeveniments.R
import com.montilivi.esdeveniments.databinding.FragmentLoginBinding
import com.montilivi.esdeveniments.ui.MainActivity

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: LoginViewModel
    private lateinit var googleSignInClient: GoogleSignInClient

    private val RC_GOOGLE_SIGN_IN = 9001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this, LoginViewModelFactory())[LoginViewModel::class.java]

        // funcio pels listeners dels botons
        setupUI()

        // funcio pels observers
        observeNavigation()

        googleSignInClient = getGoogleSignInClient()
    }

    private fun setupUI() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            if (email.isEmpty() || password.isEmpty()) {
                Snackbar.make(requireView(), getString(R.string.login_error_credentials), Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.loginWithEmail(email, password)
        }

        binding.tvRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        binding.btnGoogleLogin.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN)
            }
        }

        binding.tvForgotPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            viewModel.sendPasswordReset(email)
        }
    }

    private fun observeNavigation() {
        viewModel.navigationEvent.observe(viewLifecycleOwner) { event ->
            when (event) {
                is LoginViewModel.NavigationEvent.NavigateToHome -> {
                    (activity as? MainActivity)?.actualizarHeaderDrawer()
                    findNavController().navigate(R.id.action_login_to_home)
                }
                is LoginViewModel.NavigationEvent.NavigateToPreferences ->
                    findNavController().navigate(R.id.action_login_to_preferences)
                is LoginViewModel.NavigationEvent.ShowError ->
                    Snackbar.make(requireView(), getString(event.messageResId), Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun getGoogleSignInClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(requireActivity(), gso)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            viewModel.checkUserDataAfterGoogleLogin(data)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
