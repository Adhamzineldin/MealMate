package com.maayn.mealmate.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meals")
data class Meal(
    @PrimaryKey
    val id: String,
    val name: String = "",
    val imageUrl: String = "",
    val isFavorite: Boolean = false,
    val mealOfTheDay: Boolean = false,
    val country: String = "",
    val videoUrl: String = "",
    val category: String = "",
    val time: String = "",
    val rating: Float = 0f,
    val ratingCount: Int = 0
)
