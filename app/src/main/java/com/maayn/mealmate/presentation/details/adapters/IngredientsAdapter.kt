package com.maayn.mealmate.presentation.details.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.maayn.mealmate.data.local.entities.IngredientEntity
import com.maayn.mealmate.databinding.ItemIngredientDetailsBinding

class IngredientsAdapter(private val ingredients: List<IngredientEntity>) :
    RecyclerView.Adapter<IngredientsAdapter.IngredientViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IngredientViewHolder {
        val binding = ItemIngredientDetailsBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return IngredientViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IngredientViewHolder, position: Int) {
        holder.bind(ingredients[position])
    }

    override fun getItemCount(): Int = ingredients.size

    inner class IngredientViewHolder(private val binding: ItemIngredientDetailsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(ingredient: IngredientEntity) {
            binding.tvIngredientName.text = ingredient.name
            binding.tvIngredientMeasure.text = ingredient.measure
        }
    }
}
