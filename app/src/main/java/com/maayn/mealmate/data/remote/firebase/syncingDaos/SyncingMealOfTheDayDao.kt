package com.maayn.mealmate.data.remote.firebase.syncingDaos

import com.google.firebase.firestore.FirebaseFirestore
import com.maayn.mealmate.data.local.dao.MealOfTheDayDao
import com.maayn.mealmate.data.local.entities.MealOfTheDay
import com.maayn.mealmate.data.local.entities.MealWithDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SyncingMealOfTheDayDao(
    private val mealOfTheDayDao: MealOfTheDayDao,
    private val firestore: FirebaseFirestore
) : MealOfTheDayDao {

    override suspend fun setMealOfTheDay(meal: MealOfTheDay) {
        mealOfTheDayDao.setMealOfTheDay(meal) // Save locally

        // Sync to Firebase in the background
        CoroutineScope(Dispatchers.IO).launch {
            firestore.collection("meal_of_the_day").document(meal.date).set(meal)
        }
    }

    override suspend fun insertMealsOfTheDay(meals: List<MealOfTheDay>) {
        mealOfTheDayDao.insertMealsOfTheDay(meals) // Save locally

        // Sync all meals to Firebase
        CoroutineScope(Dispatchers.IO).launch {
            val batch = firestore.batch()
            meals.forEach { meal ->
                val docRef = firestore.collection("meal_of_the_day").document(meal.date)
                batch.set(docRef, meal)
            }
            batch.commit()
        }
    }

    override suspend fun getMealOfTheDayDetails(today: String): MealWithDetails? {
        // Fetch from Firestore first
        val snapshot = firestore.collection("meal_of_the_day").document(today).get().await()
        val mealOfTheDay = snapshot.toObject(MealOfTheDay::class.java)

        // If meal exists in Firestore, store it locally
        mealOfTheDay?.let {
            mealOfTheDayDao.setMealOfTheDay(it)
        }

        // Return the locally stored meal
        return mealOfTheDayDao.getMealOfTheDayDetails(today)
    }

    // 🔥 Sync data from Firestore to Room Database
    suspend fun syncFromFirebase() {
        try {
            val snapshot = firestore.collection("meal_of_the_day").get().await()
            val mealsOfTheDay: List<MealOfTheDay> = snapshot.documents.mapNotNull { doc ->
                doc.toObject(MealOfTheDay::class.java)
            }

            // Insert only if new data exists
            if (mealsOfTheDay.isNotEmpty()) {
                mealOfTheDayDao.insertMealsOfTheDay(mealsOfTheDay) // Insert new data
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

