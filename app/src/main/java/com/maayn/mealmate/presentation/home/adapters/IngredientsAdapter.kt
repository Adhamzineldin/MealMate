package com.maayn.mealmate.presentation.home.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.maayn.mealmate.databinding.ItemIngredientBinding
import com.maayn.mealmate.presentation.home.model.IngredientItem

class IngredientsAdapter(
    private val ingredients: List<IngredientItem>,
    private val onItemClick: (IngredientItem) -> Unit,  // Click listener for item/image
    private val onButtonClick: (IngredientItem) -> Unit  = {}
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
