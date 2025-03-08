package com.maayn.mealmate.presentation.mealplan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.maayn.mealmate.R
import com.maayn.mealmate.data.local.entities.MealPlan
import com.maayn.mealmate.utils.MealPlanDiffCallback

class MealPlanAdapter(
    private val onStartCookingClick: (MealPlan) -> Unit,  // Passes MealPlan for navigation
    private val onEditClick: (MealPlan) -> Unit           // Passes MealPlan for edit
) : ListAdapter<MealPlan, MealPlanAdapter.MealPlanViewHolder>(MealPlanDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealPlanViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_meal_plan, parent, false)
        return MealPlanViewHolder(view)
    }

    override fun onBindViewHolder(holder: MealPlanViewHolder, position: Int) {
        holder.bind(getItem(position), onStartCookingClick, onEditClick)
    }

    class MealPlanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMealPlanName: TextView = itemView.findViewById(R.id.tvMealPlanName)
        private val tvDateMealType: TextView = itemView.findViewById(R.id.tvDateMealType)
        private val tvRecipeName: TextView = itemView.findViewById(R.id.tvRecipeName)
        private val ivRecipeImage: ImageView = itemView.findViewById(R.id.ivRecipeImage)
        private val btnEdit: MaterialButton = itemView.findViewById(R.id.btnEdit)
        private val btnStartCooking: MaterialButton = itemView.findViewById(R.id.btnStartCooking)

        fun bind(mealPlan: MealPlan, onStartCookingClick: (MealPlan) -> Unit, onEditClick: (MealPlan) -> Unit) {
            tvMealPlanName.text = mealPlan.name
            tvDateMealType.text = "${mealPlan.date} • ${mealPlan.mealType}"
            tvRecipeName.text = mealPlan.recipeName
            Glide.with(itemView).load(mealPlan.recipeImage).into(ivRecipeImage)

            btnEdit.setOnClickListener {
                onEditClick(mealPlan) // Calls edit function
            }

            btnStartCooking.setOnClickListener {
                onStartCookingClick(mealPlan) // Calls navigation function
            }
        }
    }
}
