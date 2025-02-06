// com/maayn/mealmate/presentation/home/BottomNavAnimation.kt

package com.maayn.mealmate.presentation.home

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Color
import android.widget.ImageView

class BottomNavAnimation {

    fun animateNavigationIcon(view: ImageView, selected: Boolean) {
        val moveAnimator = ObjectAnimator.ofFloat(view, "translationY", if (selected) 0f else -30f, if (selected) -30f else 0f).apply { duration = 300 }
        val targetColor = if (selected) Color.WHITE else Color.parseColor("#757575")
        view.setColorFilter(targetColor)

        AnimatorSet().apply {
            play(moveAnimator)
            start()
        }
    }
}
