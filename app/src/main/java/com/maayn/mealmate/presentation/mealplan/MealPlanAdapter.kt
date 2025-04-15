package com.maayn.mealmate.presentation.mealplan

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.maayn.mealmate.R
import com.maayn.mealmate.core.utils.MealNotificationWorker
import com.maayn.mealmate.data.local.database.AppDatabase
import com.maayn.mealmate.data.local.entities.MealPlan
import com.maayn.mealmate.data.local.entities.ShoppingItem
import com.maayn.mealmate.utils.MealPlanDiffCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

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
                val mealDateString = mealPlan.date // e.g. "21/03/2025 17:22"
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

                try {
                    val mealDate = dateFormat.parse(mealDateString) ?: return@setOnClickListener
                    val currentTimeMillis = System.currentTimeMillis()
                    val oneHourBeforeMeal = mealDate.time - TimeUnit.HOURS.toMillis(1)

                    if (currentTimeMillis >= oneHourBeforeMeal) {
                        Toast.makeText(context, "Meal time is too close or already passed", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    val delayMillis = oneHourBeforeMeal - currentTimeMillis
                    Log.e("MealPlanAdapter", "Delay: $delayMillis ms")

                    val workRequest = OneTimeWorkRequestBuilder<MealNotificationWorker>()
                        .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                        .setInputData(
                            workDataOf(
                                "mealName" to mealPlan.name,
                                "mealTimeMillis" to mealDate.time
                            )
                        )
                        .build()

                    WorkManager.getInstance(context).enqueue(workRequest)
                    Toast.makeText(context, "Reminder set for ${mealPlan.name} one hour before meal time", Toast.LENGTH_SHORT).show()

                } catch (e: Exception) {
                    Log.e("MealPlanAdapter", "Error parsing date: ${e.message}")
                    Toast.makeText(context, "Invalid meal date format", Toast.LENGTH_SHORT).show()
                }
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

                    if (meal != null && meal.ingredients.isNotEmpty()) {
                        meal.ingredients.forEach { ingredient ->
                            val shoppingItem = ShoppingItem(
                                id = UUID.randomUUID().toString(),
                                name = ingredient.name // Assuming ingredient has a 'name' property
                            )
                            db.shoppingItemDao().insert(shoppingItem)
                        }
                        Toast.makeText(context, "Ingredients added to shopping list", Toast.LENGTH_SHORT).show()
                    } else {
                        // Handle the case where meal or ingredients are null or empty
                        Toast.makeText(context, "No ingredients available to add", Toast.LENGTH_SHORT).show()
                    }
                }
            }


        }


    }
}
