package com.maayn.mealmate.data.remote.firebase.syncingDaos

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.maayn.mealmate.data.local.dao.MealOfTheDayDao
import com.maayn.mealmate.data.local.entities.MealOfTheDay
import com.maayn.mealmate.data.local.entities.MealWithDetails
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await

private const val TAG = "SyncingMealOfTheDayDao"

class SyncingMealOfTheDayDao(
    private val mealOfTheDayDao: MealOfTheDayDao,
    private val firestore: FirebaseFirestore
) : MealOfTheDayDao {

    private val mealOfTheDayCollection = firestore.collection("meal_of_the_day")

    override suspend fun setMealOfTheDay(meal: MealOfTheDay) {
        // Save locally first
        mealOfTheDayDao.setMealOfTheDay(meal)

        // Then sync to Firebase
        withContext(Dispatchers.IO) {
            try {
                mealOfTheDayCollection.document(meal.date).set(meal).await()
            } catch (e: Exception) {
                Log.e(TAG, "Error saving meal of the day", e)
            }
        }
    }

    override suspend fun insertMealsOfTheDay(meals: List<MealOfTheDay>) {
        // Save locally first
        mealOfTheDayDao.insertMealsOfTheDay(meals)

        // Then batch upload to Firebase
        withContext(Dispatchers.IO) {
            try {
                if (meals.isNotEmpty()) {
                    val batch = firestore.batch()
                    meals.forEach { meal ->
                        val docRef = mealOfTheDayCollection.document(meal.date)
                        batch.set(docRef, meal)
                    }
                    batch.commit().await()
                } else {
                    Log.d(TAG, "No meals to upload")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error batch inserting meals", e)
            }
        }
    }

    override suspend fun getMealOfTheDayDetails(today: String): MealWithDetails? {
        // Try to fetch from Firebase first to ensure we have the latest data
        try {
            val snapshot = mealOfTheDayCollection.document(today).get().await()
            val mealOfTheDay = snapshot.toObject(MealOfTheDay::class.java)

            // If we found it in Firebase, update local storage
            if (mealOfTheDay != null) {
                mealOfTheDayDao.setMealOfTheDay(mealOfTheDay)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching meal of day from Firebase", e)
            // If Firebase fails, we'll fall back to local data
        }

        // Return the locally stored meal
        return mealOfTheDayDao.getMealOfTheDayDetails(today)
    }

    suspend fun syncFromFirebase() {
        withContext(Dispatchers.IO) {
            try {
                val snapshot = mealOfTheDayCollection.get().await()
                val mealsOfTheDay = snapshot.documents.mapNotNull {
                    it.toObject(MealOfTheDay::class.java)
                }

                if (mealsOfTheDay.isNotEmpty()) {
                    Log.d(TAG, "Synced ${mealsOfTheDay.size} meals from Firebase")
                    mealOfTheDayDao.insertMealsOfTheDay(mealsOfTheDay)
                } else {
                    Log.d(TAG, "No meals found in Firebase")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing from Firebase", e)
            }
        }
    }
}
