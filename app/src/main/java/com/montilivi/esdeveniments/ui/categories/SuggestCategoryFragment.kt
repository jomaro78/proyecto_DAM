package com.montilivi.esdeveniments.ui.categories

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.montilivi.esdeveniments.R
import com.montilivi.esdeveniments.data.repository.CategoryRepository
import com.montilivi.esdeveniments.databinding.FragmentSuggestCategoryBinding
import com.montilivi.esdeveniments.utils.FirebaseReferences

class SuggestCategoryFragment : Fragment() {

    private var _binding: FragmentSuggestCategoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: SuggestCategoryViewModel
    private lateinit var adapter: CategoryRequestAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSuggestCategoryBinding.inflate(inflater, container, false)

        val repository = CategoryRepository()
        val factory = SuggestCategoryViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[SuggestCategoryViewModel::class.java]

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecyclerView()
        setupListeners()
        setupObservers()

        viewModel.loadSuggestions()
    }

    private fun setupRecyclerView() {
        adapter = CategoryRequestAdapter()
        binding.recyclerSuggestions.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSuggestions.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnSubmitCategory.setOnClickListener {
            val suggestion = binding.etCategoryName.text.toString().trim()
            val userId = FirebaseReferences.auth.currentUser?.uid ?: return@setOnClickListener
            val userEmail = FirebaseReferences.auth.currentUser?.email ?: return@setOnClickListener

            if (suggestion.isNotEmpty()) {
                viewModel.submitSuggestion(suggestion, userId, userEmail)
            } else {
                Snackbar.make(requireView(), getString(R.string.suggest_category_empty), Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupObservers() {
        viewModel.existingSuggestions.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
        }

        viewModel.submissionSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Snackbar.make(requireView(), getString(R.string.suggest_category_success), Snackbar.LENGTH_SHORT).show()
                binding.etCategoryName.text?.clear()
                viewModel.resetStatus()
                viewModel.loadSuggestions()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { msg ->
            msg?.let {
                Snackbar.make(
                    requireView(),
                    getString(it.toIntOrNull() ?: R.string.suggest_category_generic_error),
                    Snackbar.LENGTH_SHORT
                ).show()
                viewModel.resetStatus()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
