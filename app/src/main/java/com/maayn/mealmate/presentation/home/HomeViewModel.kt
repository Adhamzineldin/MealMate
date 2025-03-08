package com.maayn.mealmate.presentation.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.maayn.mealmate.data.local.database.AppDatabase
import com.maayn.mealmate.data.local.entities.*
import com.maayn.mealmate.data.model.extractIngredients
import com.maayn.mealmate.data.model.extractInstructions
import com.maayn.mealmate.data.remote.api.RetrofitClient
import com.maayn.mealmate.presentation.home.model.RecipeItem
import com.maayn.mealmate.presentation.home.model.toMealWithDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.random.Random

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val mealDao = AppDatabase.getInstance(application).mealDao()
    private val mealOfTheDayDao = AppDatabase.getInstance(application).mealOfTheDayDao()
    private val firestore = FirebaseFirestore.getInstance()

    private val _mealOfTheDay = MutableLiveData<RecipeItem?>()
    val mealOfTheDay: LiveData<RecipeItem?> get() = _mealOfTheDay

    fun fetchMealOfTheDay() {
        viewModelScope.launch {
            try {
                val today = LocalDate.now().toString()

                firestore.enableNetwork().await()

                val storedMeal = withContext(Dispatchers.IO) {
                    mealOfTheDayDao.getMealOfTheDayDetails(today)
                }

                val mealWithDetails = storedMeal ?: fetchMealFromFirebase(today) ?: fetchMealFromApi(today)

                mealWithDetails?.let { meal ->
                    _mealOfTheDay.postValue(meal.toRecipeItem())
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error: ${e.message}")
            }
        }
    }

    private suspend fun fetchMealFromFirebase(today: String): MealWithDetails? {
        return try {
            val snapshot = withContext(Dispatchers.IO) {
                firestore.collection("mealOfTheDay").document(today).get().await()
            }
            val firebaseMeal = snapshot.toObject(Meal::class.java)
            firebaseMeal?.let { meal ->
                withContext(Dispatchers.IO) {
                    mealDao.insertMeal(meal)
                    mealOfTheDayDao.setMealOfTheDay(MealOfTheDay(mealId = meal.id, date = today))
                }
                mealDao.getMealWithDetails(meal.id)
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Firebase fetch failed: ${e.localizedMessage}")
            null
        } as MealWithDetails?
    }

    private suspend fun fetchMealFromApi(today: String): MealWithDetails? {
        return try {
            val response = withContext(Dispatchers.IO) { RetrofitClient.apiService.getMealOfTheDay() }
            val apiMeal = response.meals?.firstOrNull() ?: return null

            withContext(Dispatchers.IO) {
                val detailsResponse = RetrofitClient.apiService.getMealDetails(apiMeal.id)
                val mealDetails = detailsResponse.meals?.firstOrNull()

                // Convert API response to Room entities
                val mealEntity = Meal(
                    id = mealDetails?.id ?: "",
                    name = mealDetails?.name ?: "",
                    imageUrl = mealDetails?.imageUrl ?: "",
                    country = mealDetails?.area ?: "Unknown",
                    isFavorite = false,  // Default to false if not in favorites
                    time = "${Random.nextInt(10, 61)} minutes",
                    rating = listOf(1f, 1.5f, 2f, 2.5f, 3f, 3.5f, 4f, 4.5f, 5f).random(),
                    ratingCount = Random.nextInt(10, 500),
                    category = mealDetails?.category.toString()
                )

                val ingredients = mealDetails?.extractIngredients()?.map { IngredientEntity(mealId = mealEntity.id, name = it.name, measure = it.measure) } ?: emptyList()
                val instructions = mealDetails?.extractInstructions()?.map { InstructionEntity(mealId = mealEntity.id, step = it.step, description = it.step) } ?: emptyList()

                val meal = MealWithDetails(meal = mealEntity, ingredients = ingredients, instructions = instructions)
                val recipe_item = meal.toRecipeItem()



                // 🔹 Store data in Firestore and Room
                firestore.collection("mealOfTheDay").document(today).set(mealEntity).await()
                mealDao.insertMealWithDetails(recipe_item.toMealWithDetails())
                mealOfTheDayDao.setMealOfTheDay(MealOfTheDay(mealId = mealEntity.id, date = today))
            }

            // ✅ Return the full MealWithDetails after inserting
            mealDao.getMealWithDetails(apiMeal.id)
        } catch (e: Exception) {
            Log.e("HomeViewModel", "API fetch failed: ${e.localizedMessage}")
            null
        } as MealWithDetails?
    }


}
