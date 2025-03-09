package com.maayn.mealmate.data.local.entities

import androidx.room.Entity

@Entity(tableName = "meal_instructions", primaryKeys = ["mealId", "step"])
data class InstructionEntity(
    var mealId: String = "",  // Foreign key to Meal
    var step: String = "",
    var description: String = ""
) {
    constructor() : this("", "", "") // No-argument constructor for Firestore
}
