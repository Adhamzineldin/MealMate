package com.maayn.mealmate.presentation.home.model

data class Instruction(
    val step: Int,
    val description: String
) {
    companion object {
        fun fromText(text: String): List<Instruction> {
            return text.split(". ").mapIndexed { index, sentence ->
                Instruction(index + 1, sentence.trim())
            }
        }
    }
}