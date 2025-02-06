package com.maayn.mealmate.presentation.home


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.maayn.mealmate.data.model.CategoryResponse
import com.maayn.mealmate.data.model.IngredientResponse
import com.maayn.mealmate.data.model.RecipeResponse
import com.maayn.mealmate.databinding.FragmentHomeBinding
import com.maayn.mealmate.presentation.home.adapters.RecipesAdapter
import com.maayn.mealmate.presentation.home.model.CategoryItem
import com.maayn.mealmate.presentation.home.model.IngredientItem
import com.maayn.mealmate.presentation.home.model.RecipeItem
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.maayn.mealmate.R
import com.maayn.mealmate.presentation.home.adapters.CategoriesAdapter
import com.maayn.mealmate.presentation.home.adapters.IngredientsAdapter
import kotlin.random.Random

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private val apiService = RetrofitClient.apiService

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        setupGreeting()
        fetchCategories()
        fetchTrendingRecipes()
        fetchPopularIngredients()
        fetchUpcomingPlans()
        fetchMealOfTheDay()
    }

    private fun setupGreeting() {
        val userName = getUserName()
        binding.tvGreeting.text = "Hello $userName! 👋"
        binding.tvCookingPrompt.text = "Want to Cook some delicious Meals?"
    }

    private fun getUserName(): String {
        val user = auth.currentUser
        val displayName = user?.displayName ?: "Guest"
        val nameParts = displayName.split(" ")
        return if (nameParts.size >= 2) {
            "${nameParts[0]} ${nameParts[1]}"
        } else {
            displayName
        }
    }

    private fun fetchUpcomingPlans() {


    }

    private fun fetchMealOfTheDay() {
        // Make the API call to fetch the meal of the day (using a different endpoint)
        apiService.getMealOfTheDay().enqueue(object : Callback<RecipeResponse> {
            override fun onResponse(call: Call<RecipeResponse>, response: Response<RecipeResponse>) {
                if (response.isSuccessful) {
                    response.body()?.meals?.take(1)?.let { meals ->  // Only one meal to show
                        meals.map { meal ->
                            // Assuming we need to generate a similar structure as in the other function:
                            val imageUrl = meal.strMealThumb  // Image URL for the meal
                            val mealName = meal.strMeal  // Name of the meal

                            // Generate random rating and time as an example (for a single meal)
                            val randomRating = listOf(1, 2, 3, 4, 5, 1.5f, 2.5f, 3.5f, 4.5f, 5.0f).random().toFloat()
                            val randomTime = Random.nextInt(10, 61)

                            // Create a single item for the RecyclerView
                            val mealItem = RecipeItem(meal.idMeal, mealName, "$randomTime minutes", randomRating, imageUrl)

                            // Setting up the RecyclerView adapter
                            binding.rvMealOfTheDay.apply {
                                layoutManager = LinearLayoutManager(requireContext())
                                adapter = RecipesAdapter(listOf(mealItem))
                            }
                        }
                    }
                } else {
                    handleFailure("Error: ${response.code()} - ${response.message()}")
                }
            }

            override fun onFailure(call: Call<RecipeResponse>, t: Throwable) {
                handleFailure("Failure: ${t.localizedMessage}")
            }
        })
    }







    private fun fetchCategories() {
        RetrofitClient.apiService.getMealCategories().enqueue(object : Callback<CategoryResponse> {
            override fun onResponse(call: Call<CategoryResponse>, response: Response<CategoryResponse>) {
                response.body()?.categories?.let { categories ->
                    val categoryItems = categories.map {
                        CategoryItem(it.strCategory, it.strCategoryThumb)
                    }
                    binding.rvCategories.apply {
                        layoutManager = GridLayoutManager(requireContext(), 4)
                        adapter = CategoriesAdapter(categoryItems)
                    }
                }
            }

            override fun onFailure(call: Call<CategoryResponse>, t: Throwable) {
                // Handle failure (e.g., show a Toast)
            }
        })
    }


    private fun fetchTrendingRecipes() {
        // Fetch categories first
        apiService.getMealCategories().enqueue(object : Callback<CategoryResponse> {
            override fun onResponse(call: Call<CategoryResponse>, response: Response<CategoryResponse>) {
                if (response.isSuccessful) {
                    response.body()?.categories?.let { categories ->

                        val randomCategory = categories.random().strCategory
                        // Fetch recipes for the selected category
                        fetchRecipesForCategory(randomCategory)
                    }
                } else {
                    handleFailure("Error: ${response.code()} - ${response.message()}")
                }
            }

            override fun onFailure(call: Call<CategoryResponse>, t: Throwable) {
                handleFailure("Failure: ${t.localizedMessage}")
            }
        })
    }


    private fun fetchRecipesForCategory(category: String) {
        // Make the API call to fetch recipes for the category (only passing category)
        apiService.getMealsForCategory(category).enqueue(object : Callback<RecipeResponse> {
            override fun onResponse(call: Call<RecipeResponse>, response: Response<RecipeResponse>) {
                if (response.isSuccessful) {
                    response.body()?.meals?.take(10)?.let { meals ->  // Limiting to max 10 items
                        val recipeItems = meals.map { meal ->
                            val imageUrl = meal.strMealThumb // Image URL for the recipe

                            val randomRating = listOf(1, 2, 3, 4, 5, 1.5f, 2.5f, 3.5f, 4.5f, 5.0f).random().toFloat()

                            // Generate random time between 10 and 60 minutes
                            val randomTime = Random.nextInt(10, 61)

                            // Create RecipeItem with random time and rating
                            RecipeItem(meal.idMeal ,meal.strMeal, "$randomTime minutes", randomRating, imageUrl)
                        }
                        binding.rvTrendingRecipes.apply {
                            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                            adapter = RecipesAdapter(recipeItems)
                        }
                    }
                } else {
                    handleFailure("Error: ${response.code()} - ${response.message()}")
                }
            }

            override fun onFailure(call: Call<RecipeResponse>, t: Throwable) {
                handleFailure("Failure: ${t.localizedMessage}")
            }
        })
    }


    private fun handleFailure(message: String) {
        Toast.makeText(requireContext(), "No Internet Connection", Toast.LENGTH_LONG).show()
    }


    private fun fetchPopularIngredients() {
        apiService.getPopularIngredients().enqueue(object : Callback<IngredientResponse> {
            override fun onResponse(call: Call<IngredientResponse>, response: Response<IngredientResponse>) {
                if (response.isSuccessful) {
                    response.body()?.meals?.take(10)?.let { ingredients ->  // Limiting to max 10 items
                        val ingredientItems = ingredients.map { ingredient ->
                            val imageUrl = "https://www.themealdb.com/images/ingredients/${ingredient.strIngredient}.png"
                            val randomGrams = (50..500).random() // Generates a random number between 50 and 300 grams
                            IngredientItem(ingredient.strIngredient, imageUrl, randomGrams)
                        }

                        binding.rvPopularIngredients.apply {
                            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                            adapter = IngredientsAdapter(ingredientItems)
                        }
                    }
                } else {
                    handleFailure("Error: ${response.code()} - ${response.message()}")
                }
            }

            override fun onFailure(call: Call<IngredientResponse>, t: Throwable) {
                handleFailure("Failure: ${t.localizedMessage}")
            }
        })
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
