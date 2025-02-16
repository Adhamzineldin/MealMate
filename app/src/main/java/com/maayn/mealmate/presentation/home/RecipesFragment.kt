package com.maayn.mealmate.presentation.recipes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.maayn.mealmate.core.utils.NetworkMonitor
import com.maayn.mealmate.data.remote.api.RetrofitClient
import com.maayn.mealmate.databinding.FragmentRecipesBinding
import com.maayn.mealmate.presentation.home.adapters.RecipesAdapter
import com.maayn.mealmate.presentation.home.model.RecipeItem
import kotlinx.coroutines.*
import kotlin.random.Random

class RecipesFragment : Fragment() {
    private var _binding: FragmentRecipesBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("View binding is null")
    private lateinit var networkMonitor: NetworkMonitor
    private val recipesAdapter by lazy { RecipesAdapter(emptyList()) }
    private var searchJob: Job? = null
    private var allRecipes: List<RecipeItem> = emptyList()
    private var currentCategory: String = "All"

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
        setupNetworkMonitor()
        setupUI()
        setupListeners()
    }

    private fun setupNetworkMonitor() {
        networkMonitor = NetworkMonitor(requireContext()) { isConnected ->
            requireActivity().runOnUiThread {
                if (isConnected) {
                    showMainContent()
                    fetchRecipes()
                } else {
                    showEmptyState("No Internet Connection", showError = true)
                }
            }
        }
        networkMonitor.register()
    }

    private fun setupUI() {
        setupRecyclerView()
        setupSearch()
        setupChips()
    }

    private fun setupRecyclerView() {
        binding.rvRecipes.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recipesAdapter
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { editable ->
            searchJob?.cancel()
            searchJob = lifecycleScope.launch {
                delay(300) // Debounce search
                editable?.toString()?.let { searchText ->
                    filterRecipes(searchText)
                }
            }
        }
    }

    private fun setupChips() {
        binding.chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chip = group.findViewById<Chip>(checkedIds.first())
                currentCategory = chip.text.toString()
                filterRecipes(binding.etSearch.text.toString())
            }
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.btnFilter.setOnClickListener {
            // Implement filter dialog here
            Toast.makeText(context, "Filter feature coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchRecipes() {
        lifecycleScope.launch {
            try {
                val categories = RetrofitClient.apiService.getMealCategories()
                val allRecipeItems = mutableListOf<RecipeItem>()

                val recipeDeferredList = categories.categories.map { category ->
                    async {
                        try {
                            val response = RetrofitClient.apiService.getMealsForCategory(category.strCategory)
                            response.meals.map { meal ->
                                val randomRating = listOf(1, 2, 3, 4, 5, 1.5f, 2.5f, 3.5f, 4.5f, 5.0f).random().toFloat()
                                val randomTime = Random.nextInt(10, 61)
                                RecipeItem(
                                    id = meal.idMeal,
                                    name = meal.strMeal,
                                    time = "$randomTime minutes",
                                    rating = randomRating,
                                    imageUrl = meal.strMealThumb,
                                    category = category.strCategory
                                )
                            }
                        } catch (e: Exception) {
                            // Log error but continue with other categories
                            e.printStackTrace()
                            emptyList<RecipeItem>()
                        }
                    }
                }

                allRecipeItems.addAll(recipeDeferredList.awaitAll().flatten().shuffled())
                updateRecipesList(allRecipeItems)

            } catch (e: Exception) {
                showEmptyState("Failed to load recipes", showError = true)
            }
        }
    }

    private fun filterRecipes(searchQuery: String) {
        var filteredList = allRecipes

        // Apply category filter
        if (currentCategory != "All") {
            filteredList = filteredList.filter { it.category == currentCategory }
        }

        // Apply search filter
        if (searchQuery.isNotEmpty()) {
            filteredList = filteredList.filter {
                it.name.contains(searchQuery, ignoreCase = true)
            }
        }

        updateRecipesList(filteredList)
    }

    private fun updateRecipesList(recipes: List<RecipeItem>) {
        if (recipes.isEmpty()) {
            showEmptyState("No recipes found")
        } else {
            showMainContent()
            recipesAdapter.updateData(recipes)
        }
    }

    private fun showEmptyState(message: String, showError: Boolean = false) {
        binding.rvRecipes.visibility = View.GONE
        binding.emptyState.visibility = View.VISIBLE
//        binding.emptyStateMessage.text = message

        if (showError) {
            Toast.makeText(context, "Error: $message", Toast.LENGTH_LONG).show()
        }
    }

    private fun showMainContent() {
        binding.rvRecipes.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        networkMonitor.unregister()
    }
}
