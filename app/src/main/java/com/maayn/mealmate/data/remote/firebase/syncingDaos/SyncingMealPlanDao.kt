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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SyncingMealPlanDao(
    private val mealPlanDao: MealPlanDao,
    private val firestore: FirebaseFirestore,
    private val userId: String? = FirebaseAuth.getInstance().currentUser?.uid // Default to current user if null

) : MealPlanDao {

    private fun userMealPlansCollection() =
        firestore.collection("users").document(userId.toString()).collection("meal_plans")

    // 🔹 **INSERT MEAL PLAN**
    override suspend fun insertMealPlan(mealPlan: MealPlan) {
        if (mealPlan.firebaseId == null) {
            mealPlan.firebaseId = userMealPlansCollection().document().id // ✅ Ensure Firestore ID
        }

        mealPlanDao.insertMealPlan(mealPlan) // ✅ Save locally

        CoroutineScope(Dispatchers.IO).launch {
            userMealPlansCollection().document(mealPlan.firebaseId!!).set(mealPlan)
        }
    }

    // 🔹 **GET ALL MEAL PLANS**
    override fun getAllMealPlans(): LiveData<List<MealPlan>> {
        return mealPlanDao.getAllMealPlans().map { localMealPlans ->
            if (localMealPlans.isEmpty()) {
                CoroutineScope(Dispatchers.IO).launch { syncMealPlansFromFirebase() }
            }
            localMealPlans
        }
    }

    // 🔹 **GET UPCOMING MEAL PLANS**
    override fun getUpcomingMealPlans(today: String): LiveData<List<MealPlan>> {
        return mealPlanDao.getUpcomingMealPlans(today).map { localMealPlans ->
            if (localMealPlans.isEmpty()) {
                CoroutineScope(Dispatchers.IO).launch { syncMealPlansFromFirebase() }
            }
            localMealPlans
        }
    }

    // 🔹 **GET MEAL PLAN BY ID**
    override suspend fun getMealPlanById(id: Int?): MealPlan? {
        return mealPlanDao.getMealPlanById(id)
    }

    override suspend fun getMealPlanByFirebaseId(firebaseId: String?): List<MealPlan>? {
        return mealPlanDao.getMealPlanByFirebaseId(firebaseId)
    }

    // 🔹 **UPDATE MEAL PLAN**
    override suspend fun updateMealPlan(mealPlan: MealPlan) {
        mealPlanDao.updateMealPlan(mealPlan) // ✅ Update locally

        mealPlan.firebaseId?.let { firebaseId ->
            CoroutineScope(Dispatchers.IO).launch {
                userMealPlansCollection()
                    .document(firebaseId)
                    .set(mealPlan, SetOptions.merge()) // ✅ Merge instead of overwrite
            }
        }
    }

    // 🔹 **DELETE MEAL PLAN**
    override suspend fun deleteMealPlan(mealPlan: MealPlan) {
        mealPlanDao.deleteMealPlan(mealPlan) // ✅ Delete locally

        mealPlan.firebaseId?.let { firebaseId ->
            try {
                userMealPlansCollection().document(firebaseId).delete().await()
                Log.i("SyncingMealPlanDao", "Deleted meal plan from Firebase: $firebaseId")
            } catch (e: Exception) {
                Log.e("SyncingMealPlanDao", "Error deleting meal plan from Firebase", e)
            }
        }
    }

    // 🔹 **SYNC MEAL PLANS FROM FIREBASE TO ROOM**
    suspend fun syncMealPlansFromFirebase() {
        try {
            val snapshot = userMealPlansCollection().get().await()
            val mealPlans = snapshot.toObjects(MealPlan::class.java)

            Log.i("MealPlanDao", "Syncing ${mealPlans.size} meal plans from Firebase")

            mealPlans.forEach { mealPlan ->
                mealPlanDao.insertMealPlan(mealPlan)
            }
        } catch (e: Exception) {
            Log.e("SyncingMealPlanDao", "Error syncing meal plans from Firebase", e)
        }
    }
}
