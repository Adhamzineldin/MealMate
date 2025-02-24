package com.maayn.mealmate.data.local.entities

import androidx.room.Entity

@Entity(tableName = "meal_instructions", primaryKeys = ["mealId", "step"])
data class InstructionEntity(
    val mealId: String,  // Foreign key to Meal
    val step: String,
    val description: String
)
