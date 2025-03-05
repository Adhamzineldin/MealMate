package com.maayn.mealmate.presentation.mealplan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.maayn.mealmate.data.local.database.AppDatabase
import com.maayn.mealmate.data.local.entities.MealPlan
import kotlinx.coroutines.launch

class MealPlanViewModel(application: Application) : AndroidViewModel(application) {
    private val mealPlanDao = AppDatabase.getInstance(application).mealPlanDao()
    val allMealPlans: LiveData<List<MealPlan>> = mealPlanDao.getAllMealPlans()

    private val _emptyState = MutableLiveData<Boolean>()
    val emptyState: LiveData<Boolean> get() = _emptyState

    fun insertMealPlan(mealPlan: MealPlan) {
        viewModelScope.launch {
            mealPlanDao.insertMealPlan(mealPlan)
        }
    }
}
