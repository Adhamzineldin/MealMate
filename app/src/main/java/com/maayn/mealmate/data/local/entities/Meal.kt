package com.maayn.mealmate.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.maayn.mealmate.data.local.database.Converters

@Entity(tableName = "meals")
@TypeConverters(Converters::class)
data class Meal(
    @PrimaryKey
    val id: String,
    val name: String = "",
    val imageUrl: String = "",
    val isFavorite: Boolean = false,
    val mealOfTheDay: Boolean = false,
    val country: String = "",
    val ingredients: List<String> = emptyList(),
    val steps: List<String> = emptyList(),
    val videoUrl: String = ""
) {
    // Firestore needs a no-argument constructor
    constructor() : this("", "", "", false, false, "", emptyList(), emptyList(), "")
}

