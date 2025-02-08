package com.maayn.mealmate.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meal_plans")
data class MealPlan(
    @PrimaryKey(autoGenerate = true) val planId: Long = 0,  // Unique ID for the plan
    val userId: String,  // ID of the user (to differentiate meal plans)
    val mealId: String,  // The ID of the meal that is planned
    val dayOfWeek: String,  // Day of the week for the planned meal (e.g., "Monday", "Tuesday")
)
