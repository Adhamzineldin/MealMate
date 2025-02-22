package com.maayn.mealmate.presentation.recipes

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.maayn.mealmate.core.utils.NetworkMonitor
import com.maayn.mealmate.data.model.Category
import com.maayn.mealmate.data.remote.api.RetrofitClient
import com.maayn.mealmate.databinding.FragmentRecipesBinding
import com.maayn.mealmate.presentation.home.adapters.RecipesAdapter
import com.maayn.mealmate.presentation.home.model.RecipeItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.random.Random

class RecipesFragment : Fragment() {
    private var _binding: FragmentRecipesBinding? = null
    private val binding get() = _binding!!
    private lateinit var networkMonitor: NetworkMonitor
    private val recipesAdapter by lazy { RecipesAdapter(emptyList()) }

    // State management
    private var searchJob: Job? = null
    private val currentCategory = MutableStateFlow("All")
    private val searchQuery = MutableStateFlow("")
    private var allRecipes = emptyList<RecipeItem>()

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
        setupObservers()
        setupListeners()
    }

    private fun setupNetworkMonitor() {
        networkMonitor = NetworkMonitor(requireContext()) { isConnected ->
            binding.emptyState.isVisible = !isConnected
            if (isConnected && allRecipes.isEmpty()) fetchRecipes()
        }
        networkMonitor.register()
    }

    private fun setupUI() {
        binding.rvRecipes.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recipesAdapter
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            currentCategory.collectLatest { updateRecipes() }
        }

        lifecycleScope.launch {
            searchQuery.collectLatest { query ->
                delay(300) // Debounce
                updateRecipes(query)
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
        lifecycleScope.launch {
            try {
                showLoading()
                val categories = RetrofitClient.apiService.getMealCategories().categories
                setupCategoryChips(categories)
                allRecipes = fetchAllRecipes(categories)
                updateRecipes()
            } catch (e: Exception) {
                showError("Failed to load recipes: ${e.message}")
            } finally {
                hideLoading()
            }
        }
    }

    private suspend fun fetchAllRecipes(categories: List<Category>): List<RecipeItem> {
        return try {
            coroutineScope {
                categories.map { category ->
                    async {
                        fetchRecipesForCategory(category)
                    }
                }.awaitAll().flatten().shuffled()
            }
        } catch (e: Exception) {
            Log.e("fetchAllRecipes", "Error fetching recipes", e)
            emptyList()
        }
    }

    private suspend fun fetchRecipesForCategory(category: Category): List<RecipeItem> {
        return try {
            RetrofitClient.apiService.getMealsForCategory(category.strCategory).meals.map { meal ->
                RecipeItem(
                    id = meal.idMeal,
                    name = meal.strMeal,
                    time = "${Random.nextInt(10, 61)} minutes",
                    rating = listOf(1f, 1.5f, 2f, 2.5f, 3f, 3.5f, 4f, 4.5f, 5f).random(),
                    imageUrl = meal.strMealThumb,
                    category = category.strCategory
                )
            }
        } catch (e: Exception) {
            Log.e("fetchRecipesForCategory", "Error fetching category ${category.strCategory}", e)
            emptyList()
        }
    }


    private fun setupCategoryChips(categories: List<Category>) {
        binding.chipGroup.removeAllViews()
        addChip("All", isSelected = true)
        categories.forEach { addChip(it.strCategory, isSelected = false) }
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

    private fun updateRecipes(query: String = searchQuery.value) {
        val filtered = allRecipes
            .filter { it.category == currentCategory.value || currentCategory.value == "All" }
            .filter { it.name.contains(query, true) }

        if (filtered.isEmpty()) showEmptyState() else showRecipes(filtered)
    }

    private fun showLoading() {
//        binding.progressBar.isVisible = true
        binding.rvRecipes.isVisible = false
    }

    private fun hideLoading() {
//        binding.progressBar.isVisible = false
    }

    private fun showRecipes(recipes: List<RecipeItem>) {
        binding.rvRecipes.isVisible = true
        binding.emptyState.isVisible = false
        recipesAdapter.updateData(recipes)
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

    override fun onDestroyView() {
        super.onDestroyView()
        networkMonitor.unregister()
        _binding = null
    }
}