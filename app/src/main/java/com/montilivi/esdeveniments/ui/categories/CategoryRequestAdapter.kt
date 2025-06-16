package com.montilivi.esdeveniments.ui.categories

import android.graphics.Typeface
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.montilivi.esdeveniments.R
import com.montilivi.esdeveniments.databinding.ItemSuggestedCategoryBinding

class CategoryRequestAdapter : RecyclerView.Adapter<CategoryRequestAdapter.ViewHolder>() {

    private val suggestions = mutableListOf<String>()

    inner class ViewHolder(private val binding: ItemSuggestedCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: String) {
            binding.tvCategoryName.text = item

            val pattern = Regex("\\((\\d+)\\)$")
            val match = pattern.find(item)
            val count = match?.groupValues?.get(1)?.toIntOrNull() ?: 1

            if (count > 1) {
                binding.tvCategoryName.setTypeface(null, Typeface.BOLD)
                binding.tvCategoryName.setTextColor(
                    ContextCompat.getColor(binding.root.context, com.google.android.libraries.places.R.color.quantum_teal700)
                )
            } else {
                binding.tvCategoryName.setTypeface(null, Typeface.NORMAL)
                binding.tvCategoryName.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.categoryTextDefault)
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSuggestedCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = suggestions[position]
        Log.d("AdapterBind", "Mostrando item: $item")
        holder.bind(item)
    }

    override fun getItemCount(): Int = suggestions.size

    fun submitList(newList: List<String>) {
        suggestions.clear()
        suggestions.addAll(newList)
        notifyDataSetChanged()
    }
}
