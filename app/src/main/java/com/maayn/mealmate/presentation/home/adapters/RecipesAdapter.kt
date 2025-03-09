package com.maayn.mealmate.presentation.home.adapters

import android.app.AlertDialog
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
import com.maayn.mealmate.data.local.entities.MealPlan
import com.maayn.mealmate.presentation.home.model.toMealWithDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecipesAdapter(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope,
    private var recipes: List<RecipeItem>? = emptyList(),
    private val onRecipeClick: (RecipeItem) -> Unit,
    private val onCreateMealPlanButtonClick: (MealPlan) -> Unit
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
                val db = AppDatabase.getInstance(context)
                val favoriteDao = db.favoriteMealDao()
                lifecycleScope.launch(Dispatchers.IO) {
                    val isFavorited = favoriteDao.getFavoriteMealDetailsById(item.id) != null
                    updateFavoriteIcon(isFavorited)
                }


                ivFavorite.setOnClickListener {
                    val updatedItem = item.copy(isFavorited = !item.isFavorited) // Toggle favorite
                    updateFavoriteInDatabase(updatedItem)
                    updateItem(updatedItem) // Refresh UI
                }

                btnCreateMealPlan.setOnClickListener {
                    onCreateMealPlanButtonClick(item)
                }

                // 🔥 Use the passed click listener
                root.setOnClickListener { onRecipeClick(item) }
            }
        }

        private fun onCreateMealPlanButtonClick(item: RecipeItem) {
            val mealPlan = MealPlan(
                id = null,
                name = "",
                date = null,
                mealType = "breakfast",
                recipeName = item.name,
                recipeImage = item.imageUrl,
                recipeId = item.id
            )
            onCreateMealPlanButtonClick(mealPlan)
        }


        private fun updateFavoriteInDatabase(updatedItem: RecipeItem) {
            val updatedItemAndClass = updatedItem.toMealWithDetails()
            val context = context // Ensure you have a valid context

            lifecycleScope.launch(Dispatchers.Main) {
                val db = AppDatabase.getInstance(context)
                val favoriteDao = db.favoriteMealDao()

                val existingItem = withContext(Dispatchers.IO) {
                    favoriteDao.getFavoriteMealDetailsById(updatedItemAndClass.meal.id)
                }

                if (existingItem != null) {
                    // Show confirmation popup before deleting
                    AlertDialog.Builder(context)
                        .setTitle("Remove Favorite")
                        .setMessage("Are you sure you want to remove ${updatedItem.name} from favorites?")
                        .setPositiveButton("Remove") { _, _ ->
                            lifecycleScope.launch(Dispatchers.IO) {
                                try {
                                    favoriteDao.deleteFavoriteMeal(updatedItemAndClass.meal.id)
                                    Log.e("DB", "Removed favorite: ${updatedItem.name}")

                                    // Update UI on the main thread
                                    withContext(Dispatchers.Main) {
                                        updatedItem.isFavorited = false // Update the item state
                                        notifyDataSetChanged() // Refresh adapter
                                    }
                                } catch (e: Exception) {
                                    Log.e("DB_ERROR", "Failed to remove favorite: ${e.message}")
                                }
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                } else {
                    // Directly add to favorites
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            favoriteDao.insertMealWithDetails(updatedItemAndClass)
                            Log.e("DB", "Added to favorites: ${updatedItem.name}")

                            // Update UI on the main thread
                            withContext(Dispatchers.Main) {
                                updatedItem.isFavorited = true // Update the item state
                                notifyDataSetChanged() // Refresh adapter
                            }
                        } catch (e: Exception) {
                            Log.e("DB_ERROR", "Failed to add favorite: ${e.message}")
                        }
                    }
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

    private fun updateItem(updatedItem: RecipeItem) {
        val newList = currentList.toMutableList()
        val index = newList.indexOfFirst { it.id == updatedItem.id }
        if (index != -1) {
            newList[index] = updatedItem
            submitList(newList)
        }
    }

    fun updateData(newRecipes: List<RecipeItem>) {
        recipes = ArrayList(newRecipes)
        submitList(recipes)
    }
}

class RecipeDiffCallback : DiffUtil.ItemCallback<RecipeItem>() {
    override fun areItemsTheSame(oldItem: RecipeItem, newItem: RecipeItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: RecipeItem, newItem: RecipeItem): Boolean {
        return oldItem == newItem
    }
}
