package com.maayn.mealmate.presentation.home.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.maayn.mealmate.databinding.ItemRecipeBinding
import com.maayn.mealmate.presentation.home.model.*
import com.maayn.mealmate.R

class RecipesAdapter(
    private val recipes: List<RecipeItem>,
    private val onRecipeClick: (RecipeItem) -> Unit = {}
) : RecyclerView.Adapter<RecipesAdapter.RecipeViewHolder>() {

    inner class RecipeViewHolder(
        private val binding: ItemRecipeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RecipeItem) {
            binding.apply {
                // Load image using Glide
                Glide.with(ivRecipe.context)
                    .load(item.image) // The image URL or resource
                    .placeholder(R.drawable.meal_mate_icon) // Optional placeholder
                    .into(ivRecipe)

                tvRecipeName.text = item.name
                tvRecipeDuration.text = item.duration
                rbRecipeRating.rating = item.rating
                tvRatingCount.text = "${item.ratingCount} ratings"  // Ensure ratingCount is set correctly

                root.setOnClickListener { onRecipeClick(item) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        return RecipeViewHolder(
            ItemRecipeBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        holder.bind(recipes[position])
    }

    override fun getItemCount(): Int = recipes.size
}
