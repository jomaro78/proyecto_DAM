package com.montilivi.esdeveniments.ui.events.chat

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.montilivi.esdeveniments.R
import com.montilivi.esdeveniments.data.repository.ChatRepository
import com.montilivi.esdeveniments.data.repository.SubscriptionRepository
import com.montilivi.esdeveniments.databinding.FragmentChatBinding
import com.montilivi.esdeveniments.ui.events.adapters.ChatAdapter
import com.montilivi.esdeveniments.utils.FirebaseReferences
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val args: ChatFragmentArgs by navArgs()
    private val viewModel: ChatViewModel by viewModels { ChatViewModelFactory(args.eventId) }
    private lateinit var adapter: ChatAdapter

    private var lastReadId: String? = null
    private val chatRepository = ChatRepository()

    private var updateActivityHandler: Handler? = null
    private val updateActivityRunnable = object : Runnable {
        override fun run() {
            lifecycleScope.launch {
                chatRepository.updateUserActivity(args.eventId)
            }
            updateActivityHandler?.postDelayed(this, 60_000)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbarAndStats()
        setupRecyclerView()
        setupObservers()
        setupListeners()
        viewModel.fetchLastReadMessageId { }
    }

    private fun setupRecyclerView() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        adapter = ChatAdapter(currentUserId)
        binding.recyclerViewChat.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewChat.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.lastReadMessageId.observe(viewLifecycleOwner) { readId ->
            lastReadId = readId
        }

        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            adapter.submitList(messages)

            val lastMsg = messages.lastOrNull()
            if (lastMsg != null && lastMsg.messageId.isNotBlank()) {
                viewModel.markMessageAsRead(lastMsg.messageId)
            } else {
                viewModel.markMessageAsRead("system_init")
            }

            val layoutManager = binding.recyclerViewChat.layoutManager as LinearLayoutManager
            binding.recyclerViewChat.postDelayed({
                if (viewModel.hasJustSentMessage) {
                    layoutManager.scrollToPositionWithOffset(messages.size - 1, 50)
                    viewModel.hasJustSentMessage = false
                } else {
                    val index = messages.indexOfFirst { it.messageId == lastReadId }
                    val scrollIndex = if (index != -1 && index + 1 < messages.size) index + 1 else messages.size - 1
                    layoutManager.scrollToPositionWithOffset(scrollIndex, 50)
                }
            }, 100)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.canSendMessages.observe(viewLifecycleOwner) { canSend ->
            binding.editMessage.isEnabled = canSend
            binding.btnSend.isEnabled = canSend
        }
    }

    private fun setupListeners() {
        binding.btnSend.setOnClickListener {
            val messageText = binding.editMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                viewModel.sendMessage(messageText)
                binding.editMessage.text.clear()
                val imm = getSystemService(requireContext(), InputMethodManager::class.java)
                imm?.hideSoftInputFromWindow(binding.editMessage.windowToken, 0)
            } else {
                Snackbar.make(requireView(), getString(R.string.empty_message_warning), Snackbar.LENGTH_SHORT).show()
            }
            viewModel.hasJustSentMessage = true
        }
    }

    private fun setupToolbarAndStats() {
        val eventId = args.eventId
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val eventSnapshot = FirebaseReferences.db.collection("events").document(eventId).get().await()
                val eventName = eventSnapshot.getString("title") ?: "Chat"
                (requireActivity() as? AppCompatActivity)?.supportActionBar?.title = eventName

                val subscriptionRepo = SubscriptionRepository()
                val total = subscriptionRepo.getSubscriberCount(eventId)
                val online = subscriptionRepo.getOnlineUserCount(eventId)
                val statsText = getString(R.string.chat_toolbar_stats, total, online)
                binding.tvChatStats.text = statsText
                binding.tvChatStats.setBackgroundColor(Color.GREEN)
            } catch (e: Exception) {
                binding.tvChatStats.text = "Error al cargar datos"
                binding.tvChatStats.setBackgroundColor(Color.DKGRAY)
            } finally {
                binding.tvChatStats.visibility = View.VISIBLE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            chatRepository.updateUserActivity(args.eventId)
        }
        updateActivityHandler = Handler(Looper.getMainLooper())
        updateActivityHandler?.post(updateActivityRunnable)
    }

    override fun onPause() {
        super.onPause()
        updateActivityHandler?.removeCallbacks(updateActivityRunnable)
        updateActivityHandler = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
