package com.maayn.mealmate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.maayn.mealmate.data.local.entities.MealOfTheDay
import java.time.LocalDate

@Dao
interface MealOfTheDayDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setMealOfTheDay(meal: MealOfTheDay)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealOfTheDay(mealOfTheDay: List<MealOfTheDay>)

    @Query("SELECT * FROM meal_of_the_day WHERE date = :today LIMIT 1")
    suspend fun getMealOfTheDay(today: String): MealOfTheDay?
}
