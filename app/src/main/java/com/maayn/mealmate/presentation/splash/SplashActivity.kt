package com.maayn.mealmate.presentation.splash


import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.airbnb.lottie.LottieAnimationView
import com.maayn.mealmate.MainActivity
import com.maayn.mealmate.R

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Start Lottie animation
        val animationView = findViewById<LottieAnimationView>(R.id.lottie_splash)
        animationView.playAnimation()


        animationView.addAnimatorUpdateListener { animation ->
            if (animation.animatedFraction >= 0.5f) {
                animationView.removeAllUpdateListeners()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }
}
