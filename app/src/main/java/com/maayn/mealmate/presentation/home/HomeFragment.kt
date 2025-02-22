package com.maayn.mealmate.presentation.home


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.maayn.mealmate.core.utils.NetworkMonitor
import com.maayn.mealmate.data.local.database.AppDatabase
import com.maayn.mealmate.data.local.entities.Meal
import com.maayn.mealmate.data.local.entities.MealOfTheDay
import com.maayn.mealmate.databinding.FragmentHomeBinding
import com.maayn.mealmate.presentation.home.adapters.RecipesAdapter
import com.maayn.mealmate.presentation.home.model.CategoryItem
import com.maayn.mealmate.presentation.home.model.IngredientItem
import com.maayn.mealmate.presentation.home.model.RecipeItem
import com.maayn.mealmate.data.remote.api.RetrofitClient
import com.maayn.mealmate.presentation.home.adapters.CategoriesAdapter
import com.maayn.mealmate.presentation.home.adapters.IngredientsAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.random.Random

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private val apiService = RetrofitClient.apiService
    private lateinit var networkMonitor: NetworkMonitor

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

        setupNetworkMonitor()
        setupUI()


    }

    private fun setupNetworkMonitor() {
        networkMonitor = NetworkMonitor(requireContext()) { isConnected ->
            requireActivity().runOnUiThread {
                if (isConnected) {
                    showMainContent()
                    setupUI()
                } else {
                    showNoInternetView()
                }
            }
        }
        networkMonitor.register()
    }

    private fun setupUI() {
        fetchMealOfTheDay()
        setupGreeting()
        fetchCategories()
        fetchTrendingRecipes()
        fetchPopularIngredients()
        fetchUpcomingPlans()

    }

    private fun showNoInternetView() {
        binding.noInternetView.visibility = View.VISIBLE
        binding.mainContentView.visibility = View.GONE
    }

    private fun showMainContent() {
        binding.noInternetView.visibility = View.GONE
        binding.mainContentView.visibility = View.VISIBLE
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
        val db = AppDatabase.getInstance(requireContext())
        val mealDao = db.mealDao()
        val mealOfTheDayDao = db.mealOfTheDayDao()
        val today = LocalDate.now().toString()
        val firestore = FirebaseFirestore.getInstance()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Step 1: Check Room database first
                val storedMealOfTheDay = withContext(Dispatchers.IO) {
                    mealOfTheDayDao.getMealOfTheDay(today)
                }

                val localMeal = storedMealOfTheDay?.let {
                    withContext(Dispatchers.IO) {
                        mealDao.getMealById(it.mealId)
                    }
                }

                val meal = localMeal ?: run {
                    // Step 2: If not found in Room, check Firebase
                    val firebaseMealSnapshot = try {
                        withContext(Dispatchers.IO) {
                            firestore.collection("mealOfTheDay").document(today).get().await()
                        }
                    } catch (e: Exception) {
                        Log.e("fetchMealOfTheDay", "Firebase fetch failed: ${e.localizedMessage}")
                        null
                    }

                    val firebaseMeal = if (firebaseMealSnapshot != null && firebaseMealSnapshot.exists()) {
                        firebaseMealSnapshot.toObject(Meal::class.java)?.also { mealData ->
                            withContext(Dispatchers.IO) {
                                mealDao.insertMeal(mealData)
                                mealOfTheDayDao.setMealOfTheDay(MealOfTheDay(mealId = mealData.id, date = today))
                            }
                        }
                    } else {
                        null
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
                                Log.e("fetchMealOfTheDay", "Firebase write failed: ${e.localizedMessage}")
                            }
                            mealDao.insertMeal(mealEntity)
                            mealOfTheDayDao.setMealOfTheDay(MealOfTheDay(mealId = mealEntity.id, date = today))
                        }
                        mealEntity
                    }
                }

                // Step 5: Update UI on main thread
                meal.let {
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

                    withContext(Dispatchers.Main) {
                        binding.rvMealOfTheDay.layoutManager = LinearLayoutManager(requireContext())
                        binding.rvMealOfTheDay.adapter = RecipesAdapter(listOf(recipeItem))
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error: ${e.message}")
                handleFailure("Failure: ${e.localizedMessage}")
            }
        }
    }


    private fun fetchCategories() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getMealCategories()
                response.categories.let { categories ->
                    val categoryItems = categories.map {
                        CategoryItem(it.strCategory, it.strCategoryThumb)
                    }
                    binding.rvCategories.apply {
                        layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                        adapter = CategoriesAdapter(categoryItems)
                    }
                }
            } catch (e: Exception) {
               handleFailure("Failure: ${e.localizedMessage}")
            }
        }
    }

    private fun fetchTrendingRecipes() {
        lifecycleScope.launch {
            try {
                // Fetch categories
                val categoryResponse = RetrofitClient.apiService.getMealCategories()
                val categories = categoryResponse.categories

                if (categories.isNotEmpty()) {
                    val randomCategory = categories.random().strCategory
                    // Fetch recipes for the selected category
                    Log.i("HomeFragment", "Selected category: $randomCategory")
                    fetchRecipesForCategory(randomCategory)
                } else {
                    handleFailure("No categories found.")
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Selected category: ${e.message}")
                handleFailure("Failure: ${e.localizedMessage}")
            }
        }
    }



    private fun fetchRecipesForCategory(category: String) {
        lifecycleScope.launch {
            try {
                // Fetch recipes for the category
                val response = RetrofitClient.apiService.getMealsForCategory(category)

                val recipeItems = response.meals.take(10).map { meal ->
                    val imageUrl = meal.strMealThumb  // Recipe image URL
                    val randomRating = listOf(1, 2, 3, 4, 5, 1.5f, 2.5f, 3.5f, 4.5f, 5.0f).random().toFloat()
                    val randomTime = Random.nextInt(10, 61)  // Random time between 10 and 60 min

                    RecipeItem(
                        meal.idMeal,
                        meal.strMeal,
                        "$randomTime minutes",
                        randomRating,
                        imageUrl,
                        category = meal.strCategory
                    )
                }

                binding.rvTrendingRecipes.apply {
                    layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                    adapter = RecipesAdapter(recipeItems)
                }

            } catch (e: Exception) {
                handleFailure("Failure: ${e.localizedMessage}")
            }
        }
    }


    private fun handleFailure(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        Log.e("Toast", message)
    }


    private fun fetchPopularIngredients() {
        lifecycleScope.launch {
            try {
                // Fetch ingredients from API
                val response = RetrofitClient.apiService.getPopularIngredients()

                val ingredientItems = response.meals.take(10).map { ingredient ->
                    val imageUrl = "https://www.themealdb.com/images/ingredients/${ingredient.strIngredient}.png"
                    val randomGrams = (50..500).random() // Generates a random number between 50 and 500 grams
                    IngredientItem(ingredient.strIngredient, imageUrl, randomGrams)
                }

                // Update UI on the main thread
                binding.rvPopularIngredients.apply {
                    layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                    adapter = IngredientsAdapter(ingredientItems)
                }

            } catch (e: Exception) {
                handleFailure("Failure: ${e.localizedMessage}")
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        networkMonitor.unregister()

    }
}
