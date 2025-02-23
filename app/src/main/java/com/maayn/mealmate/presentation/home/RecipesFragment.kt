package com.maayn.mealmate.presentation.home

import android.os.Bundle
import android.os.StrictMode
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.maayn.mealmate.core.utils.NetworkMonitor
import com.maayn.mealmate.data.model.Category
import com.maayn.mealmate.data.remote.api.RetrofitClient
import com.maayn.mealmate.databinding.FragmentRecipesBinding
import com.maayn.mealmate.databinding.ItemRecipeBinding
import com.maayn.mealmate.presentation.home.adapters.RecipesAdapter
import com.maayn.mealmate.presentation.home.model.RecipeItem
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

    // Adapter using ListAdapter with DiffUtil
    val recipesAdapter = RecipesAdapter()


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

                val categories = RetrofitClient.apiService.getMealCategories().categories

                val recipes = fetchAllRecipes(categories)

                withContext(Dispatchers.Main) {
                    allRecipes = recipes
                    setupCategoryChips(categories)
                    updateRecipes()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { showError("Failed to load recipes: ${e.message}") }
            } finally {
                withContext(Dispatchers.Main) { hideLoading() }
            }
        }
    }



    private suspend fun fetchAllRecipes(categories: List<Category>): List<RecipeItem> {
        return coroutineScope {
            categories.map { category ->
                async(Dispatchers.IO) { fetchRecipesForCategory(category) }
            }.awaitAll().flatten().shuffled()
        }
    }


    private suspend fun fetchRecipesForCategory(category: Category): List<RecipeItem> {
        return try {
            semaphore.withPermit {
                val meals = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.getMealsForCategory(category.strCategory).meals
                }
                meals.map { meal ->
                    RecipeItem(
                        id = meal.idMeal,
                        name = meal.strMeal,
                        time = "${Random.nextInt(10, 61)} minutes",
                        rating = listOf(1f, 1.5f, 2f, 2.5f, 3f, 3.5f, 4f, 4.5f, 5f).random(),
                        imageUrl = meal.strMealThumb,
                        category = category.strCategory
                    )
                }
            }
        } catch (e: Exception) {
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

    private fun showRecipeDetails(recipe: RecipeItem) {
        // Implement navigation to recipe details
    }

    override fun onDestroyView() {
        super.onDestroyView()
        networkMonitor.unregister()

    }

}
