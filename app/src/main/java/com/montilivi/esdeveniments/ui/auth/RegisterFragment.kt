package com.montilivi.esdeveniments.ui.auth

import android.os.Bundle
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.montilivi.esdeveniments.R
import com.montilivi.esdeveniments.databinding.FragmentRegisterBinding

class RegisterFragment : Fragment() {

    private lateinit var binding: FragmentRegisterBinding
    private lateinit var viewModel: RegisterViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this, RegisterViewModelFactory())[RegisterViewModel::class.java]

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        setupObservers()
        setupUI()
    }

    private fun setupUI() {
        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            // validacions
            if (email.isEmpty() || password.isEmpty()) {
                Snackbar.make(requireView(), getString(R.string.email_password_required), Snackbar.LENGTH_SHORT).show()
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Snackbar.make(requireView(), getString(R.string.invalid_email), Snackbar.LENGTH_SHORT).show()
            } else {
                viewModel.registerWithEmail(email, password)
            }
        }

        binding.tvGoToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }
    }

    private fun setupObservers() {
        viewModel.navigationEvent.observe(viewLifecycleOwner) { event ->
            when (event) {
                is RegisterViewModel.NavigationEvent.NavigateToPreferences -> {
                    // navegacio i neteja de pila
                    findNavController().navigate(
                        R.id.action_register_to_preferences,
                        null,
                        androidx.navigation.NavOptions.Builder()
                            .setPopUpTo(R.id.registerFragment, true)
                            .build()
                    )
                }
                is RegisterViewModel.NavigationEvent.ShowError -> {
                    Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
