package com.maayn.mealmate.presentation.grocery

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.maayn.mealmate.R

class ShoppingListAdapter(
    private var shoppingItems: MutableList<ShoppingItem>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<ShoppingListAdapter.ShoppingItemViewHolder>() {

    interface OnItemClickListener {
        fun onDeleteClick(position: Int)
        fun onCheckboxClick(position: Int, isChecked: Boolean)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShoppingItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shopping_list, parent, false)
        return ShoppingItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShoppingItemViewHolder, position: Int) {
        val item = shoppingItems[position]
        holder.tvIngredientName.text = item.name

        // Prevent multiple listeners from stacking
        holder.checkboxIngredient.setOnCheckedChangeListener(null)
        holder.checkboxIngredient.isChecked = item.isChecked

        // Apply or remove strikethrough
        holder.tvIngredientName.paintFlags = if (item.isChecked) {
            holder.tvIngredientName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.tvIngredientName.paintFlags and (Paint.STRIKE_THRU_TEXT_FLAG.inv())
        }

        // Reattach the listener
        holder.checkboxIngredient.setOnCheckedChangeListener { _, isChecked ->
            listener.onCheckboxClick(position, isChecked)
        }
    }

    override fun getItemCount(): Int = shoppingItems.size

    fun updateList(newList: List<ShoppingItem>) {
        shoppingItems.clear()
        shoppingItems.addAll(newList)
        notifyDataSetChanged()
    }

    inner class ShoppingItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkboxIngredient: CheckBox = itemView.findViewById(R.id.checkboxIngredient)
        val tvIngredientName: TextView = itemView.findViewById(R.id.tvIngredientName)
        val btnDeleteIngredient: ImageButton = itemView.findViewById(R.id.btnDeleteIngredient)

        init {
            btnDeleteIngredient.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onDeleteClick(position)
                }
            }

            checkboxIngredient.setOnCheckedChangeListener { _, isChecked ->
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onCheckboxClick(position, isChecked)
                }
            }
        }
    }
}
