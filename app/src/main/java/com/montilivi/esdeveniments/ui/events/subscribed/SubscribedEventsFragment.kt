package com.montilivi.esdeveniments.ui.events.subscribed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.montilivi.esdeveniments.data.model.Event
import com.montilivi.esdeveniments.databinding.FragmentSubscribedEventsBinding
import com.montilivi.esdeveniments.ui.MainActivity
import com.montilivi.esdeveniments.ui.events.adapters.EventsAdapter

class SubscribedEventsFragment : Fragment() {

    private lateinit var binding: FragmentSubscribedEventsBinding
    private lateinit var viewModel: SubscribedEventsViewModel
    private lateinit var adapter: EventsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSubscribedEventsBinding.inflate(inflater, container, false)
        val factory = SubscribedEventsViewModelFactory()
        viewModel = ViewModelProvider(this, factory)[SubscribedEventsViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as? MainActivity)?.verificarRolYActualizarMenu()

        setupRecyclerView()
        setupObservers()
        viewModel.loadSubscribedEvents()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadSubscribedEvents()
    }

    private fun setupRecyclerView() {
        adapter = EventsAdapter ({ event -> navigateToEventDetail(event) })
        binding.recyclerSubscribedEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSubscribedEvents.adapter = adapter
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
        val action = SubscribedEventsFragmentDirections
            .actionSubscribedEventsToEventDetail(event.eventId, fromPastEvents = false)
        findNavController().navigate(action)
    }
}