package com.montilivi.esdeveniments.ui.events.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.navigation.fragment.findNavController
import com.montilivi.esdeveniments.databinding.FragmentHomeBinding
import com.montilivi.esdeveniments.data.model.Event
import com.montilivi.esdeveniments.ui.MainActivity
import com.montilivi.esdeveniments.ui.events.adapters.EventsAdapter

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels { HomeViewModelFactory() }
    private lateinit var adapter: EventsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as? MainActivity)?.verificarRolYActualizarMenu()

        setupRecyclerView()
        setupObservers()
        viewModel.loadEvents()
    }

    private fun setupRecyclerView() {
        adapter = EventsAdapter ({ event -> navigateToEventDetail(event) })
        binding.recyclerViewEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewEvents.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.categoryColors.observe(viewLifecycleOwner) { colors ->
            adapter.updateCategoryColors(colors)
        }

        viewModel.events.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            binding.tvEmptyMessage.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun navigateToEventDetail(event: Event) {
        val action = HomeFragmentDirections.actionHomeToEventDetail(event.eventId)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
