package com.montilivi.esdeveniments.ui.events.adapters

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.montilivi.esdeveniments.R
import com.montilivi.esdeveniments.data.model.ChatMessage
import com.montilivi.esdeveniments.databinding.ItemChatDateBinding
import com.montilivi.esdeveniments.databinding.ItemChatMeBinding
import com.montilivi.esdeveniments.databinding.ItemChatOtherBinding
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(private val currentUserId: String) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_ME = 1
    private val VIEW_TYPE_OTHER = 2
    private val VIEW_TYPE_DATE = 3

    private var items: List<Any> = emptyList()

    fun submitList(messages: List<ChatMessage>) {
        // Aseguramos que estén ordenados por timestamp de forma fiable
        val sortedMessages = messages.sortedBy { it.timestamp }

        val grouped = mutableListOf<Any>()
        var lastDate = ""

        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        for (msg in sortedMessages) {
            val dayKey = sdf.format(Date(msg.timestamp))
            if (dayKey != lastDate) {
                lastDate = dayKey
                grouped.add(dayKey) // Separador de fecha
            }
            grouped.add(msg)
        }

        items = grouped
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = items[position]) {
            is String -> VIEW_TYPE_DATE
            is ChatMessage -> if (item.senderId == currentUserId) VIEW_TYPE_ME else VIEW_TYPE_OTHER
            else -> throw IllegalArgumentException("Tipo de item desconocido")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_ME -> ChatViewHolderMe(ItemChatMeBinding.inflate(inflater, parent, false))
            VIEW_TYPE_OTHER -> ChatViewHolderOther(ItemChatOtherBinding.inflate(inflater, parent, false))
            VIEW_TYPE_DATE -> DateSeparatorViewHolder(ItemChatDateBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException("Tipo de vista desconocido")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ChatMessage -> when (holder) {
                is ChatViewHolderMe -> holder.bind(item)
                is ChatViewHolderOther -> holder.bind(item)
            }
            is String -> (holder as DateSeparatorViewHolder).bind(item)
        }
    }

    override fun getItemCount(): Int = items.size

    class ChatViewHolderMe(private val binding: ItemChatMeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            binding.tvMessage.text = message.message
            binding.tvTimeStamp.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))

            val lp = binding.root.layoutParams
            if (lp is LinearLayout.LayoutParams) {
                lp.gravity = Gravity.END
                binding.root.layoutParams = lp
            }
        }
    }

    class ChatViewHolderOther(private val binding: ItemChatOtherBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            binding.tvSender.text = message.senderName
            binding.tvMessage.text = message.message
            binding.tvTimeStamp.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))

            val layoutParams = itemView.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.marginStart = 0
            layoutParams.marginEnd = 50
            itemView.layoutParams = layoutParams

            val lp = binding.root.layoutParams
            if (lp is LinearLayout.LayoutParams) {
                lp.gravity = Gravity.START
                binding.root.layoutParams = lp
            }
        }
    }

    class DateSeparatorViewHolder(private val binding: ItemChatDateBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(dateKey: String) {
            val sdfIn = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val sdfOut = SimpleDateFormat("EEEE, d MMM", Locale("es"))
            val date = sdfIn.parse(dateKey)
            binding.tvDate.text = sdfOut.format(date ?: Date())

            // Centrar explícitamente el texto si el layout lo pierde
            val lp = binding.tvDate.layoutParams
            if (lp is LinearLayout.LayoutParams) {
                lp.gravity = Gravity.CENTER_HORIZONTAL
                binding.tvDate.layoutParams = lp
            }

            // También podemos centrar todo el itemView
            val params = itemView.layoutParams as ViewGroup.MarginLayoutParams
            params.marginStart = 0
            params.marginEnd = 0
            itemView.layoutParams = params
        }
    }
}
