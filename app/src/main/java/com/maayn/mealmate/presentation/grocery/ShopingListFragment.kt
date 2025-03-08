package com.maayn.mealmate.presentation.grocery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.maayn.mealmate.R
import com.maayn.mealmate.databinding.FragmentShoppingListBinding
import java.util.UUID

class ShoppingListFragment : Fragment(), ShoppingListAdapter.OnItemClickListener {

    private var _binding: FragmentShoppingListBinding? = null
    private val binding get() = _binding!!

    private val shoppingItems = mutableListOf<ShoppingItem>()
    private lateinit var adapter: ShoppingListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShoppingListBinding.inflate(inflater, container, false)
        setupRecyclerView()
        setupClickListeners()
        updateEmptyState()




        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = ShoppingListAdapter(shoppingItems, this)
        binding.rvShoppingList.layoutManager = LinearLayoutManager(context)
        binding.rvShoppingList.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnAddIngredient.setOnClickListener {
            val ingredientName = binding.etIngredientName.text.toString().trim()
            if (ingredientName.isNotEmpty()) {
                addItem(ingredientName)
                binding.etIngredientName.text.clear()
            } else {
                Toast.makeText(context, "Please enter an ingredient name", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnClearAll.setOnClickListener { clearAllItems() }
    }

    private fun addItem(ingredientName: String) {
        val newItem = ShoppingItem(UUID.randomUUID().toString(), ingredientName)
        shoppingItems.add(newItem)
        adapter.notifyItemInserted(shoppingItems.lastIndex)
        updateEmptyState()
    }

    private fun removeItem(position: Int) {
        if (position in shoppingItems.indices) {
            shoppingItems.removeAt(position)
            adapter.notifyItemRemoved(position)
            updateEmptyState()
        }
    }

    private fun clearAllItems() {
        shoppingItems.clear()
        adapter.updateList(emptyList())  // ✅ Ensure UI updates correctly
        updateEmptyState()
    }

    private fun updateEmptyState() {
        with(binding) {
            val isEmpty = shoppingItems.isEmpty()
            rvShoppingList.visibility = if (isEmpty) View.GONE else View.VISIBLE
            layoutEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE

            // Show or hide the "Clear All" button based on whether the list is empty
            btnClearAll.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }
    }


    override fun onDeleteClick(position: Int) {
        removeItem(position)
    }

    override fun onCheckboxClick(position: Int, isChecked: Boolean) {
        if (position in shoppingItems.indices) {
            shoppingItems[position].isChecked = isChecked
            binding.rvShoppingList.post {
                adapter.notifyItemChanged(position)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
