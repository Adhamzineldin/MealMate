package com.maayn.mealmate.presentation.home


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.maayn.mealmate.R
import com.maayn.mealmate.core.utils.NetworkMonitor
import com.maayn.mealmate.data.local.database.AppDatabase
import com.maayn.mealmate.data.model.extractIngredients
import com.maayn.mealmate.data.model.extractInstructions
import com.maayn.mealmate.databinding.FragmentHomeBinding
import com.maayn.mealmate.presentation.home.adapters.RecipesAdapter
import com.maayn.mealmate.presentation.home.model.CategoryItem
import com.maayn.mealmate.presentation.home.model.IngredientItem
import com.maayn.mealmate.presentation.home.model.RecipeItem
import com.maayn.mealmate.data.remote.api.RetrofitClient
import com.maayn.mealmate.presentation.favorites.FavoritesFragmentDirections
import com.maayn.mealmate.presentation.home.adapters.CategoriesAdapter
import com.maayn.mealmate.presentation.home.adapters.IngredientsAdapter
import com.maayn.mealmate.presentation.home.model.toMealWithDetails
import com.maayn.mealmate.presentation.mealplan.MealPlanAdapter
import com.maayn.mealmate.presentation.mealplan.MealPlanFragmentDirections
import com.maayn.mealmate.presentation.mealplan.MealPlanViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import kotlin.random.Random
import java.util.Date
import java.util.Locale

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

        setupNetworkMonitor(view)
        setupUI(view)


    }

    private fun setupNetworkMonitor(view: View) {
        networkMonitor = NetworkMonitor(requireContext()) { isConnected ->
            requireActivity().runOnUiThread {
                if (isConnected) {
                    showMainContent()
                    setupUI(view)
                } else {
                    showNoInternetView()
                }
            }
        }
        networkMonitor.register()
    }

    private fun setupUI(view: View) {
        fetchMealOfTheDay()
        setupGreeting()
        fetchCategories()
        fetchCountries()
        fetchTrendingRecipes()
        fetchPopularIngredients()
        fetchUpcomingPlans(view)
        initializeSideMenu()

    }

    private fun initializeSideMenu() {
        val btnMenu: ImageButton = requireView().findViewById(R.id.btnMenu)
        val drawerLayout: DrawerLayout = requireActivity().findViewById(R.id.drawer_layout)
        val navView: NavigationView = requireActivity().findViewById(R.id.nav_view)
        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        val firebaseAuth = FirebaseAuth.getInstance()
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_shopping_list -> {
                    // Handle Shopping List Click
                }
                R.id.nav_profile -> {
                    if (firebaseAuth.currentUser != null) {
                        findNavController().navigateSafely(R.id.profileFragment)
                    } else {
                        findNavController().navigateSafely(R.id.loginFragment)
                    }
                    true
                }

            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

    }

    fun NavController.navigateSafely(destinationId: Int) {
        // Prevent navigation to the same destination
        if (currentDestination?.id != destinationId) {
            navigate(destinationId)
        }
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

    private fun fetchUpcomingPlans(view: View) {
        val rvMealPlans = view.findViewById<RecyclerView>(R.id.rvUpcomingPlans)
        val layoutEmptyState = view.findViewById<View>(R.id.layoutEmptyState)

        val db = AppDatabase.getInstance(requireContext()) // Access the database
        val mealPlanDao = db.mealPlanDao()

        // Set up RecyclerView Adapter
        val mealPlanAdapter = MealPlanAdapter(
            onStartCookingClick = { mealPlan ->
                val action = MealPlanFragmentDirections
                    .actionMealPlanFragmentToRecipeDetailsFragment(mealPlan.recipeId)
                findNavController().navigate(action)
            },
            onEditClick = { mealPlan ->
                val action = MealPlanFragmentDirections
                    .actionMealPlanFragmentToCreateMealPlanFragment(mealPlan)
                findNavController().navigate(action)
            }
        )

        rvMealPlans.layoutManager = LinearLayoutManager(requireContext())
        rvMealPlans.adapter = mealPlanAdapter

        // Get today's date in "DD/MM/YYYY" format
        val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        // Observe LiveData and filter for the nearest meal plan
        mealPlanDao.getUpcomingMealPlans(today).observe(viewLifecycleOwner) { mealPlans ->
            if (mealPlans.isNotEmpty()) {
                layoutEmptyState.visibility = View.GONE
                rvMealPlans.visibility = View.VISIBLE

                // Only show the nearest meal plan
                mealPlanAdapter.submitList(listOf(mealPlans.first()))
            } else {
                layoutEmptyState.visibility = View.VISIBLE
                rvMealPlans.visibility = View.GONE
            }
        }
    }




    private fun fetchMealOfTheDay() {
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        // Observe LiveData
        viewModel.mealOfTheDay.observe(viewLifecycleOwner) { meal ->
            meal?.let {
                binding.rvMealOfTheDay.layoutManager = LinearLayoutManager(requireContext())
                binding.rvMealOfTheDay.adapter = RecipesAdapter(
                    requireContext(),
                    viewLifecycleOwner.lifecycleScope,
                    listOf(it),
                    onRecipeClick = { recipe ->
                        val action = HomeFragmentDirections.actionHomeFragmentToRecipeDetailsFragment(recipe.id)
                        findNavController().navigate(action)
                    },
                    onCreateMealPlanButtonClick = { mealPlan ->
                        val action = HomeFragmentDirections.actionHomeFragmentToCreateMealPlanFragment(mealPlan)
                        findNavController().navigate(action)
                    }
                )

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
                        adapter = CategoriesAdapter(categoryItems,
                            onCategoryClick = { category ->
                                val action = HomeFragmentDirections.actionHomeFragmentToFilteredRecipesFragment("category", category.name)
                                findNavController().navigate(action)
                            }
                            )
                    }
                }
            } catch (e: Exception) {
               handleFailure("Failure: ${e.localizedMessage}")
            }
        }
    }




    private fun fetchCountries() {
        lifecycleScope.launch {
            try {
                val countryCodeMap = mapOf(
                    "American" to "US",
                    "British" to "GB",
                    "Canadian" to "CA",
                    "Chinese" to "CN",
                    "Croatian" to "HR",
                    "Dutch" to "NL",
                    "Egyptian" to "EG",
                    "Filipino" to "PH",
                    "French" to "FR",
                    "Greek" to "GR",
                    "Indian" to "IN",
                    "Irish" to "IE",
                    "Italian" to "IT",
                    "Jamaican" to "JM",
                    "Japanese" to "JP",
                    "Kenyan" to "KE",
                    "Malaysian" to "MY",
                    "Mexican" to "MX",
                    "Moroccan" to "MA",
                    "Polish" to "PL",
                    "Portuguese" to "PT",
                    "Russian" to "RU",
                    "Spanish" to "ES",
                    "Thai" to "TH",
                    "Tunisian" to "TN",
                    "Turkish" to "TR",
                    "Ukrainian" to "UA",
                    "Uruguayan" to "UY",
                    "Vietnamese" to "VN"
                )


                val response = RetrofitClient.apiService.getMealCountries()


                response.meals.let { country ->

                    val countryItems = country.map {
                        val countryCode = countryCodeMap[it.strArea] ?: "unknown"
                        CategoryItem(it.strArea, "https://www.themealdb.com/images/icons/flags/big/64/${countryCode}.png")
                    }
                    binding.rvCountries.apply {
                        layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                        adapter = CategoriesAdapter(countryItems,
                            onCategoryClick = { category ->
                                val action = HomeFragmentDirections.actionHomeFragmentToFilteredRecipesFragment("area", category.name)
                                findNavController().navigate(action)
                            }
                        )

                    }
                }
                Log.e("HomeFragment", "${response.meals}")
            } catch (e: Exception) {
                handleFailure("Failure: ${e.localizedMessage}")
                Log.e("HomeFragment", "${e.localizedMessage}")
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
                // First API Call: Get meals for the category
                val response = RetrofitClient.apiService.getMealsForCategory(category)
                val db = AppDatabase.getInstance(requireContext())
                val mealDao = db.mealDao()

                val recipeItems = mutableListOf<RecipeItem>()

                response.meals?.take(10)?.forEach { meal ->
                    // Second API Call: Get full meal details using meal ID
                    val mealDetailsResponse = RetrofitClient.apiService.getMealDetails(meal.id)

                    // If the response is valid, extract full details
                    mealDetailsResponse.meals?.firstOrNull()?.let { detailedMeal ->
                        val recipeItem = RecipeItem(
                            id = detailedMeal.id,
                            name = detailedMeal.name,
                            category = category,
                            area = detailedMeal.area ?: "Unknown",
                            instructions = detailedMeal.extractInstructions(),
                            imageUrl = detailedMeal.imageUrl,
                            youtubeUrl = detailedMeal.youtubeUrl ?: "",
                            ingredients = detailedMeal.extractIngredients(),
                            isFavorited = false
                        )

                        mealDao.insertMealWithDetails(recipeItem.toMealWithDetails())
                        Log.e("DATABASE_TAG", "Recipe Item: ${recipeItem.toMealWithDetails()}")

                        recipeItems.add(recipeItem)
                    }
                }

                // Update RecyclerView
                binding.rvTrendingRecipes.apply {
                    layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                    adapter = RecipesAdapter(
                        requireContext(),
                        viewLifecycleOwner.lifecycleScope,
                        recipeItems,
                        onRecipeClick = { recipe ->
                            val action = HomeFragmentDirections.actionHomeFragmentToRecipeDetailsFragment(recipe.id)
                            findNavController().navigate(action)
                        },
                        onCreateMealPlanButtonClick = { mealPlan ->
                            val action = HomeFragmentDirections.actionHomeFragmentToCreateMealPlanFragment(mealPlan)
                            findNavController().navigate(action)
                        }
                    )

                }

            } catch (e: Exception) {
                Log.e("HomeFragment", "Error fetching recipes: ${e.localizedMessage}", e)
                handleFailure("Failed to load recipes: ${e.localizedMessage}")
            }
        }
    }



    private fun handleFailure(message: String) {
        Log.e("Toast", message)
    }


    private fun fetchPopularIngredients() {
        lifecycleScope.launch {
            try {
                // Fetch ingredients from API
                val response = RetrofitClient.apiService.getPopularIngredients()

                val ingredientItems = response.meals.map { ingredient ->
                    val imageUrl = "https://www.themealdb.com/images/ingredients/${ingredient.strIngredient}.png"
                    val randomGrams = (50..500).random() // Generates a random number between 50 and 500 grams
                    IngredientItem(ingredient.strIngredient, imageUrl, randomGrams)
                }

                // Update UI on the main thread
                binding.rvPopularIngredients.apply {
                    layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                    adapter = IngredientsAdapter(ingredientItems,
                        onItemClick = { ingredient ->
                            val action = HomeFragmentDirections.actionHomeFragmentToFilteredRecipesFragment("ingredient", ingredient.name)
                            findNavController().navigate(action)
                        }

                        )
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
