package com.maayn.mealmate.presentation.mealplan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.maayn.mealmate.R
import com.maayn.mealmate.data.local.entities.MealPlan
import java.text.SimpleDateFormat
import java.util.*

class MealPlanFragment : Fragment() {

    private lateinit var viewModel: MealPlanViewModel
    private lateinit var rvMealPlans: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var btnAddMealPlan: MaterialButton
    private lateinit var mealPlanAdapter: MealPlanAdapter
    private lateinit var btnBack: AppCompatImageButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_meal_plan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[MealPlanViewModel::class.java]

        rvMealPlans = view.findViewById(R.id.rvMealPlans)
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState)
        btnAddMealPlan = view.findViewById(R.id.btnAddMealPlan)
        btnBack = view.findViewById(R.id.btnBack)

        // Set up RecyclerView
        mealPlanAdapter = MealPlanAdapter(
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

        // Observe meal plans
        viewModel.allMealPlans.observe(viewLifecycleOwner) { mealPlans ->
            if (mealPlans.isNotEmpty()) {
                layoutEmptyState.visibility = View.GONE
                rvMealPlans.visibility = View.VISIBLE

                // ✅ Sort meal plans by nearest date
                val sortedMealPlans = mealPlans.sortedBy { parseDate(it.date) }

                mealPlanAdapter.submitList(sortedMealPlans)
            } else {
                layoutEmptyState.visibility = View.VISIBLE
                rvMealPlans.visibility = View.GONE
            }
        }

        // Handle Add Meal Plan button click
        btnAddMealPlan.setOnClickListener {
            val action = MealPlanFragmentDirections.actionMealPlanFragmentToCreateMealPlanFragment(null)
            findNavController().navigate(action)
        }

        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

    }

    // ✅ Helper function to parse date and return Date object
    private fun parseDate(dateStr: String?): Date? {
        return try {
            dateStr?.let {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                sdf.parse(it)
            }
        } catch (e: Exception) {
            null // Return null for invalid dates
        }
    }
}
