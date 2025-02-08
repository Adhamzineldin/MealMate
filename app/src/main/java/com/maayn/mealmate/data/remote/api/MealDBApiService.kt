package com.maayn.mealmate.data.remote.api

import com.maayn.mealmate.data.model.CategoryResponse
import com.maayn.mealmate.data.model.IngredientResponse
import com.maayn.mealmate.data.model.RecipeResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface MealDBApiService {
    @GET("categories.php")
    fun getMealCategories(): Call<CategoryResponse>

    @GET("filter.php")
    fun getMealsForCategory(@Query("c") category: String): Call<RecipeResponse>

    @GET("list.php?i=list")
    fun getPopularIngredients(): Call<IngredientResponse>

    @GET("random.php")
    fun getMealOfTheDay(): Call<RecipeResponse>
}
