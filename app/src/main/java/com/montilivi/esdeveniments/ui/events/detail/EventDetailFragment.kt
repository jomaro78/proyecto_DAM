package com.montilivi.esdeveniments.ui.events.detail

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.snackbar.Snackbar
import com.montilivi.esdeveniments.R
import com.montilivi.esdeveniments.data.repository.EventRepository
import com.montilivi.esdeveniments.databinding.FragmentEventDetailBinding
import com.montilivi.esdeveniments.utils.FirebaseReferences
import com.montilivi.esdeveniments.utils.StringUtils
import kotlinx.coroutines.launch
import java.util.Date

class EventDetailFragment : Fragment() {

    private var _binding: FragmentEventDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: EventDetailViewModel
    private val args: EventDetailFragmentArgs by navArgs()
    private val userId = FirebaseReferences.auth.currentUser?.uid

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val factory = EventDetailViewModelFactory(args.eventId)
        viewModel = ViewModelProvider(this, factory)[EventDetailViewModel::class.java]

        binding.scrollView.visibility = View.INVISIBLE

        observeEvent()
        observeSubscription()
        observeExtras()

        viewModel.loadSubscriberCount()

        binding.btnSubscribe.setOnClickListener {
            if (viewModel.isSubscribed.value == true) {
                viewModel.unsubscribe()
            } else {
                viewModel.subscribe()
            }
        }

        binding.btnEditEvent.setOnClickListener {
            val action = EventDetailFragmentDirections
                .actionEventDetailFragmentToCreateEventFragment(args.eventId)
            findNavController().navigate(action)
        }

        binding.btnDeleteEvent.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.event_delete_title))
                .setMessage(getString(R.string.event_delete_message))
                .setPositiveButton(getString(R.string.yes)) { _, _ ->
                    lifecycleScope.launch {
                        val result = EventRepository().deleteEvent(args.eventId)
                        if (result.isSuccess) {
                            Snackbar.make(requireView(), getString(R.string.event_deleted), Snackbar.LENGTH_SHORT).show()
                            findNavController().navigate(R.id.homeFragment)
                        } else {
                            Snackbar.make(requireView(), getString(R.string.error_deleting_event), Snackbar.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }

        binding.btnChat.setOnClickListener {
            val action = EventDetailFragmentDirections
                .actionEventDetailFragmentToChatFragment(args.eventId)
            findNavController().navigate(action)
        }
    }

    private fun observeExtras() {
        viewModel.categoryColor.observe(viewLifecycleOwner) { colorHex ->
            try {
                val color = android.graphics.Color.parseColor(colorHex)
                binding.root.setBackgroundColor(color)
            } catch (e: IllegalArgumentException) {
                Log.e("EventDetail", "Invalid color format: $colorHex", e)
            }
        }

        viewModel.subscriberCount.observe(viewLifecycleOwner) { count ->
            binding.tvSubscriberCount.text = getString(R.string.subscriber_count, count)
        }
    }

    private fun observeEvent() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            val dateText = StringUtils.formatEventDate(binding.root.context, Date(event.startDate), Date(event.endDate))
            binding.tvEventDate.text = dateText

            val now = System.currentTimeMillis()
            val hasEnded = event.endDate < now
            val fromPastEvents = args.fromPastEvents
            val isCreator = event.creatorId == FirebaseReferences.auth.currentUser?.uid

            binding.btnEditEvent.visibility = if (!hasEnded && event.creatorId == userId) View.VISIBLE else View.GONE
            binding.btnDeleteEvent.visibility = if (!hasEnded && event.creatorId == userId) View.VISIBLE else View.GONE
            binding.eventDetailOwnerButtons.visibility =
                if (!hasEnded && event.creatorId == userId) View.VISIBLE else View.GONE
            binding.btnSubscribe.visibility = if (!hasEnded && !fromPastEvents && !isCreator) View.VISIBLE else View.GONE

            binding.tvEventTitle.text = event.title
            binding.tvEventDescription.text = event.description
            binding.tvEventLocation.text = event.locationName

            if (!event.imageUrl.isNullOrBlank()) {
                Glide.with(this)
                    .load(Uri.parse(event.imageUrl))
                    .diskCacheStrategy(DiskCacheStrategy.DATA)
                    .into(binding.ivEventImage)
            } else {
                binding.ivEventImage.setImageDrawable(null)
            }

            binding.scrollView.visibility = View.VISIBLE
            binding.btnHowToGetThere.setOnClickListener {
                val lat = event.location!!.latitude
                val lng = event.location.longitude
                val uri = Uri.parse("google.navigation:q=$lat,$lng")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.setPackage("com.google.android.apps.maps")
                if (intent.resolveActivity(requireActivity().packageManager) != null) {
                    startActivity(intent)
                } else {
                    Snackbar.make(requireView(), getString(R.string.no_maps_found), Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun observeSubscription() {
        viewModel.isSubscribed.observe(viewLifecycleOwner) { subscribed ->
            if (subscribed == true) {
                binding.btnSubscribe.text = getString(R.string.unsubscribe)
                binding.btnChat.visibility = View.VISIBLE
            } else {
                binding.btnSubscribe.text = getString(R.string.subscribe)
                binding.btnChat.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}