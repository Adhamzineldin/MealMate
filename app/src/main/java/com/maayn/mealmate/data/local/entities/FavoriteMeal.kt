package com.maayn.mealmate.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_meals")
data class FavoriteMeal(
    @PrimaryKey val id: String,  // Unique meal ID
    val mealName: String,
    val imageUrl: String,
    val country: String,
    val isFavorite: Boolean = true // Track whether the meal is in the favorite list
)
