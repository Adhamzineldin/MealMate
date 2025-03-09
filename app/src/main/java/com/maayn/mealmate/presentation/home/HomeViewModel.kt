package com.maayn.mealmate.presentation.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.maayn.mealmate.data.local.database.AppDatabase
import com.maayn.mealmate.data.local.entities.MealWithDetails
import com.maayn.mealmate.data.remote.firebase.syncingDaos.SyncingMealOfTheDayDao
import com.maayn.mealmate.presentation.home.model.RecipeItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val mealOfTheDayDao = AppDatabase.getInstance(application).mealOfTheDayDao()
    val firestore = FirebaseFirestore.getInstance()
    val syncingMealOfTheDayDao = SyncingMealOfTheDayDao(mealOfTheDayDao, firestore)

    private val _mealOfTheDay = MutableLiveData<RecipeItem?>()
    val mealOfTheDay: LiveData<RecipeItem?> get() = _mealOfTheDay

    fun fetchMealOfTheDay() {
        viewModelScope.launch {
            try {
                val today = LocalDate.now().toString()3

                // 🔥 Ensure data is up-to-date before fetching from local DB
                syncingMealOfTheDayDao.syncFromFirebase()

                // 🔍 Fetch locally stored meal of the day
                val storedMeal = mealOfTheDayDao.getMealOfTheDayDetails(today)

                _mealOfTheDay.postValue(storedMeal?.toRecipeItem())
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error fetching meal: ${e.message}")
            }
        }
    }
}
