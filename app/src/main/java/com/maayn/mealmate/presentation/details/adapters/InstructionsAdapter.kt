package com.maayn.mealmate.presentation.details.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.maayn.mealmate.data.local.entities.InstructionEntity
import com.maayn.mealmate.databinding.ItemInstructionBinding

class InstructionsAdapter(private val steps: List<InstructionEntity>) : RecyclerView.Adapter<InstructionsAdapter.StepViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepViewHolder {
        val binding = ItemInstructionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StepViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StepViewHolder, position: Int) {
        holder.bind(position + 1, steps[position])
    }

    override fun getItemCount(): Int = steps.size

    class StepViewHolder(private val binding: ItemInstructionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(stepNumber: Int, instructionEntity: InstructionEntity) {
            binding.tvStepNumber.text = stepNumber.toString()
            binding.tvStepInstruction.text = instructionEntity.description.replace(Regex("^\\d+\\.\\s*"), "")
        }
    }

}
