package com.maayn.mealmate.presentation.search

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.maayn.mealmate.data.local.dao.MealDao
import com.maayn.mealmate.data.local.database.AppDatabase
import com.maayn.mealmate.data.local.entities.IngredientEntity
import com.maayn.mealmate.data.local.entities.InstructionEntity
import com.maayn.mealmate.data.local.entities.Meal
import com.maayn.mealmate.data.local.entities.MealWithDetails
import com.maayn.mealmate.data.local.entities.toDomain
import com.maayn.mealmate.data.model.extractIngredients
import com.maayn.mealmate.data.model.extractInstructions
import com.maayn.mealmate.data.remote.api.RetrofitClient
import com.maayn.mealmate.databinding.FragmentRecipesBinding
import com.maayn.mealmate.presentation.home.adapters.RecipesAdapter
import com.maayn.mealmate.presentation.home.model.RecipeItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class FilteredRecipesFragment : Fragment() {
    private var _binding: FragmentRecipesBinding? = null
    private val binding get() = _binding!!

    private lateinit var mealDao: MealDao
    private lateinit var filterType: String
    private lateinit var filterValue: String
    private var allRecipes = emptyList<RecipeItem>()

    private var recipesAdapter: RecipesAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            filterType = it.getString("filterType", "category") // Default to category
            filterValue = it.getString("filterValue", "All")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecipesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mealDao = AppDatabase.getInstance(requireContext()).mealDao()

        recipesAdapter = RecipesAdapter(
            requireContext(),
            viewLifecycleOwner.lifecycleScope,
            recipes = emptyList(), // You might want to pass actual recipe items here
            onRecipeClick = { recipe ->
                val action = FilteredRecipesFragmentDirections
                    .actionFilteredRecipesFragmentToRecipeDetailsFragment(recipe.id)
                findNavController().navigate(action)
            },
            onCreateMealPlanButtonClick = { mealPlan ->
                val action = FilteredRecipesFragmentDirections
                    .actionFilteredRecipesFragmentToCreateMealPlanFragment(mealPlan)
                findNavController().navigate(action)
            }
        )

        setupUI()
        fetchFilteredRecipes()
    }

    private fun setupUI() {
        binding.rvRecipes.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recipesAdapter
            setHasFixedSize(true)
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun fetchFilteredRecipes() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { showLoading() }

                val cachedRecipes = getCachedRecipes()
                val newRecipes = if (cachedRecipes.isEmpty()) fetchFromApi() else emptyList()

                val allMeals = (cachedRecipes + newRecipes).distinctBy { it.id }
                allRecipes = allMeals

                withContext(Dispatchers.Main) { showRecipes(allMeals) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { showError("Failed to load recipes: ${e.message}") }
            } finally {
                withContext(Dispatchers.Main) { hideLoading() }
            }
        }
    }

    private suspend fun getCachedRecipes(): List<RecipeItem> {
        return withContext(Dispatchers.IO) {
            val mealsWithDetails: List<MealWithDetails> = when (filterType) {
                "category" -> mealDao.getMealsWithDetailsByCategory(filterValue)
                "area" -> mealDao.getMealsWithDetailsByArea(filterValue)
                "ingredient" -> mealDao.getMealsWithDetailsByIngredient(filterValue)
                else -> emptyList()
            }

            mealsWithDetails.map { it.toRecipeItem() }
        }
    }

    private suspend fun fetchFromApi(): List<RecipeItem> {
        return try {
            withContext(Dispatchers.IO) {
                val response = when (filterType) {
                    "category" -> RetrofitClient.apiService.getMealsForCategory(filterValue)
                    "area" -> RetrofitClient.apiService.getMealsForArea(filterValue)
                    "ingredient" -> RetrofitClient.apiService.getMealsForIngredient(filterValue)
                    else -> return@withContext emptyList()
                }

                response.meals?.mapNotNull { mealDto ->
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
                        category = filterValue
                    )

                    val ingredients = mealDetails.extractIngredients().map {
                        IngredientEntity(mealId = mealEntity.id, name = it.name, measure = it.measure)
                    }
                    val instructions = mealDetails.extractInstructions().map {
                        InstructionEntity(mealId = mealEntity.id, step = it.step, description = it.step)
                    }

                    MealWithDetails(meal = mealEntity, ingredients = ingredients, instructions = instructions)
                }?.also { mealsWithDetails ->
                    // Store meals in Room DB
                    if (mealsWithDetails.isNotEmpty()) {
                        mealDao.insertMealsWithDetails(mealsWithDetails)
                    }
                }?.map { mealWithDetails ->
                    // Convert MealWithDetails to RecipeItem
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
            Log.e("FilteredRecipesFragment", "Error fetching recipes: ${e.message}")
            emptyList()
        }
    }



    private fun showLoading() {
        binding.rvRecipes.isVisible = false
        binding.emptyState.isVisible = false
    }

    private fun hideLoading() {
        binding.rvRecipes.isVisible = true
    }

    private fun showRecipes(recipes: List<RecipeItem>) {
        if (recipes.isEmpty()) showEmptyState()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
