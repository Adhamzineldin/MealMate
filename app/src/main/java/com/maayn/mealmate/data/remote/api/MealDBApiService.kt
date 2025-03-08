package com.maayn.mealmate.data.remote.api

import com.maayn.mealmate.data.model.CategoryResponse
import com.maayn.mealmate.data.model.CountryResponse
import com.maayn.mealmate.data.model.IngredientResponse
import com.maayn.mealmate.data.model.RecipeResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface MealDBApiService {
    @GET("categories.php")
    suspend fun getMealCategories(): CategoryResponse

    @GET("filter.php")
    suspend fun getMealsForCategory(@Query("c") category: String): RecipeResponse

    @GET("list.php?a=list")
    suspend fun getMealCountries(): CountryResponse

    @GET("list.php?i=list")
    suspend fun getPopularIngredients(): IngredientResponse

    @GET("random.php")
    suspend fun getMealOfTheDay(): RecipeResponse

    @GET("random.php")
    suspend fun getRandomMeal(): RecipeResponse

    @GET("lookup.php")
    suspend fun getMealDetails(@Query("i") mealId: String): RecipeResponse

    @GET("filter.php")
    suspend fun getMealsForArea(@Query("a") area: String): RecipeResponse

    @GET("filter.php")
    suspend fun getMealsForIngredient(@Query("i") ingredient: String): RecipeResponse
}

