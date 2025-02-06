package com.maayn.mealmate.presentation.home.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.maayn.mealmate.databinding.ItemCategoryBinding
import com.maayn.mealmate.presentation.home.model.CategoryItem

class CategoriesAdapter(
    private val categories: List<CategoryItem>,
    private val onCategoryClick: (CategoryItem) -> Unit = {}
) : RecyclerView.Adapter<CategoriesAdapter.CategoryViewHolder>() {

    inner class CategoryViewHolder(
        private val binding: ItemCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CategoryItem) {
            binding.apply {
                // Use Glide to load the image from the URL
                Glide.with(root.context)
                    .load(item.strCategoryThumb)
                    .placeholder(com.maayn.mealmate.R.drawable.ic_launcher_background)
                    .into(ivCategory)

                tvCategory.text = item.name
                root.setOnClickListener { onCategoryClick(item) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        return CategoryViewHolder(
            ItemCategoryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size
}
