package com.maayn.mealmate.presentation.mealplan


import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.maayn.mealmate.databinding.FragmentCreateMealPlanBinding
import java.text.SimpleDateFormat
import java.util.*

class CreateMealPlanFragment : Fragment() {

    private var _binding: FragmentCreateMealPlanBinding? = null
    private val binding get() = _binding!!

    private var selectedMealType: String? = null
    private var selectedRecipe: String? = null
    private val calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateMealPlanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
    }

    private fun setupListeners() {
        // Back button click
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Date picker dialog
        binding.etDate.setOnClickListener {
            showDatePicker()
        }

        // Meal type selection
        binding.chipGroupMealType.setOnCheckedChangeListener { group, checkedId ->
            val selectedChip = group.findViewById<Chip>(checkedId)
            selectedMealType = selectedChip?.text.toString()
        }

        // Recipe selection button
        binding.btnChooseRecipe.setOnClickListener {
            // Open recipe selection screen (implement navigation)
            Toast.makeText(requireContext(), "Select Recipe Clicked", Toast.LENGTH_SHORT).show()
        }

        // Save meal plan button
        binding.btnSaveMealPlan.setOnClickListener {
            saveMealPlan()
        }
    }

    private fun showDatePicker() {
        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                binding.etDate.setText(dateFormat.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun saveMealPlan() {
        val mealPlanName = binding.etMealPlanName.text.toString().trim()
        val date = binding.etDate.text.toString()

        if (mealPlanName.isEmpty() || date.isEmpty() || selectedMealType == null) {
            Toast.makeText(requireContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(requireContext(), "Meal Plan Saved!", Toast.LENGTH_SHORT).show()

        // Implement saving logic (e.g., save to database or API)
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}
