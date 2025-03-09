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

    private lateinit var adapter: ShoppingListAdapter
    private lateinit var shoppingItemDao: ShoppingItemDao

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShoppingListBinding.inflate(inflater, container, false)

        // Initialize DAO
        val database = AppDatabase.getInstance(requireContext())
        shoppingItemDao = database.shoppingItemDao()

        setupRecyclerView()
        setupClickListeners()
        loadShoppingItems()

        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = ShoppingListAdapter(this)
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
        val newItem = ShoppingItem(UUID.randomUUID().toString(), ingredientName, false)

        lifecycleScope.launch(Dispatchers.IO) {
            shoppingItemDao.insert(newItem)
            val updatedList = shoppingItemDao.getAll()
            withContext(Dispatchers.Main) {
                adapter.submitList(updatedList)
                updateEmptyState(updatedList.isEmpty())
            }
        }
    }

    private fun loadShoppingItems() {
        lifecycleScope.launch(Dispatchers.IO) {
            val items = shoppingItemDao.getAll()
            Log.d("ShoppingListFragment", "Loaded items: $items")
            withContext(Dispatchers.Main) {
                adapter.submitList(items)
                updateEmptyState(items.isEmpty())
            }
        }
    }

    private fun removeItem(position: Int) {
        val item = adapter.currentList[position]

        lifecycleScope.launch(Dispatchers.IO) {
            shoppingItemDao.delete(item)
            val updatedList = shoppingItemDao.getAll()
            withContext(Dispatchers.Main) {
                adapter.submitList(updatedList)
                updateEmptyState(updatedList.isEmpty())
            }
        }
    }

    private fun clearAllItems() {
        lifecycleScope.launch(Dispatchers.IO) {
            shoppingItemDao.deleteAll()
            withContext(Dispatchers.Main) {
                adapter.submitList(emptyList())
                updateEmptyState(true)
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        with(binding) {
            rvShoppingList.visibility = if (isEmpty) View.GONE else View.VISIBLE
            layoutEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
            btnClearAll.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }
    }

    override fun onDeleteClick(position: Int) {
        removeItem(position)
    }

    override fun onCheckboxClick(position: Int, isChecked: Boolean) {
        val item = adapter.currentList[position].copy(isChecked = isChecked)

        lifecycleScope.launch(Dispatchers.IO) {
            shoppingItemDao.update(item)
            val updatedList = shoppingItemDao.getAll()
            withContext(Dispatchers.Main) {
                adapter.submitList(updatedList)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

    }
}
