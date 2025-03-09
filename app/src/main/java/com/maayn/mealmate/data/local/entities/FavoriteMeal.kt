package com.maayn.mealmate.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_meals")
data class FavoriteMeal(
    @PrimaryKey
    var id: String = ""  // Firestore needs mutable properties
) {
    constructor() : this("") // No-argument constructor for Firestore
}
