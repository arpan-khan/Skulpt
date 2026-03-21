package com.skulpt.app.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.graphics.drawable.shapes.RectShape
import androidx.core.content.ContextCompat
import com.skulpt.app.R

object PlaceholderUtil {

    private val exerciseColorMap = mapOf(
        "push" to "#E53935",
        "pull" to "#8E24AA",
        "squat" to "#1E88E5",
        "lunge" to "#00ACC1",
        "plank" to "#43A047",
        "dead" to "#F4511E",
        "bench" to "#039BE5",
        "curl" to "#7CB342",
        "press" to "#FB8C00",
        "row" to "#00897B",
        "dip" to "#E91E63",
        "jump" to "#FF5722",
        "crunch" to "#9C27B0",
        "leg" to "#3F51B5",
        "hip" to "#F06292",
        "run" to "#FF7043",
        "bike" to "#26A69A",
        "default" to "#6750A4"
    )

    fun getColorForExercise(exerciseName: String): Int {
        val lower = exerciseName.lowercase()
        val hex = exerciseColorMap.entries
            .firstOrNull { lower.contains(it.key) }?.value
            ?: exerciseColorMap["default"]!!
        return Color.parseColor(hex)
    }

    fun getPlaceholderDrawable(context: Context, exerciseName: String): Drawable {
        val color = getColorForExercise(exerciseName)
        val letter = exerciseName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        return LetterDrawable(color, letter)
    }

    class LetterDrawable(private val bgColor: Int, private val letter: String) : Drawable() {

        private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            style = Paint.Style.FILL
        }

        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

        override fun draw(canvas: Canvas) {
            val bounds = bounds
            val cx = bounds.exactCenterX()
            val cy = bounds.exactCenterY()
            val radius = minOf(bounds.width(), bounds.height()) / 2f

            canvas.drawCircle(cx, cy, radius, bgPaint)

            textPaint.textSize = radius * 0.9f
            val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2
            canvas.drawText(letter, cx, textY, textPaint)
        }

        override fun setAlpha(alpha: Int) { bgPaint.alpha = alpha }
        override fun setColorFilter(cf: android.graphics.ColorFilter?) {
            bgPaint.colorFilter = cf
        }
        @Deprecated("Deprecated in Java")
        override fun getOpacity() = android.graphics.PixelFormat.TRANSLUCENT
    }

    fun getDynamicImageUrl(exerciseName: String, baseQuery: String? = null): String {
        val query = exerciseName.trim().replace(" ", "+")
        val suffix = if (baseQuery.isNullOrBlank()) "workout+gym" else baseQuery.trim().replace(" ", "+")

        return "https://tse1.mm.bing.net/th?q=$query+$suffix&pid=Api"
    }

    fun getSearchImageUrl(query: String, seed: Int, baseQuery: String? = null): String {
        val encodedQuery = query.trim().replace(" ", "+")
        val suffix = if (baseQuery.isNullOrBlank()) "workout" else baseQuery.trim().replace(" ", "+")
        return "https://tse1.mm.bing.net/th?q=$encodedQuery+$suffix&pid=Api"
    }
}
