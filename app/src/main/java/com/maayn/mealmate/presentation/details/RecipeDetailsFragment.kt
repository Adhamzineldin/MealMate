package com.maayn.mealmate.presentation.details

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import kotlinx.coroutines.launch

class RecipeDetailsFragment : Fragment() {
    private var _binding: FragmentRecipeDetailsBinding? = null
    private val binding get() = _binding!!

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
        val mealId = args.mealId // Get the meal ID

        lifecycleScope.launch {
            val db = AppDatabase.getInstance(requireContext())
            val mealDao = db.mealDao()
            val mealWithDetails = mealDao.getMealWithDetails(mealId)
            mealWithDetails.let { setupUI(it) }
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

        val ingredientsAdapter = IngredientsAdapter(mealWithDetails.ingredients)
        binding.rvIngredients.adapter = ingredientsAdapter
        binding.rvIngredients.layoutManager = LinearLayoutManager(requireContext())

        if (!meal.videoUrl.isNullOrEmpty()) {
            binding.ivVideoThumbnail.load(meal.videoUrl) {
                crossfade(true)
                placeholder(R.drawable.meal_mate_icon)
            }
            binding.cardVideo.setOnClickListener {
                openVideo(meal.videoUrl)
            }
        } else {
            binding.cardVideo.visibility = View.GONE
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnFavorite.setOnClickListener {
            // Handle favorite toggle logic
        }
    }

    private fun openVideo(videoUrl: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
