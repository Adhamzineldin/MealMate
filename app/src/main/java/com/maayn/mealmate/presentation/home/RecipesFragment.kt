package com.maayn.mealmate.presentation.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.maayn.mealmate.core.utils.NetworkMonitor
import com.maayn.mealmate.data.local.dao.MealDao
import com.maayn.mealmate.data.local.database.AppDatabase
import com.maayn.mealmate.data.local.entities.IngredientEntity
import com.maayn.mealmate.data.local.entities.InstructionEntity
import com.maayn.mealmate.data.local.entities.Meal
import com.maayn.mealmate.data.local.entities.MealWithDetails
import com.maayn.mealmate.data.local.entities.toDomain
import com.maayn.mealmate.data.model.Category
import com.maayn.mealmate.data.model.extractIngredients
import com.maayn.mealmate.data.model.extractInstructions
import com.maayn.mealmate.data.remote.api.RetrofitClient
import com.maayn.mealmate.databinding.FragmentRecipesBinding
import com.maayn.mealmate.presentation.home.adapters.RecipesAdapter
import com.maayn.mealmate.presentation.home.model.RecipeItem
import com.maayn.mealmate.presentation.home.model.toMealWithDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

class RecipesFragment : Fragment() {
    private var _binding: FragmentRecipesBinding? = null
    private val binding get() = _binding!!
    private lateinit var networkMonitor: NetworkMonitor

    // State management
    private val currentCategory = MutableStateFlow("All")
    private val searchQuery = MutableStateFlow("")
    private var allRecipes = emptyList<RecipeItem>()
    private val semaphore = Semaphore(5) // Limit concurrent network requests
    private lateinit var mealDao: MealDao // Inject your DAO

    // Adapter using ListAdapter with DiffUtil
    var recipesAdapter: RecipesAdapter ?= null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecipesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recipesAdapter = RecipesAdapter(
            requireContext(),
            viewLifecycleOwner.lifecycleScope,
            recipes = emptyList(), // You might want to pass actual recipe items here
            onRecipeClick = { recipe ->
                val action = RecipesFragmentDirections.actionRecipesFragmentToRecipeDetailsFragment(recipe.id)
                findNavController().navigate(action)
            },
            onCreateMealPlanButtonClick = { mealPlan ->
                val action = RecipesFragmentDirections.actionRecipesFragmentToCreateMealPlanFragment(mealPlan)
                findNavController().navigate(action)
            }
        )


