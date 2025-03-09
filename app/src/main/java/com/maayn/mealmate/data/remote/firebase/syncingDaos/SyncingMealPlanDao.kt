package com.maayn.mealmate.data.remote.firebase.syncingDaos

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.google.firebase.firestore.FirebaseFirestore
import com.maayn.mealmate.data.local.dao.MealPlanDao
import com.maayn.mealmate.data.local.entities.MealPlan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class SyncingMealPlanDao(
    private val mealPlanDao: MealPlanDao,
    private val firestore: FirebaseFirestore
) : MealPlanDao {

    override suspend fun insertMealPlan(mealPlan: MealPlan) {
        // Ensure Firestore ID is set
        if (mealPlan.firebaseId == null) {
            mealPlan.firebaseId = firestore.collection("meal_plans").document().id
        }

        mealPlanDao.insertMealPlan(mealPlan) // Save locally

        // Sync to Firebase
        CoroutineScope(Dispatchers.IO).launch {
            firestore.collection("meal_plans").document(mealPlan.firebaseId!!).set(mealPlan)
        }
    }

    override fun getAllMealPlans(): LiveData<List<MealPlan>> {
        return mealPlanDao.getAllMealPlans().map { localMealPlans ->
            // Sync only once, not on every call
            if (localMealPlans.isEmpty()) {
                CoroutineScope(Dispatchers.IO).launch { syncMealPlansFromFirebase() }
            }
            localMealPlans
        }
    }

    override fun getUpcomingMealPlans(today: String): LiveData<List<MealPlan>> {
        return mealPlanDao.getUpcomingMealPlans(today).map { localMealPlans ->
            // Sync only if local DB is empty
            if (localMealPlans.isEmpty()) {
                CoroutineScope(Dispatchers.IO).launch { syncMealPlansFromFirebase() }
            }
            localMealPlans
        }
    }

    override suspend fun getMealPlanById(id: Int?): MealPlan? {
        return mealPlanDao.getMealPlanById(id)
    }

    override suspend fun updateMealPlan(mealPlan: MealPlan) {
        mealPlanDao.updateMealPlan(mealPlan) // Update locally

        // Sync updated meal plan to Firebase
        CoroutineScope(Dispatchers.IO).launch {
            mealPlan.firebaseId?.let {
                firestore.collection("meal_plans").document(it).set(mealPlan)
            }
        }
    }

    /**
     * Sync meal plans from Firebase to local Room database
     */
    suspend fun syncMealPlansFromFirebase() {
        val snapshot = firestore.collection("meal_plans").get().await()
        val mealPlans = snapshot.toObjects(MealPlan::class.java)
        Log.i("MealPlanDao", "Raw Firebase Data: ${snapshot.documents}")

        Log.i("MealPlanDao", "Syncing ${mealPlans} meal plans from Firebase")

        mealPlans.forEach { mealPlan ->
            val existingMealPlan = mealPlanDao.getMealPlanById(mealPlan.id)
            if (existingMealPlan == null) { // Only insert if it doesn't exist
                mealPlanDao.insertMealPlan(mealPlan)
            }
        }
    }
}

