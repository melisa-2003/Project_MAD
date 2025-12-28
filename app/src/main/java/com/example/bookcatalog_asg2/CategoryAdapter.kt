package com.example.bookcatalog_asg2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bookcatalog_asg2.databinding.ItemCategoryChipBinding

class CategoryAdapter(
    private val categories: MutableList<Category>,
    private val onCategoryClick: (String) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(
        val binding: ItemCategoryChipBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryChipBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]

        holder.binding.tvCategoryName.text = category.name
        holder.binding.root.isSelected = category.isSelected

        holder.binding.tvCategoryName.setTextColor(
            if (category.isSelected)
                holder.itemView.context.getColor(android.R.color.white)
            else
                holder.itemView.context.getColor(android.R.color.black)
        )

        holder.binding.root.setOnClickListener {
            // reset all
            categories.forEach { it.isSelected = false }

            // select clicked
            category.isSelected = true

            notifyDataSetChanged()

            // trigger filtering
            onCategoryClick(category.name)
        }
    }

    override fun getItemCount(): Int = categories.size
}