        setupNetworkMonitor()
        setupUI()
        setupObservers()
        setupListeners()
    }




    private fun setupNetworkMonitor() {
        networkMonitor = NetworkMonitor(requireContext()) { isConnected ->
            activity?.runOnUiThread {
                binding.emptyState.isVisible = !isConnected
                if (isConnected && allRecipes.isEmpty()) fetchRecipes()
            }
        }
        networkMonitor.register()
    }


    private fun setupUI() {

        binding.rvRecipes.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recipesAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            combine(
                currentCategory,
                searchQuery.debounce(300.milliseconds)
            ) { category, query -> Pair(category, query) }
                .flowOn(Dispatchers.IO) // Move heavy processing off the main thread
                .collectLatest { (category, query) ->
                    val filtered = allRecipes.filter {
                        (it.category == category || category == "All") &&
                                it.name.contains(query, true)
                    }
                    withContext(Dispatchers.Main) { showRecipes(filtered) }
                }
        }
    }


    private fun setupListeners() {
        binding.apply {
            btnBack.setOnClickListener { activity?.onBackPressed() }
            btnFilter.setOnClickListener { showFilterComingSoon() }
            etSearch.addTextChangedListener { text ->
                searchQuery.value = text?.toString()?.trim() ?: ""
            }
        }
    }



    private fun fetchRecipes() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { showLoading() }

                // Initialize database
                val db = AppDatabase.getInstance(requireContext())
                mealDao = db.mealDao()

                // Check local database first
                val cachedRecipes = mealDao.getAllMealWithDetails()?.map { mealWithDetails ->
                    RecipeItem(
                        id = mealWithDetails.meal.id,
                        name = mealWithDetails.meal.name,
                        imageUrl = mealWithDetails.meal.imageUrl,
                        area = mealWithDetails.meal.country,
                        isFavorited = mealWithDetails.meal.isFavorite,
                        time = mealWithDetails.meal.time,
                        rating = mealWithDetails.meal.rating,
                        ratingCount = mealWithDetails.meal.ratingCount,
                        category = mealWithDetails.meal.category,
                        ingredients = mealWithDetails.ingredients.map { it.toDomain() },
                        instructions = mealWithDetails.instructions.map { it.toDomain() }
                    )
                } ?: emptyList()

                Log.d("RecipesFragment", "Fetching 10 random meals from API")
                val newRecipes = fetchRandomMeals(10) // ✅ Fetch 10 random meals from API

                // Save new meals to the database
                saveRecipesToDatabase(newRecipes)

                // Combine cached and new meals, removing duplicates by `id`
                val allMeals = (cachedRecipes + newRecipes).distinctBy { it.id }

                // Extract unique categories
                val categories = allMeals.map { Category(strCategory = it.category) }.distinct()

                withContext(Dispatchers.Main) {
                    allRecipes = allMeals
                    setupCategoryChips(categories) // ✅ Ensure category chips are updated properly
                    updateRecipes() // ✅ UI now contains both cached and new meals
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) { showError("Failed to load recipes: ${e.message}") }
            } finally {
                withContext(Dispatchers.Main) { hideLoading() }
            }
        }
    }



    private suspend fun fetchRandomMeals(count: Int): List<RecipeItem> {
        val recipes = mutableListOf<RecipeItem>()

        repeat(count) {
            val mealResponse = RetrofitClient.apiService.getRandomMeal() // Get a random meal
            val meal = mealResponse.meals?.firstOrNull() ?: return@repeat

            // ✅ Extract ingredients and instructions properly
            val ingredients = meal.extractIngredients()
            val instructions = meal.extractInstructions()

            recipes.add(
                RecipeItem(
                    id = meal.id,
                    name = meal.name,
                    imageUrl = meal.imageUrl,
                    area = meal.area ?: "Unknown",
                    category = meal.category ?: "Unknown",
                    youtubeUrl = meal.youtubeUrl,
                    ingredients = ingredients,
                    instructions = instructions,
                    isFavorited = false,
                    time = "${(10..60).random()} min",
                    rating = (3..5).random().toFloat(),
                    ratingCount = (10..500).random()
                )
            )
        }

        return recipes
    }







    private suspend fun saveRecipesToDatabase(recipes: List<RecipeItem>) {
        withContext(Dispatchers.IO) {
            val meals = recipes.map { recipe ->
                recipe.toMealWithDetails()
            }

           mealDao.insertMealsWithDetails(meals)
        }
    }




    private suspend fun fetchAllRecipes(categories: List<Category>): List<RecipeItem> {
        return coroutineScope {
            categories.map { category ->
                async(Dispatchers.IO) {
                    val cachedMeals = mealDao.getMealsWithDetailsByCategory(category.strCategory)

                    if (cachedMeals.isNotEmpty()) {
                        Log.d("RecipesFragment", "Loaded category '${category.strCategory}' from cache")
                        cachedMeals.map { mealWithDetails ->
                            RecipeItem(
                                id = mealWithDetails.meal.id,
                                name = mealWithDetails.meal.name,
                                imageUrl = mealWithDetails.meal.imageUrl,
                                area = mealWithDetails.meal.country,
                                isFavorited = mealWithDetails.meal.isFavorite,
                                time = mealWithDetails.meal.time,
                                rating = mealWithDetails.meal.rating,
                                ratingCount = mealWithDetails.meal.ratingCount,
                                category = mealWithDetails.meal.category,
                                ingredients = mealWithDetails.ingredients.map { it.toDomain() },
                                instructions = mealWithDetails.instructions.map { it.toDomain() }
                            )
                        }
                    } else {
                        Log.d("RecipesFragment", "Fetching category '${category.strCategory}' from API")
                        fetchRecipesForCategory(category)
                    }
                }
            }.awaitAll().flatten().shuffled()
        }
    }



    private suspend fun fetchRecipesForCategory(category: Category): List<RecipeItem> {
        return try {
            semaphore.withPermit {
                val mealsWithDetails = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.getMealsForCategory(category.strCategory).meals?.mapNotNull { mealDto ->
                        val detailsResponse = RetrofitClient.apiService.getMealDetails(mealDto.id)
                        val mealDetails = detailsResponse.meals?.firstOrNull() ?: return@mapNotNull null
                        // Convert API response to Room entities
                        val mealEntity = Meal(
                            id = mealDetails.id,
                            name = mealDetails.name,
                            imageUrl = mealDetails.imageUrl,
                            country = mealDetails.area ?: "Unknown",
                            isFavorite = false,  // Default to false if not in favorites
                            time = "${Random.nextInt(10, 61)} minutes",
                            rating = listOf(1f, 1.5f, 2f, 2.5f, 3f, 3.5f, 4f, 4.5f, 5f).random(),
                            ratingCount = Random.nextInt(10, 500),
                            category = category.strCategory
                        )

                        val ingredients = mealDetails.extractIngredients().map { IngredientEntity(mealId = mealEntity.id, name = it.name, measure = it.measure) }
                        val instructions = mealDetails.extractInstructions().map { InstructionEntity(mealId = mealEntity.id, step = it.step, description = it.step) }

                        MealWithDetails(meal = mealEntity, ingredients = ingredients, instructions = instructions)
                    }
                }

                // Convert MealWithDetails to RecipeItem


                 mealsWithDetails?.map { mealWithDetails ->
                    RecipeItem(
                        id = mealWithDetails.meal.id,
                        name = mealWithDetails.meal.name,
                        imageUrl = mealWithDetails.meal.imageUrl,
                        area = mealWithDetails.meal.country,
                        isFavorited = mealWithDetails.meal.isFavorite,
                        time = mealWithDetails.meal.time,
                        rating = mealWithDetails.meal.rating,
                        ratingCount = mealWithDetails.meal.ratingCount,
                        category = mealWithDetails.meal.category,
                        ingredients = mealWithDetails.ingredients.map { it.toDomain() },
                        instructions = mealWithDetails.instructions.map { it.toDomain() }
                    )
                } ?: emptyList()




            }
        } catch (e: Exception) {
            Log.e("RecipesFragment", "Error fetching recipes: ${e.message}")
            emptyList()
        }
    }


    private fun setupCategoryChips(categories: List<Category>) {
        binding.chipGroup.removeAllViews()
        addChip("All", true)
        categories.forEach { addChip(it.strCategory, false) }
    }

    private fun addChip(label: String, isSelected: Boolean) {
        Chip(requireContext()).apply {
            text = label
            isCheckable = true
            isChecked = isSelected
            setOnCheckedChangeListener { _, checked ->
                if (checked) currentCategory.value = label
            }
            binding.chipGroup.addView(this)
        }
    }

    private fun updateRecipes() {
        lifecycleScope.launch(Dispatchers.IO) {
            val currentQuery = searchQuery.value
            val currentCategory = currentCategory.value

            val filtered = allRecipes.filter {
                (it.category == currentCategory || currentCategory == "All") &&
                        it.name.contains(currentQuery, true)
            }

            withContext(Dispatchers.Main) { showRecipes(filtered) }
        }
    }


    private fun showLoading() {
        // binding.progressBar.isVisible = true
        binding.rvRecipes.isVisible = false
        binding.emptyState.isVisible = false
    }

    private fun hideLoading() {
        // binding.progressBar.isVisible = false
    }

    private fun showRecipes(recipes: List<RecipeItem>) {
        binding.rvRecipes.isVisible = true
        binding.emptyState.isVisible = false
        recipesAdapter?.updateData(recipes)
    }

    private fun showEmptyState() {
        binding.rvRecipes.isVisible = false
        binding.emptyState.isVisible = true
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        showEmptyState()
    }

    private fun showFilterComingSoon() {
        Toast.makeText(context, "Filter feature coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun showRecipeDetails(recipe: RecipeItem) {
        // Implement navigation to recipe details
    }

    override fun onDestroyView() {
        super.onDestroyView()
        networkMonitor.unregister()

    }

}
