package com.maayn.mealmate.presentation.home.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.maayn.mealmate.databinding.ItemRecipeBinding
import com.maayn.mealmate.presentation.home.model.RecipeItem
import com.maayn.mealmate.R
import com.maayn.mealmate.data.local.database.AppDatabase
import com.maayn.mealmate.presentation.home.model.toMealWithDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RecipesAdapter(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope,
    private var recipes: List<RecipeItem>? = emptyList(),
) : ListAdapter<RecipeItem, RecipesAdapter.RecipeViewHolder>(RecipeDiffCallback()) {

    init {
        submitList(ArrayList(recipes)) // Ensure list cloning to trigger UI updates
    }

    inner class RecipeViewHolder(
        private val binding: ItemRecipeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RecipeItem) {
            binding.apply {
                Glide.with(ivRecipe.context)
                    .load(item.imageUrl)
                    .placeholder(R.drawable.meal_mate_icon)
                    .into(ivRecipe)

                tvRecipeName.text = item.name
                tvRecipeDuration.text = item.time
                rbRecipeRating.rating = item.rating
                tvRatingCount.text = "${item.ratingCount} ratings"

                // Set heart icon based on `isFavorited`
                updateFavoriteIcon(item.isFavorited)

                // Handle heart click
                ivFavorite.setOnClickListener {
                    val updatedItem = item.copy(isFavorited = !item.isFavorited) // Toggle state
                    updateFavoriteInDatabase(updatedItem) // 🔥 Save to database
                    updateItem(updatedItem) // 🔥 Force UI update
                }

                btnCreateMealPlan.setOnClickListener {
                    onCreateMealPlanButtonClick(item)
                }

                root.setOnClickListener { onRecipeClick(item) }
            }
        }
        private fun onCreateMealPlanButtonClick(item: RecipeItem) {
            Log.e("click", "onCreateMealPlanButtonClick:")
        }

        private fun onRecipeClick(item: RecipeItem) {
            Log.e("click", "onRecipeClick:")

        }

        private fun updateFavoriteInDatabase(updatedItem: RecipeItem) {
            val updatedItemAndClass = updatedItem.toMealWithDetails()
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val db = AppDatabase.getInstance(context) // Use passed context
                    val favoriteDao = db.favoriteMealDao()

                    val existingItem = favoriteDao.getFavoriteMealDetailsById(updatedItemAndClass.meal.id)

                    if (existingItem != null) {
                        favoriteDao.updateMealWithDetails(updatedItemAndClass)
                        Log.e("DB", "Updated existing favorite: ${updatedItem.name}")
                    } else {
                        favoriteDao.insertMealWithDetails(updatedItemAndClass)
                        Log.e("DB", "Added new favorite: ${updatedItem.name}")
                    }
                } catch (e: Exception) {
                    Log.e("DB_ERROR", "Failed to update favorite: ${e.message}")
                }
            }
        }

        private fun updateFavoriteIcon(isFavorited: Boolean) {
            binding.ivFavorite.setImageResource(
                if (isFavorited) R.drawable.ic_heart_filled
                else R.drawable.ic_favorite_border
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        return RecipeViewHolder(
            ItemRecipeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // 🔥 Force UI update for the toggled item
    private fun updateItem(updatedItem: RecipeItem) {
        val newList = currentList.toMutableList() // Clone list
        val index = newList.indexOfFirst { it.id == updatedItem.id }
        if (index != -1) {
            newList[index] = updatedItem // Replace item
            submitList(newList) // Update RecyclerView
        }
    }

    fun updateData(newRecipes: List<RecipeItem>) {
        recipes = ArrayList(newRecipes) // Clone list before submitting
        submitList(recipes)
    }
}

class RecipeDiffCallback : DiffUtil.ItemCallback<RecipeItem>() {
    override fun areItemsTheSame(oldItem: RecipeItem, newItem: RecipeItem): Boolean {
        return oldItem.id == newItem.id // Compare unique IDs
    }

    override fun areContentsTheSame(oldItem: RecipeItem, newItem: RecipeItem): Boolean {
        return oldItem == newItem // Compare data fields
    }
}
