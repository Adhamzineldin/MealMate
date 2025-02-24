package com.maayn.mealmate.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_meals")
data class FavoriteMeal(
    @PrimaryKey
    val id: String,
//    val name: String = "",
//    val imageUrl: String = "",
//    val isFavorite: Boolean = false,
//    val mealOfTheDay: Boolean = false,
//    val country: String = "",
//    val ingredients: List<String> = emptyList(),
//    val steps: List<String> = emptyList(),
//    val videoUrl: String = "",
//    val time: Float
)

