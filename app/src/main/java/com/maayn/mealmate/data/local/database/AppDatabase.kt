package com.maayn.mealmate.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.google.firebase.firestore.FirebaseFirestore
import com.maayn.mealmate.data.local.dao.*
import com.maayn.mealmate.data.local.entities.*
import com.maayn.mealmate.data.remote.firebase.syncingDaos.SyncingFavoriteMealDao
import com.maayn.mealmate.data.remote.firebase.syncingDaos.SyncingIngredientDao
import com.maayn.mealmate.data.remote.firebase.syncingDaos.SyncingMealDao
import com.maayn.mealmate.data.remote.firebase.syncingDaos.SyncingMealOfTheDayDao
import com.maayn.mealmate.data.remote.firebase.syncingDaos.SyncingMealPlanDao
import com.maayn.mealmate.data.remote.firebase.syncingDaos.SyncingShoppingItemDao
import com.maayn.mealmate.data.remote.firebase.syncingDaos.SyncingShoppingListDao

@Database(
    entities = [Meal::class, FavoriteMeal::class, MealPlan::class, Ingredient::class, ShoppingList::class,
        MealOfTheDay::class, InstructionEntity::class, IngredientEntity::class, ShoppingItem::class],
    version = 15
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    // Original DAOs
    abstract fun realMealDao(): MealDao
    abstract fun realFavoriteMealDao(): FavoriteMealDao
    abstract fun realMealPlanDao(): MealPlanDao
    abstract fun realIngredientDao(): IngredientDao
    abstract fun realShoppingListDao(): ShoppingListDao
    abstract fun realMealOfTheDayDao(): MealOfTheDayDao
    abstract fun realShoppingItemDao(): ShoppingItemDao

    fun mealDao(): MealDao {
        return SyncingMealDao(realMealDao(), FirebaseFirestore.getInstance())
    }

    fun favoriteMealDao(): FavoriteMealDao {
        return SyncingFavoriteMealDao(realFavoriteMealDao(), FirebaseFirestore.getInstance())
    }

    fun mealPlanDao(): MealPlanDao {
        return SyncingMealPlanDao(realMealPlanDao(), FirebaseFirestore.getInstance())
    }

    fun ingredientDao(): IngredientDao {
        return SyncingIngredientDao(realIngredientDao(), FirebaseFirestore.getInstance())
    }

    fun shoppingListDao(): ShoppingListDao {
        return SyncingShoppingListDao(realShoppingListDao(), FirebaseFirestore.getInstance())
    }

    fun mealOfTheDayDao(): MealOfTheDayDao {
        return SyncingMealOfTheDayDao(realMealOfTheDayDao(), FirebaseFirestore.getInstance())
    }

    fun shoppingItemDao(): ShoppingItemDao {
        return SyncingShoppingItemDao(realShoppingItemDao(), FirebaseFirestore.getInstance())
    }

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context?): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context?.applicationContext ?: throw IllegalStateException("Context is null"),
                    AppDatabase::class.java,
                    "meal_database"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
