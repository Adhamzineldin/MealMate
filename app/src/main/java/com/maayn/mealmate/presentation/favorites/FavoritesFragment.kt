package com.maayn.mealmate.presentation.favorites

import android.os.Bundle
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
import com.maayn.mealmate.data.local.database.AppDatabase
import com.maayn.mealmate.data.local.entities.toDomain
import com.maayn.mealmate.data.model.Category
import com.maayn.mealmate.databinding.FragmentRecipesBinding
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

class FavoritesFragment : Fragment(){
    private var _binding: FragmentRecipesBinding? = null
    private val binding get() = _binding!!
    private lateinit var networkMonitor: NetworkMonitor

    // State management
    private val currentCategory = MutableStateFlow("All")
    private val searchQuery = MutableStateFlow("")
    private var allRecipes = emptyList<RecipeItem>()
    private val semaphore = Semaphore(5) // Limit concurrent network requests

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
        this.recipesAdapter = RecipesAdapter(requireContext(), viewLifecycleOwner.lifecycleScope)
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

                val db = AppDatabase.getInstance(requireContext())
                val favoriteDao = db.favoriteMealDao()

                // Fetch favorite meals with details
                val mealsWithDetails = favoriteDao.getAllFavoriteMealDetails()

                withContext(Dispatchers.Main) {
                    allRecipes = mealsWithDetails.map { mealWithDetails ->
                        RecipeItem(
                            id = mealWithDetails.meal.id,
                            name = mealWithDetails.meal.name,
                            imageUrl = mealWithDetails.meal.imageUrl,
                            area = mealWithDetails.meal.country ?: "Unknown",
                            isFavorited = mealWithDetails.meal.isFavorite,
                            time = mealWithDetails.meal.time ?: "Unknown",
                            rating = mealWithDetails.meal.rating ?: 0f,
                            ratingCount = mealWithDetails.meal.ratingCount ?: 0,
                            category = mealWithDetails.meal.category ?: "Uncategorized",
                            ingredients = mealWithDetails.ingredients.map { it.toDomain() },
                            instructions = mealWithDetails.instructions.map { it.toDomain() }
                        )
                    }
                    updateRecipes()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { showError("Failed to load favorites: ${e.message}") }
            } finally {
                withContext(Dispatchers.Main) { hideLoading() }
            }
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