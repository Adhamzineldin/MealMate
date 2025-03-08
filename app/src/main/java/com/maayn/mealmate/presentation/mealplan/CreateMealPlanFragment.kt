package com.maayn.mealmate.presentation.mealplan

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.maayn.mealmate.R
import com.maayn.mealmate.data.local.database.AppDatabase
import com.maayn.mealmate.data.local.entities.Meal
import com.maayn.mealmate.data.local.entities.MealPlan
import com.maayn.mealmate.databinding.FragmentCreateMealPlanBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CreateMealPlanFragment : Fragment() {

    private var _binding: FragmentCreateMealPlanBinding? = null
    private val binding get() = _binding!!

    private var selectedMealType: String? = null
    private var selectedRecipe: Meal? = null
    private val calendar = Calendar.getInstance()

    private var mealPlanId: Int? = null // Store meal plan ID if editing

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateMealPlanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get arguments if editing an existing meal plan
        val args: CreateMealPlanFragmentArgs by navArgs()
        val mealPlan = args.mealPlan

        if (mealPlan != null) {
            populateMealPlan(mealPlan)
        }

        setupListeners()
    }

    private fun populateMealPlan(mealPlan: MealPlan) {
        mealPlanId = mealPlan.id // Store ID for updates
        binding.etMealPlanName.setText(mealPlan.name)
        binding.etDate.setText(mealPlan.date)

        selectedMealType = mealPlan.mealType
        selectedRecipe = Meal(
            id = mealPlan.recipeId,
            name = mealPlan.recipeName,
            imageUrl = mealPlan.recipeImage
        )

        // Set selected meal type chip
        for (i in 0 until binding.chipGroupMealType.childCount) {
            val chip = binding.chipGroupMealType.getChildAt(i) as Chip
            if (chip.text.toString() == mealPlan.mealType) {
                chip.isChecked = true
                break
            }
        }

        binding.btnChooseRecipe.text = mealPlan.recipeName
        if (mealPlanId != null){
            binding.btnSaveMealPlan.text = "Update Meal Plan"
        }

    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.etDate.setOnClickListener {
            showDatePicker()
        }

        binding.chipGroupMealType.setOnCheckedChangeListener { group, checkedId ->
            val selectedChip = group.findViewById<Chip>(checkedId)
            selectedMealType = selectedChip?.text.toString()
        }

        binding.btnChooseRecipe.setOnClickListener {
            showRecipeSelectionDialog()
        }

        binding.btnSaveMealPlan.setOnClickListener {
            saveOrUpdateMealPlan()
        }
    }

    private fun showDatePicker() {
        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                showTimePicker()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun showTimePicker() {
        val timePicker = TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)

                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                binding.etDate.setText(dateFormat.format(calendar.time))
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        )
        timePicker.show()
    }

    private fun showRecipeSelectionDialog() {
        val db = AppDatabase.getInstance(requireContext())
        lifecycleScope.launch(Dispatchers.IO) {
            val recipes = db.mealDao().getAllMeals() // Fetch all recipes from DB

            if (recipes.isNotEmpty()) {
                val recipeNames = recipes.map { it.name }.toTypedArray()

                requireActivity().runOnUiThread {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Select Recipe")
                        .setSingleChoiceItems(recipeNames, -1) { dialog, which ->
                            selectedRecipe = recipes[which]
                        }
                        .setPositiveButton("Confirm") { dialog, _ ->
                            binding.btnChooseRecipe.text = selectedRecipe?.name ?: "Choose Recipe"
                            dialog.dismiss()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            } else {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "No recipes available", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveOrUpdateMealPlan() {
        val mealPlanName = binding.etMealPlanName.text.toString().trim()
        val formattedDate = binding.etDate.text.toString().trim()

        if (mealPlanName.isEmpty() || formattedDate.isEmpty() || selectedMealType == null || selectedRecipe == null) {
            Toast.makeText(requireContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val mealPlan = if (mealPlanId == null) {
            // Creating a new meal plan (no id provided)
            MealPlan(
                name = mealPlanName,
                date = formattedDate,
                mealType = selectedMealType!!,
                recipeName = selectedRecipe!!.name,
                recipeImage = selectedRecipe!!.imageUrl,
                recipeId = selectedRecipe!!.id
            )
        } else {
            // Updating an existing meal plan (retain id)
            MealPlan(
                id = mealPlanId!!,
                name = mealPlanName,
                date = formattedDate,
                mealType = selectedMealType!!,
                recipeName = selectedRecipe!!.name,
                recipeImage = selectedRecipe!!.imageUrl,
                recipeId = selectedRecipe!!.id
            )
        }

        val db = AppDatabase.getInstance(requireContext())
        lifecycleScope.launch(Dispatchers.IO) {
            if (mealPlanId == null) {
                db.mealPlanDao().insertMealPlan(mealPlan)
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Meal Plan Created!", Toast.LENGTH_SHORT).show()
                }
            } else {
                db.mealPlanDao().updateMealPlan(mealPlan)
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Meal Plan Updated!", Toast.LENGTH_SHORT).show()
                }
            }

            requireActivity().runOnUiThread {
                findNavController().navigate(R.id.action_createMealPlanFragment_to_mealPlanFragment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

    }
}
