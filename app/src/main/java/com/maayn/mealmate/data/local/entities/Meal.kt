package com.maayn.mealmate.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.random.Random

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
    val time: String = "${Random.nextInt(10, 90)} min",
    val rating: Float = Random.nextDouble(3.0, 5.0).toFloat(),
    val ratingCount: Int = Random.nextInt(10, 1000)
)
