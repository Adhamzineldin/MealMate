package com.maayn.mealmate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import androidx.room.OnConflictStrategy
import com.maayn.mealmate.data.local.entities.FavoriteMeal

@Dao
interface FavoriteMealDao {
    @Insert
    suspend fun addFavoriteMeal(favoriteMeal: FavoriteMeal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorites(favorites: List<FavoriteMeal>)

    @Delete
    suspend fun removeFavoriteMeal(favoriteMeal: FavoriteMeal)

    @Query("SELECT * FROM favorite_meals")
    suspend fun getAllFavoriteMeals(): List<FavoriteMeal>

    @Query("SELECT * FROM favorite_meals WHERE id = :mealId")
    suspend fun getFavoriteMealById(mealId: String): FavoriteMeal?
}
