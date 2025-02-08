package com.maayn.mealmate.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.maayn.mealmate.data.local.database.Converters

@Entity(tableName = "meals")
@TypeConverters(Converters::class)
data class Meal(
    @PrimaryKey val id: String,
    val name: String,
    val imageUrl: String,
    val country: String,
    val ingredients: List<String>, // Stores ingredients as a JSON string
    val steps: List<String>,
    val videoUrl: String,
    val isFavorite: Boolean = false,
    val mealOfTheDay: Boolean = false  // Added to track the "Meal of the Day"
)
