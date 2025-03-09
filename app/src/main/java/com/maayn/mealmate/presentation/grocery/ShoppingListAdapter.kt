package com.maayn.mealmate.presentation.grocery

import android.app.AlertDialog
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.maayn.mealmate.data.local.entities.ShoppingItem
import com.maayn.mealmate.databinding.ItemShoppingListBinding

class ShoppingListAdapter(private val listener: OnItemClickListener) :
    ListAdapter<ShoppingItem, ShoppingListAdapter.ShoppingItemViewHolder>(ShoppingItemDiffCallback()) {

    interface OnItemClickListener {
        fun onDeleteClick(position: Int)
        fun onCheckboxClick(position: Int, isChecked: Boolean)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShoppingItemViewHolder {
        val binding = ItemShoppingListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ShoppingItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ShoppingItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ShoppingItemViewHolder(private val binding: ItemShoppingListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ShoppingItem) {
            binding.tvIngredientName.text = item.name
            binding.checkboxIngredient.isChecked = item.isChecked

            // Apply or remove strikethrough
            binding.tvIngredientName.paintFlags = if (item.isChecked) {
                binding.tvIngredientName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.tvIngredientName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            binding.checkboxIngredient.setOnCheckedChangeListener(null)
            binding.checkboxIngredient.setOnCheckedChangeListener { _, isChecked ->
                listener.onCheckboxClick(adapterPosition, isChecked)
            }

            binding.btnDeleteIngredient.setOnClickListener {
                AlertDialog.Builder(it.context)
                    .setTitle("Delete Ingredient")
                    .setMessage("Are you sure you want to delete this ingredient?")
                    .setPositiveButton("Delete") { _, _ ->
                        listener.onDeleteClick(adapterPosition)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }
}

class ShoppingItemDiffCallback : DiffUtil.ItemCallback<ShoppingItem>() {
    override fun areItemsTheSame(oldItem: ShoppingItem, newItem: ShoppingItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ShoppingItem, newItem: ShoppingItem): Boolean {
        return oldItem == newItem
    }
}
