package com.maayn.mealmate.presentation.details

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.maayn.mealmate.R
import com.maayn.mealmate.data.local.database.AppDatabase
import com.maayn.mealmate.databinding.FragmentRecipeDetailsBinding
import com.maayn.mealmate.data.local.entities.MealWithDetails
import com.maayn.mealmate.presentation.details.adapters.IngredientsAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RecipeDetailsFragment : Fragment() {
    private var _binding: FragmentRecipeDetailsBinding? = null
    private val binding get() = _binding!!
    private var currentMeal: MealWithDetails? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecipeDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args: RecipeDetailsFragmentArgs by navArgs()
        val mealId = args.mealId

        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(requireContext())
            val mealDao = db.mealDao()
            val mealWithDetails = mealDao.getMealWithDetails(mealId)
            Log.e("DB", "Meal with details: $mealWithDetails")
            if (mealWithDetails != null) {
                currentMeal = mealWithDetails
                launch(Dispatchers.Main) { setupUI(mealWithDetails) }
            }
        }

        setupListeners()
    }

    private fun setupUI(mealWithDetails: MealWithDetails) {
        val meal = mealWithDetails.meal
        binding.ivRecipeImage.load(meal.imageUrl) {
            crossfade(true)
            placeholder(R.drawable.meal_mate_icon)
        }
        binding.tvRecipeTitle.text = meal.name
        binding.tvCategoryArea.text = "${meal.category} • ${meal.country}"
        binding.tvTime.text = meal.time

        // Setup RecyclerView for ingredients
        binding.rvIngredients.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = IngredientsAdapter(mealWithDetails.ingredients)
        }

        if (!meal.videoUrl.isNullOrEmpty()) {
            setupVideoPlayer(meal.videoUrl)
        } else {
            binding.webViewVideo.visibility = View.GONE
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupVideoPlayer(videoUrl: String) {
        binding.webViewVideo.apply {
            settings.javaScriptEnabled = true
            settings.pluginState = WebSettings.PluginState.ON
            settings.mediaPlaybackRequiresUserGesture = false
            webChromeClient = WebChromeClient()
            webViewClient = WebViewClient()
            visibility = View.VISIBLE

            val embedUrl = convertYouTubeUrlToEmbed(videoUrl)
            loadUrl(embedUrl)
        }
    }

    private fun convertYouTubeUrlToEmbed(url: String): String {
        val videoId = Uri.parse(url).getQueryParameter("v")
        return if (videoId != null) {
            "https://www.youtube.com/embed/$videoId"
        } else {
            url
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnFavorite.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                currentMeal?.let { meal ->
                    try {
                        val db = AppDatabase.getInstance(requireContext())
                        val favoriteDao = db.favoriteMealDao()
                        val existingItem = favoriteDao.getFavoriteMealDetailsById(meal.meal.id)
                        if (existingItem != null) {
                            favoriteDao.deleteFavoriteMeal(meal.meal.id)
                            Log.e("DB", "Removed favorite: ${meal.meal.name}")
                        } else {
                            favoriteDao.insertMealWithDetails(meal)
                            Log.e("DB", "Added to favorites: ${meal.meal.name}")
                        }
                    } catch (e: Exception) {
                        Log.e("DB_ERROR", "Failed to update favorite: ${e.message}")
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

    }
}
