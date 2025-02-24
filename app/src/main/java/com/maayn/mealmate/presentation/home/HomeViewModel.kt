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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate

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
        }
    }

    private suspend fun fetchMealFromApi(today: String): MealWithDetails? {
        return try {
            val response = withContext(Dispatchers.IO) { RetrofitClient.apiService.getMealOfTheDay() }
            val apiMeal = response.meals?.firstOrNull() ?: return null

            withContext(Dispatchers.IO) {


                val mealEntity = Meal(
                    id = apiMeal.id,
                    name = apiMeal.name,
                    category = apiMeal.category ?: "Unknown",
                    country = apiMeal.area ?: "Unknown",
                    imageUrl = apiMeal.imageUrl,
                    videoUrl = apiMeal.youtubeUrl.toString(),
                    isFavorite = false
                )

                val ingredients = apiMeal.extractIngredients().map {
                    IngredientEntity(mealId = mealEntity.id, name = it.name, measure = it.measure)
                }
                val instructions = apiMeal.extractInstructions().map {
                    InstructionEntity(mealId = mealEntity.id, step = it.step, description = it.step)
                }

                // 🔹 Store data in Firestore and Room
                firestore.collection("mealOfTheDay").document(today).set(mealEntity).await()
                mealDao.insertMealWithDetails(mealEntity, ingredients, instructions)
                mealOfTheDayDao.setMealOfTheDay(MealOfTheDay(mealId = mealEntity.id, date = today))
            }

            // ✅ Return the full MealWithDetails after inserting
            mealDao.getMealWithDetails(apiMeal.id)
        } catch (e: Exception) {
            Log.e("HomeViewModel", "API fetch failed: ${e.localizedMessage}")
            null
        }
    }


}
