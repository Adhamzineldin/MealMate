package com.maayn.mealmate.data.remote.firebase.syncingDaos

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.maayn.mealmate.data.local.dao.MealPlanDao
import com.maayn.mealmate.data.local.entities.MealPlan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "SyncingMealPlanDao"

class SyncingMealPlanDao(
    private val mealPlanDao: MealPlanDao,
    private val firestore: FirebaseFirestore,
    private val userId: String? = FirebaseAuth.getInstance().currentUser?.uid
) : MealPlanDao {

    private fun userMealPlansCollection() =
        firestore.collection("users").document(userId ?: "").collection("meal_plans")

    override suspend fun insertMealPlan(mealPlan: MealPlan) {
        // Generate Firebase ID if none exists
        if (mealPlan.firebaseId == null) {
            mealPlan.firebaseId = userMealPlansCollection().document().id
        }

        // Save locally first
        mealPlanDao.insertMealPlan(mealPlan)

        // Then sync to Firebase
        withContext(Dispatchers.IO) {
            try {
                userMealPlansCollection().document(mealPlan.firebaseId!!).set(mealPlan).await()
            } catch (e: Exception) {
                Log.e(TAG, "Error inserting meal plan", e)
            }
        }
    }

    override fun getAllMealPlans(): LiveData<List<MealPlan>> {
        // Trigger sync if local data is empty
        return mealPlanDao.getAllMealPlans().map { localMealPlans ->
            if (localMealPlans.isEmpty()) {
                // Launch a coroutine to call the suspend function
                CoroutineScope(Dispatchers.IO).launch {
                    syncMealPlansFromFirebase()
                }
                localMealPlans // Return value for the if branch
            } else {
                localMealPlans // Return value for the else branch
            }
        }
    }

    override fun getUpcomingMealPlans(today: String): LiveData<List<MealPlan>> {
        // Trigger sync if local data is empty
        return mealPlanDao.getUpcomingMealPlans(today).map { localMealPlans ->
            if (localMealPlans.isEmpty()) {
                // Launch a coroutine to call the suspend function
                CoroutineScope(Dispatchers.IO).launch {
                    syncMealPlansFromFirebase()
                }
                localMealPlans // Return value for the if branch
            } else {
                localMealPlans // Return value for the else branch
            }
        }
    }

    override suspend fun getMealPlanById(id: Int?): MealPlan? {
        return mealPlanDao.getMealPlanById(id)
    }

    override suspend fun getMealPlanByFirebaseId(firebaseId: String?): List<MealPlan>? {
        return mealPlanDao.getMealPlanByFirebaseId(firebaseId)
    }

    override suspend fun updateMealPlan(mealPlan: MealPlan) {
        // Update locally first
        mealPlanDao.updateMealPlan(mealPlan)

        // Then update in Firebase
        mealPlan.firebaseId?.let { firebaseId ->
            withContext(Dispatchers.IO) {
                try {
                    userMealPlansCollection()
                        .document(firebaseId)
                        .set(mealPlan, SetOptions.merge())
                        .await()
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating meal plan", e)
                }
            }
        }
    }

    override suspend fun deleteMealPlan(mealPlan: MealPlan) {
        // Delete locally first
        mealPlanDao.deleteMealPlan(mealPlan)

        // Then delete from Firebase
        mealPlan.firebaseId?.let { firebaseId ->
            withContext(Dispatchers.IO) {
                try {
                    userMealPlansCollection().document(firebaseId).delete().await()
                    Log.i(TAG, "Deleted meal plan from Firebase: $firebaseId")
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting meal plan from Firebase", e)
                }
            }
        }
    }

    suspend fun syncMealPlansFromFirebase() {
        withContext(Dispatchers.IO) {
            try {
                val snapshot = userMealPlansCollection().get().await()
                val mealPlans = snapshot.toObjects(MealPlan::class.java)

                if (mealPlans.isNotEmpty()) {
                    Log.i(TAG, "Syncing ${mealPlans.size} meal plans from Firebase")
                    mealPlans.forEach { mealPlan ->
                        mealPlanDao.insertMealPlan(mealPlan)
                    }
                } else {
                    Log.i(TAG, "No meal plans found in Firebase")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing meal plans from Firebase", e)
            }
        }
    }
}