package com.maayn.mealmate.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.maayn.mealmate.data.local.dao.*
import com.maayn.mealmate.data.local.entities.*

@Database(
    entities = [Meal::class, FavoriteMeal::class, MealPlan::class, Ingredient::class, ShoppingList::class,
        MealOfTheDay::class, InstructionEntity::class, IngredientEntity::class, ShoppingItem::class],
    version = 12
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao
    abstract fun favoriteMealDao(): FavoriteMealDao
    abstract fun mealPlanDao(): MealPlanDao
    abstract fun ingredientDao(): IngredientDao
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun mealOfTheDayDao(): MealOfTheDayDao
    abstract fun shoppingItemDao(): ShoppingItemDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context?): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context?.applicationContext ?: throw IllegalStateException("Context is null"),
                    AppDatabase::class.java,
                    "meal_database"
                ).fallbackToDestructiveMigration() .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
