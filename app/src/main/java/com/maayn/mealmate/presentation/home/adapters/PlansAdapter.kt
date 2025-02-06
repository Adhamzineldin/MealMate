package com.maayn.mealmate.presentation.home.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.maayn.mealmate.R
import com.maayn.mealmate.databinding.ItemPlanBinding
import com.maayn.mealmate.presentation.home.model.*

class PlansAdapter(
    private val plans: List<PlanItem>,
    private val onPlanClick: (PlanItem) -> Unit = {},
    private val onCookClick: (PlanItem) -> Unit = {},
    private val onCookedToggle: (PlanItem) -> Unit = {},
    private val onFavoriteClick: (PlanItem) -> Unit = {}
) : RecyclerView.Adapter<PlansAdapter.PlanViewHolder>() {

    inner class PlanViewHolder(
        private val binding: ItemPlanBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PlanItem) {
            binding.apply {
                // Get LayoutInflater using root.context
                val layoutInflater = root.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

                // Set meal type icon and text
                val (icon, title) = when (item.mealType) {
                    MealType.BREAKFAST -> Pair(R.drawable.ic_meal_breakfast, "Breakfast")
                    MealType.LUNCH -> Pair(R.drawable.ic_meal_lunch, "Lunch")
                    MealType.DINNER -> Pair(R.drawable.ic_meal_dinner, "Dinner")
                }
                ivMealIcon.setImageResource(icon)
                tvMealType.text = title

                // Load dynamic image if available
                item.dynamicImageUrl?.let {
                    Glide.with(root.context)
                        .load(it) // Use Glide to load the image dynamically
//                        .into(ivMealImage)  // Assuming ivMealImage is the ImageView for the meal image
                }

                // Set favorite state and click listener
                ivFavorite.setImageResource(
                    if (item.isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
                )
                ivFavorite.setOnClickListener { onFavoriteClick(item) }

                // Set time and servings
                tvDuration.text = "${item.duration} minutes"
                tvServings.text = "${item.servings} serve"

                // Clear existing chips and add new ones for the menu items
                menuItemsGroup.removeAllViews()
                item.menuItems.forEach { menuItem ->
                    val chip = layoutInflater.inflate(
                        R.layout.item_menu_chip,
                        menuItemsGroup,
                        false
                    ) as com.google.android.material.chip.Chip

                    chip.text = menuItem.name + if (menuItem.extraCount > 0) {
                        " +${menuItem.extraCount}"
                    } else ""
                    menuItemsGroup.addView(chip)
                }

                // Set button states and click listeners
                btnCooked.isChecked = item.isCooked
                btnCooked.setOnClickListener { onCookedToggle(item) }
                btnCook.setOnClickListener { onCookClick(item) }

                // Set click listener for the entire item
                root.setOnClickListener { onPlanClick(item) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanViewHolder {
        return PlanViewHolder(
            ItemPlanBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: PlanViewHolder, position: Int) {
        holder.bind(plans[position])
    }

    override fun getItemCount(): Int = plans.size
}
