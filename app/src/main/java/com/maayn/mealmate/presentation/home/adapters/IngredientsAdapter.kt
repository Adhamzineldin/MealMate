package com.maayn.mealmate.presentation.home.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.maayn.mealmate.data.local.database.AppDatabase
import com.maayn.mealmate.data.local.entities.ShoppingItem
import com.maayn.mealmate.databinding.ItemIngredientBinding
import com.maayn.mealmate.presentation.home.model.IngredientItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID

class IngredientsAdapter(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val ingredients: List<IngredientItem>,
    private val onItemClick: (IngredientItem) -> Unit,  // Click listener for item/image

) : RecyclerView.Adapter<IngredientsAdapter.IngredientViewHolder>() {

    inner class IngredientViewHolder(private val binding: ItemIngredientBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: IngredientItem) {
            binding.apply {
                Glide.with(root.context)
                    .load(item.imageUrl)
                    .into(ivIngredient)

                tvIngredientName.text = item.name
                tvGrams.text = "${item.grams}g"

                // Set click listener on the entire item (including the image)
                root.setOnClickListener { onItemClick(item) }
                ivIngredient.setOnClickListener { onItemClick(item) }

                // Set click listener for a button (Assuming a button exists in ItemIngredientBinding)
                btnAddToList.setOnClickListener { onButtonClick(item) } // Replace 'btnAction' with actual button ID
            }
        }
    }

    fun onButtonClick(item: IngredientItem) {
        coroutineScope.launch {
            val db = AppDatabase.getInstance(context)
            val shoppingItem = ShoppingItem(UUID.randomUUID().toString(), item.name)
            db.shoppingItemDao().insert(shoppingItem)
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IngredientViewHolder {
        return IngredientViewHolder(
            ItemIngredientBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: IngredientViewHolder, position: Int) {
        holder.bind(ingredients[position])
    }

    override fun getItemCount(): Int = ingredients.size
}
