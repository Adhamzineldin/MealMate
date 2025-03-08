package com.maayn.mealmate.presentation.grocery

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.maayn.mealmate.data.local.dao.MealDao
import com.maayn.mealmate.data.local.dao.ShoppingItemDao
import com.maayn.mealmate.data.local.database.AppDatabase
import com.maayn.mealmate.data.local.entities.ShoppingItem
import com.maayn.mealmate.databinding.FragmentShoppingListBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class ShoppingListFragment : Fragment(), ShoppingListAdapter.OnItemClickListener {

    private var _binding: FragmentShoppingListBinding? = null
    private val binding get() = _binding!!

    private val shoppingItems = mutableListOf<ShoppingItem>()
    private lateinit var adapter: ShoppingListAdapter
    private lateinit var shoppingItemDao: ShoppingItemDao
    private lateinit var mealDao: MealDao

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShoppingListBinding.inflate(inflater, container, false)

        // Initialize DAO
        val database = AppDatabase.getInstance(requireContext())
        shoppingItemDao = database.shoppingItemDao()
        mealDao = database.mealDao()

        setupRecyclerView()
        setupClickListeners()
        updateEmptyState()

        // Load shopping items from Room DB asynchronously
        loadShoppingItems()

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

        // Insert item into Room DB asynchronously
        lifecycleScope.launch {
            insertItemIntoDb(newItem)
        }

        shoppingItems.add(newItem)
        adapter.notifyItemInserted(shoppingItems.lastIndex)
        updateEmptyState()
    }

    // Make sure database operations happen off the main thread
    private suspend fun insertItemIntoDb(item: ShoppingItem) {
        withContext(Dispatchers.IO) {
            shoppingItemDao.insert(item)
        }
    }

    // Load shopping items from Room DB
    private fun loadShoppingItems() {
        lifecycleScope.launch {
            val items = loadItemsFromDb()
            Log.d("ShoppingListFragment", "Loaded items: $items")

            // Clear previous items to avoid duplicating
            shoppingItems.clear()

            // Add the loaded items to the shopping list
            shoppingItems.addAll(items)

            // Notify adapter that the data has changed
            withContext(Dispatchers.Main) {
                adapter.notifyDataSetChanged()
                updateEmptyState()
            }
        }
    }

    // Fetch items from Room DB asynchronously
    private suspend fun loadItemsFromDb(): List<ShoppingItem> {
        return withContext(Dispatchers.IO) {
            shoppingItemDao.getAll()
        }
    }

    private fun removeItem(position: Int) {
        if (position in shoppingItems.indices) {
            val item = shoppingItems[position]
            shoppingItems.removeAt(position)

            // Delete item from Room DB asynchronously
            lifecycleScope.launch {
                deleteItemFromDb(item)
            }

            adapter.notifyItemRemoved(position)
            updateEmptyState()
        }
    }

    // Delete item from Room DB
    private suspend fun deleteItemFromDb(item: ShoppingItem) {
        withContext(Dispatchers.IO) {
            shoppingItemDao.delete(item)
        }
    }

    private fun clearAllItems() {
        shoppingItems.clear()
        adapter.updateList(emptyList())  // Ensure UI updates correctly
        lifecycleScope.launch {
            clearAllItemsFromDb()
        }
        updateEmptyState()
    }

    // Clear all items from Room DB
    private suspend fun clearAllItemsFromDb() {
        withContext(Dispatchers.IO) {
            shoppingItemDao.deleteAll()  // You can create a deleteAll() method in your DAO
        }
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
            // Update the shopping item in the list
            shoppingItems[position].isChecked = isChecked

            // Notify the adapter to update the UI
            binding.rvShoppingList.post {
                adapter.notifyItemChanged(position)
            }

            // Update the item in the Room database asynchronously
            lifecycleScope.launch {
                updateItemInDb(shoppingItems[position])
            }
        }
    }

    // Update the item in the database
    private suspend fun updateItemInDb(item: ShoppingItem) {
        withContext(Dispatchers.IO) {
            shoppingItemDao.update(item)  // Assuming your DAO has an update method
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
