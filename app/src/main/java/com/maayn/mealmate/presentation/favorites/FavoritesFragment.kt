package com.maayn.mealmate.presentation.favorites

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.Chip
import com.maayn.mealmate.R
import com.maayn.mealmate.core.utils.NetworkMonitor
import com.maayn.mealmate.data.local.database.AppDatabase
import com.maayn.mealmate.data.local.entities.toDomain
import com.maayn.mealmate.data.model.Category
import com.maayn.mealmate.databinding.FragmentFavoritesBinding
import com.maayn.mealmate.databinding.FragmentRecipesBinding
import com.maayn.mealmate.presentation.home.RecipesFragmentDirections
import com.maayn.mealmate.presentation.home.adapters.RecipesAdapter
import com.maayn.mealmate.presentation.home.model.RecipeItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import kotlin.collections.forEach
import kotlin.time.Duration.Companion.milliseconds

class FavoritesFragment : Fragment() {
    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!


    // State management
    private val currentCategory = MutableStateFlow("All")
    private val searchQuery = MutableStateFlow("")
    private var allRecipes = emptyList<RecipeItem>()
    private val semaphore = Semaphore(5) // Limit concurrent network requests

    // Adapter using ListAdapter with DiffUtil
    var recipesAdapter: RecipesAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("FavoritesFragment", "onCreateView called")
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("FavoritesFragment", "onViewCreated called")
        recipesAdapter = RecipesAdapter(
            requireContext(),
            viewLifecycleOwner.lifecycleScope,
            emptyList(), // or provide the actual list of recipes
            onRecipeClick = { recipe ->
                val action = FavoritesFragmentDirections.actionFavoritesFragmentToRecipeDetailsFragment(recipe.id)
                findNavController().navigate(action)
            },
            onCreateMealPlanButtonClick = { mealPlan ->
                val action = FavoritesFragmentDirections.actionFavoritesFragmentToCreateMealPlanFragment(mealPlan)
                findNavController().navigate(action)
            }
        )

        setupUI()
        setupObservers()
        setupListeners()
        fetchRecipes()
    }



    private fun setupUI() {
        Log.d("FavoritesFragment", "Setting up UI")
        binding.rvRecipes.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recipesAdapter
            setHasFixedSize(true)
        }



    }

    private fun setupObservers() {
        Log.d("FavoritesFragment", "Setting up observers")
        lifecycleScope.launch {
            combine(
                currentCategory,
                searchQuery.debounce(300.milliseconds)
            ) { category, query -> Pair(category, query) }
                .flowOn(Dispatchers.IO)
                .collectLatest { (category, query) ->
                    Log.d("FavoritesFragment", "Filtering recipes for category: $category, query: $query")
                    val filtered = allRecipes.filter {
                        (it.category == category || category == "All") &&
                                it.name.contains(query, true)
                    }
                    Log.d("FavoritesFragment", "Filtered recipes count: ${filtered.size}")
                    withContext(Dispatchers.Main) { showRecipes(filtered) }
                }
        }
    }

    private fun setupListeners() {
        Log.d("FavoritesFragment", "Setting up listeners")
        binding.apply {
            btnBack.setOnClickListener {
                Log.d("FavoritesFragment", "Back button clicked")
                activity?.onBackPressed()
            }
            btnFilter.setOnClickListener {
                Log.d("FavoritesFragment", "Filter button clicked")
                showFilterComingSoon()
            }
            etSearch.addTextChangedListener { text ->
                Log.d("FavoritesFragment", "Search query changed: $text")
                searchQuery.value = text?.toString()?.trim() ?: ""
            }
        }
    }

    private fun fetchRecipes() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { showLoading() }

                val db = AppDatabase.getInstance(requireContext())
                val favoriteDao = db.favoriteMealDao()

                // Fetch favorite meals with details
                val mealsWithDetails = favoriteDao.getAllFavoriteMealDetails()
                Log.d("FavoritesFragment", "Fetched ${mealsWithDetails.size} meals from database")

                if (mealsWithDetails.isEmpty()) {
                    Log.w("FavoritesFragment", "Database returned 0 meals!")
                }

                withContext(Dispatchers.Main) {
                    allRecipes = mealsWithDetails.map { mealWithDetails ->
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

                    Log.d("FavoritesFragment", "Mapped ${allRecipes.size} recipes")
                    updateRecipes()
                }
            } catch (e: Exception) {
                Log.e("FavoritesFragment", "Error fetching favorites: ${e.message}", e)
                withContext(Dispatchers.Main) { showError("Failed to load favorites: ${e.message}") }
            } finally {
                withContext(Dispatchers.Main) { hideLoading() }
            }
        }
    }

    private fun updateRecipes() {
        lifecycleScope.launch(Dispatchers.IO) {
            Log.d("FavoritesFragment", "Updating recipes list...")
            val currentQuery = searchQuery.value
            val currentCategory = currentCategory.value

            val filtered = allRecipes.filter {
                (it.category == currentCategory || currentCategory == "All") &&
                        it.name.contains(currentQuery, true)
            }
            Log.d("FavoritesFragment", "Filtered recipes count after update: ${filtered.size}")

            withContext(Dispatchers.Main) { showRecipes(filtered) }
        }
    }

    private fun showLoading() {
        Log.d("FavoritesFragment", "Showing loading state")
        binding.rvRecipes.isVisible = false
        binding.emptyState.isVisible = false
    }

    private fun hideLoading() {
        Log.d("FavoritesFragment", "Hiding loading state")
    }

    private fun showRecipes(recipes: List<RecipeItem>) {
        Log.d("FavoritesFragment", "Displaying ${recipes.size} recipes")
        binding.rvRecipes.isVisible = true
        binding.emptyState.isVisible = false
        recipesAdapter?.updateData(recipes)
    }

    private fun showEmptyState() {
        Log.d("FavoritesFragment", "Showing empty state")
        binding.rvRecipes.isVisible = false
        binding.emptyState.isVisible = true
    }

    private fun showError(message: String) {
        Log.e("FavoritesFragment", "Error: $message")
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        showEmptyState()
    }

    private fun showFilterComingSoon() {
        Log.d("FavoritesFragment", "Filter feature is not implemented yet")
        Toast.makeText(context, "Filter feature coming soon!", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("FavoritesFragment", "onDestroyView called, unregistering network monitor")

    }
}
