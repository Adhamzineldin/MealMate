package com.maayn.mealmate.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.random.Random

@Entity(tableName = "meals")
data class Meal(
    @PrimaryKey
    val id: String = "",  // Provide default value
    val name: String = "",
    val imageUrl: String = "",
    val isFavorite: Boolean = false,
    val mealOfTheDay: Boolean = false,
    val country: String = "",
    val videoUrl: String = "",
    val category: String = "",
    val time: String = "10 min",  // Remove Random generator for Firebase compatibility
    val rating: Float = 3.0f,
    val ratingCount: Int = 10
) {
    // Explicit no-arg constructor (not needed but ensures Firebase compatibility)
    constructor() : this("")
}
