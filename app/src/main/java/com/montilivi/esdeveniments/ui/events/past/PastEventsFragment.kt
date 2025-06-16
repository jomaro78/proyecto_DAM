package com.montilivi.esdeveniments.ui.events.past

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.montilivi.esdeveniments.data.model.Event
import com.montilivi.esdeveniments.databinding.FragmentPastEventsBinding
import com.montilivi.esdeveniments.ui.MainActivity
import com.montilivi.esdeveniments.ui.events.adapters.EventsAdapter

class PastEventsFragment : Fragment() {

    private var _binding: FragmentPastEventsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PastEventsViewModel by viewModels { PastEventsViewModelFactory() }
    private lateinit var adapter: EventsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPastEventsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as? MainActivity)?.verificarRolYActualizarMenu()

        setupRecyclerView()
        setupObservers()
    }

    private fun setupRecyclerView() {
        adapter = EventsAdapter ({ event -> navigateToEventDetail(event) })
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.categoryColors.observe(viewLifecycleOwner) { colors ->
            adapter.updateCategoryColors(colors)
        }

        viewModel.pastEvents.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            binding.tvEmptyMessage.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun navigateToEventDetail(event: Event) {
        val action = PastEventsFragmentDirections.actionPastEventsToEventDetail(event.eventId)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}