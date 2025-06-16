package com.montilivi.esdeveniments.ui.events.adapters

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.montilivi.esdeveniments.R
import com.montilivi.esdeveniments.data.model.Event
import com.montilivi.esdeveniments.databinding.ItemEventBinding
import com.montilivi.esdeveniments.utils.StringUtils
import java.util.Date

class EventsAdapter(
    private val onItemClick: (Event) -> Unit,
    private val categoryColors: MutableMap<String, String> = mutableMapOf()
) : RecyclerView.Adapter<EventsAdapter.EventViewHolder>() {

    private var events = emptyList<Event>()

    inner class EventViewHolder(val binding: ItemEventBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event) {
            binding.eventTitle.text = event.title

            val formattedDate = StringUtils.formatEventDate(
                binding.root.context,
                Date(event.startDate),
                Date(event.endDate)
            )
            binding.eventDate.text = formattedDate

            binding.eventLocation.text = StringUtils.formatEventLocation(event.locationName)

            val imageUrl = event.imageUrl
            if (!imageUrl.isNullOrBlank()) {
                Glide.with(binding.root.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_broken_image)
                    .diskCacheStrategy(DiskCacheStrategy.DATA)
                    .into(binding.eventImage)
            } else {
                binding.eventImage.setImageResource(R.drawable.ic_launcher_background)
            }

            binding.root.setOnClickListener {
                onItemClick(event)
            }

            val colorHex = categoryColors[event.category] ?: "#FFFFFF"
            try {
                val color = Color.parseColor(colorHex)
                binding.root.setCardBackgroundColor(color)
            } catch (e: IllegalArgumentException) {
                Log.e("EventsAdapter", "Color inválido: $colorHex")
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(events[position])
    }

    override fun getItemCount(): Int = events.size

    fun submitList(newEvents: List<Event>) {
        events = newEvents
        notifyDataSetChanged()
    }

    fun updateCategoryColors(newColors: Map<String, String>) {
        (categoryColors as MutableMap).clear()
        (categoryColors as MutableMap).putAll(newColors)
        notifyDataSetChanged()
    }
}
