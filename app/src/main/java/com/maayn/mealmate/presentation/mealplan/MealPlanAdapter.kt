package com.maayn.mealmate.presentation.mealplan

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.maayn.mealmate.R
import com.maayn.mealmate.core.utils.NotificationReceiver
import com.maayn.mealmate.data.local.database.AppDatabase
import com.maayn.mealmate.data.local.entities.MealPlan
import com.maayn.mealmate.data.local.entities.ShoppingItem
import com.maayn.mealmate.utils.MealPlanDiffCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

class MealPlanAdapter(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val onStartCookingClick: (MealPlan) -> Unit,  // Passes MealPlan for navigation
    private val onEditClick: (MealPlan) -> Unit           // Passes MealPlan for edit
) : ListAdapter<MealPlan, MealPlanAdapter.MealPlanViewHolder>(MealPlanDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealPlanViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_meal_plan, parent, false)
        return MealPlanViewHolder(view)
    }

    override fun onBindViewHolder(holder: MealPlanViewHolder, position: Int) {
        holder.bind(getItem(position), onStartCookingClick, onEditClick)


    }

    inner class MealPlanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMealPlanName: TextView = itemView.findViewById(R.id.tvMealPlanName)
        private val tvDateMealType: TextView = itemView.findViewById(R.id.tvDateMealType)
        private val tvRecipeName: TextView = itemView.findViewById(R.id.tvRecipeName)
        private val ivRecipeImage: ImageView = itemView.findViewById(R.id.ivRecipeImage)
        private val btnEdit: MaterialButton = itemView.findViewById(R.id.btnEdit)
        private val btnStartCooking: MaterialButton = itemView.findViewById(R.id.btnStartCooking)
        private val btnAddToShoppingList: MaterialButton = itemView.findViewById(R.id.btnAddToShoppingList)
        private val ivDelete: ImageView = itemView.findViewById(R.id.ivDelete)
        private val ivNotification: ImageView = itemView.findViewById(R.id.ivNotification)

        fun bind(mealPlan: MealPlan, onStartCookingClick: (MealPlan) -> Unit, onEditClick: (MealPlan) -> Unit) {
            tvMealPlanName.text = mealPlan.name
            tvDateMealType.text = "${mealPlan.date} • ${mealPlan.mealType}"
            tvRecipeName.text = mealPlan.recipeName
            Glide.with(itemView).load(mealPlan.recipeImage).into(ivRecipeImage)

            btnEdit.setOnClickListener {
                onEditClick(mealPlan) // Calls edit function
            }

            btnStartCooking.setOnClickListener {
                onStartCookingClick(mealPlan) // Calls navigation function
            }

            ivNotification.setOnClickListener {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(context, NotificationReceiver::class.java).apply {
                    putExtra("mealName", mealPlan.name)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context, mealPlan.id ?: 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Set reminder time (Example: 1 hour before the meal time)
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 10)  // Set reminder hour (adjust based on mealPlan)
                    set(Calendar.MINUTE, 0)
                }

                // Schedule the alarm
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)

                Toast.makeText(context, "Reminder set for ${mealPlan.name}", Toast.LENGTH_SHORT).show()
            }

            ivDelete.setOnClickListener {
                MaterialAlertDialogBuilder(context)
                    .setTitle("Delete Meal Plan")
                    .setMessage("Are you sure you want to delete this meal plan? This action cannot be undone.")
                    .setPositiveButton("Delete") { _, _ ->
                        coroutineScope.launch {
                            val db = AppDatabase.getInstance(context)
                            db.mealPlanDao().deleteMealPlan(mealPlan)
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }


            btnAddToShoppingList.setOnClickListener {
                coroutineScope.launch {
                    val db = AppDatabase.getInstance(context)
                    val meal = db.mealDao().getMealWithDetails(mealPlan.recipeId)
                    meal.ingredients.forEach { ingredient ->
                        val shoppingItem = ShoppingItem(UUID.randomUUID().toString(), ingredient.name)
                        db.shoppingItemDao().insert(shoppingItem)
                    }
                }
                Toast.makeText(context, "Ingredients added to shopping list", Toast.LENGTH_SHORT).show()

            }

        }


    }
}
