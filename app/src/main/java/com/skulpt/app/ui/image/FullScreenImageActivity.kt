package com.skulpt.app.ui.image

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.skulpt.app.databinding.ActivityFullScreenImageBinding

class FullScreenImageActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityFullScreenImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageUri = intent.getStringExtra(EXTRA_IMAGE_URI)

        Glide.with(this)
            .load(imageUri)
            .error(com.skulpt.app.R.drawable.ic_exercise_placeholder)
            .into(binding.ivFullScreen)

        binding.btnClose.setOnClickListener { finish() }

        // Full screen immersive
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }
}
