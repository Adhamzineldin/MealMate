package com.maayn.mealmate.presentation.home


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.maayn.mealmate.core.utils.NetworkMonitor
import com.maayn.mealmate.data.model.extractIngredients
import com.maayn.mealmate.data.model.extractInstructions
import com.maayn.mealmate.databinding.FragmentHomeBinding
import com.maayn.mealmate.presentation.home.adapters.RecipesAdapter
import com.maayn.mealmate.presentation.home.model.CategoryItem
import com.maayn.mealmate.presentation.home.model.IngredientItem
import com.maayn.mealmate.presentation.home.model.RecipeItem
import com.maayn.mealmate.data.remote.api.RetrofitClient
import com.maayn.mealmate.presentation.home.adapters.CategoriesAdapter
import com.maayn.mealmate.presentation.home.adapters.IngredientsAdapter
import kotlinx.coroutines.launch
import kotlin.random.Random

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private val apiService = RetrofitClient.apiService
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var viewModel: HomeViewModel

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
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        // Observe LiveData
        viewModel.mealOfTheDay.observe(viewLifecycleOwner) { meal ->
            meal?.let {
                binding.rvMealOfTheDay.layoutManager = LinearLayoutManager(requireContext())
                binding.rvMealOfTheDay.adapter = RecipesAdapter(requireContext(), viewLifecycleOwner.lifecycleScope,listOf(it))
            }
        }

        // Fetch data
        viewModel.fetchMealOfTheDay()
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



                // Map API response to RecipeItem list
                val recipeItems = response.meals?.take(10)?.map { meal ->
                    val randomRating = listOf(1, 2, 3, 4, 5, 1.5f, 2.5f, 3.5f, 4.5f, 5.0f).random().toFloat()
                    val randomTime = Random.nextInt(10, 61)  // Random time between 10 and 60 min

                    RecipeItem(
                        id = meal.id,
                        name = meal.name,
                        category = category,
                        area = meal.area ?: "Unknown",
                        instructions = meal.extractInstructions(),
                        imageUrl = meal.imageUrl,
                        youtubeUrl = meal.youtubeUrl,
                        ingredients = meal.extractIngredients(),
                        isFavorited = false
                    )
                }

                // Update RecyclerView
                binding.rvTrendingRecipes.apply {
                    layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                    adapter = RecipesAdapter(requireContext(), viewLifecycleOwner.lifecycleScope, recipeItems)
                }

            } catch (e: Exception) {
                Log.e("HomeFragment", "Error fetching recipes: ${e.localizedMessage}", e)
                handleFailure("Failed to load recipes: ${e.localizedMessage}")
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
