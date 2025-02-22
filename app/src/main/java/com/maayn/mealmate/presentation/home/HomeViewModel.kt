package com.maayn.mealmate.presentation.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.maayn.mealmate.data.local.dao.MealDao
import com.maayn.mealmate.data.local.dao.MealOfTheDayDao
import com.maayn.mealmate.data.local.database.AppDatabase
import com.maayn.mealmate.data.local.entities.Meal
import com.maayn.mealmate.data.local.entities.MealOfTheDay
import com.maayn.mealmate.data.remote.api.RetrofitClient
import com.maayn.mealmate.presentation.home.model.RecipeItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.random.Random

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val mealDao: MealDao
    private val mealOfTheDayDao: MealOfTheDayDao
    private val firestore = FirebaseFirestore.getInstance()

    private val _mealOfTheDay = MutableLiveData<RecipeItem?>()
    val mealOfTheDay: LiveData<RecipeItem?> get() = _mealOfTheDay

    init {
        val db = AppDatabase.getInstance(application)
        mealDao = db.mealDao()
        mealOfTheDayDao = db.mealOfTheDayDao()
    }

    fun fetchMealOfTheDay() {
        viewModelScope.launch {
            try {
                val today = LocalDate.now().toString()

                // Step 1: Check Room database first
                val storedMealOfTheDay = withContext(Dispatchers.IO) {
                    mealOfTheDayDao.getMealOfTheDay(today)
                }

                val localMeal = storedMealOfTheDay?.let {
                    withContext(Dispatchers.IO) { mealDao.getMealById(it.mealId) }
                }

                val meal = localMeal ?: run {
                    // Step 2: If not found in Room, check Firebase
                    val firebaseMealSnapshot = try {
                        withContext(Dispatchers.IO) {
                            firestore.collection("mealOfTheDay").document(today).get().await()
                        }
                    } catch (e: Exception) {
                        Log.e("HomeViewModel", "Firebase fetch failed: ${e.localizedMessage}")
                        null
                    }

                    val firebaseMeal = firebaseMealSnapshot?.toObject(Meal::class.java)?.also { mealData ->
                        withContext(Dispatchers.IO) {
                            mealDao.insertMeal(mealData)
                            mealOfTheDayDao.setMealOfTheDay(MealOfTheDay(mealId = mealData.id, date = today))
                        }
                    }

                    firebaseMeal ?: run {
                        // Step 3: If not found in Firebase, fetch from API
                        val response = withContext(Dispatchers.IO) {
                            RetrofitClient.apiService.getMealOfTheDay()
                        }

                        val apiMeal = response.meals?.firstOrNull()
                            ?: throw Exception("No meal data returned from API")

                        val mealEntity = Meal(
                            id = apiMeal.idMeal,
                            name = apiMeal.strMeal,
                            imageUrl = apiMeal.strMealThumb,
                            isFavorite = false,
                            mealOfTheDay = true,
                            country = "todo",
                            ingredients = emptyList(),
                            steps = emptyList(),
                            videoUrl = "todo"
                        )

                        // Step 4: Store meal in Firebase and Room
                        withContext(Dispatchers.IO) {
                            try {
                                firestore.collection("mealOfTheDay").document(today).set(mealEntity).await()
                            } catch (e: Exception) {
                                Log.e("HomeViewModel", "Firebase write failed: ${e.localizedMessage}")
                            }
                            mealDao.insertMeal(mealEntity)
                            mealOfTheDayDao.setMealOfTheDay(MealOfTheDay(mealId = mealEntity.id, date = today))
                        }
                        mealEntity
                    }
                }

                // Step 5: Update LiveData
                meal?.let {
                    val randomRating = listOf(1, 2, 3, 4, 5, 1.5f, 2.5f, 3.5f, 4.5f, 5.0f).random().toFloat()
                    val randomTime = Random.nextInt(10, 61)
                    val recipeItem = RecipeItem(
                        id = it.id,
                        name = it.name,
                        time = "$randomTime minutes",
                        rating = randomRating,
                        imageUrl = it.imageUrl,
                        category = "todo"
                    )

                    _mealOfTheDay.postValue(recipeItem)
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error: ${e.message}")
            }
        }
    }
}
