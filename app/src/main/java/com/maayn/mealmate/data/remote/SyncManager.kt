package com.maayn.mealmate.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.maayn.mealmate.data.local.database.AppDatabase
import com.maayn.mealmate.data.local.entities.FavoriteMeal
import com.maayn.mealmate.data.local.entities.Ingredient
import com.maayn.mealmate.data.local.entities.Meal
import com.maayn.mealmate.data.local.entities.MealOfTheDay
import com.maayn.mealmate.data.local.entities.MealPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

class SyncManager(private val db: AppDatabase) {

    private val firestore = FirebaseFirestore.getInstance()

    // 🔹 Fetch all data from Firestore & store in Room
    suspend fun fetchAllDataFromFirestore(userId: String) {
        withContext(Dispatchers.IO) {
            try {
                // Fetch Meals
                val mealDocs = firestore.collection("meals").get().await()
                val meals = mealDocs.toObjects(Meal::class.java)
                db.mealDao().insertMeals(meals)

                // Fetch Favorites
                val favoriteDocs = firestore.collection("users")
                    .document(userId)
                    .collection("favorites")
                    .get()
                    .await()
                val favorites = favoriteDocs.toObjects(FavoriteMeal::class.java)
                db.favoriteMealDao().insertFavorites(favorites)

                // Fetch Meal Plans
                val mealPlanDocs = firestore.collection("users")
                    .document(userId)
                    .collection("mealPlans")
                    .get()
                    .await()
                val mealPlans = mealPlanDocs.toObjects(MealPlan::class.java)
                db.mealPlanDao().insertMealPlans(mealPlans)

                // Fetch Ingredients
                val ingredientDocs = firestore.collection("ingredients").get().await()
                val ingredients = ingredientDocs.toObjects(Ingredient::class.java)
                db.ingredientDao().insertIngredients(ingredients)

                // Fetch Shopping List
                val shoppingDocs = firestore.collection("users")
                    .document(userId)
                    .collection("shoppingList")
                    .get()
                    .await()
                val shoppingItems = shoppingDocs.toObjects(Ingredient::class.java)
                db.shoppingListDao().insertShoppingItems(shoppingItems)

                // Fetch Meal of the Day
                val mealOfTheDayDocs = firestore.collection("users")
                    .document(userId)
                    .collection("mealOfTheDay")
                    .get()
                    .await()
                val mealOfTheDay = mealOfTheDayDocs.toObjects(MealOfTheDay::class.java)
                db.mealOfTheDayDao().insertMealOfTheDay(mealOfTheDay)

            } catch (e: Exception) {
                Log.e("SyncManager", "Error fetching data: ${e.localizedMessage}")
            }
        }
    }

    // 🔹 Efficiently syncs all data
    suspend fun syncAll(userId: String) {
        withContext(Dispatchers.IO) {
            fetchAllDataFromFirestore(userId)
            syncMealsWithFirebase()
            syncFavoritesWithFirebase(userId)
            syncMealPlansWithFirebase(userId)
            syncIngredientsWithFirebase()
            syncShoppingListWithFirebase(userId)
            syncMealOfTheDayWithFirebase(userId)
        }
    }

    suspend fun syncMealsWithFirebase() {
        val meals = db.mealDao().getAllMeals()
        val batch = firestore.batch()

        for (meal in meals) {
            val mealRef = firestore.collection("meals").document(meal.id.toString())
            batch.set(mealRef, meal)
        }

        batch.commit().addOnFailureListener { e ->
            Log.e("SyncManager", "Failed to sync meals: ${e.localizedMessage}")
        }
    }

    suspend fun syncFavoritesWithFirebase(userId: String) {
        val favorites = db.favoriteMealDao().getAllFavoriteMeals()
        val batch = firestore.batch()

        for (fav in favorites) {
            val favRef = firestore.collection("users")
                .document(userId)
                .collection("favorites")
                .document(fav.id.toString())
            batch.set(favRef, fav)
        }

        batch.commit().addOnFailureListener { e ->
            Log.e("SyncManager", "Failed to sync favorites: ${e.localizedMessage}")
        }
    }

    suspend fun syncMealPlansWithFirebase(userId: String) {
        val mealPlans = db.mealPlanDao().getMealPlansForUser(userId)
        val batch = firestore.batch()

        for (plan in mealPlans) {
            val planRef = firestore.collection("users")
                .document(userId)
                .collection("mealPlans")
                .document(plan.planId.toString())
            batch.set(planRef, plan)
        }

        batch.commit().addOnFailureListener { e ->
            Log.e("SyncManager", "Failed to sync meal plans: ${e.localizedMessage}")
        }
    }

    suspend fun syncIngredientsWithFirebase() {
        val ingredients = db.ingredientDao().getAllIngredients()
        val batch = firestore.batch()

        for (ingredient in ingredients) {
            val ingRef = firestore.collection("ingredients").document(ingredient.id.toString())
            batch.set(ingRef, ingredient)
        }

        batch.commit().addOnFailureListener { e ->
            Log.e("SyncManager", "Failed to sync ingredients: ${e.localizedMessage}")
        }
    }

    suspend fun syncShoppingListWithFirebase(userId: String) {
        val shoppingItems = db.shoppingListDao().getShoppingList()
        val batch = firestore.batch()

        for (item in shoppingItems) {
            val itemRef = firestore.collection("users")
                .document(userId)
                .collection("shoppingList")
                .document(item.id.toString())
            batch.set(itemRef, item)
        }

        batch.commit().addOnFailureListener { e ->
            Log.e("SyncManager", "Failed to sync shopping list: ${e.localizedMessage}")
        }
    }

    suspend fun syncMealOfTheDayWithFirebase(userId: String) {
        val today = LocalDate.now().toString()
        val mealOfTheDay = db.mealOfTheDayDao().getMealOfTheDay(today)

        mealOfTheDay?.let {
            firestore.collection("users")
                .document(userId)
                .collection("mealOfTheDay")
                .document(it.id.toString())
                .set(it)
                .addOnFailureListener { e ->
                    Log.e("SyncManager", "Failed to sync meal of the day: ${e.localizedMessage}")
                }
        }
    }
}
