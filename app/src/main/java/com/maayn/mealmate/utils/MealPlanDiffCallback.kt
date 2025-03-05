package com.maayn.mealmate.utils

import androidx.recyclerview.widget.DiffUtil
import com.maayn.mealmate.data.local.entities.MealPlan

class MealPlanDiffCallback : DiffUtil.ItemCallback<MealPlan>() {
    override fun areItemsTheSame(oldItem: MealPlan, newItem: MealPlan): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: MealPlan, newItem: MealPlan): Boolean {
        return oldItem == newItem
    }
}
