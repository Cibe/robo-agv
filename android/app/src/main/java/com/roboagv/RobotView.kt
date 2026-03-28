package com.roboagv

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

/**
 * Custom view that draws a robot (circle + directional arrow) and animates it
 * when a navigation command is received.
 */
class RobotView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1565C0")
        style = Paint.Style.FILL
    }

    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#442196F3")
        style = Paint.Style.STROKE
        strokeWidth = 6f
        pathEffect = DashPathEffect(floatArrayOf(14f, 8f), 0f)
        strokeCap = Paint.Cap.ROUND
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#37474F")
        textSize = 34f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#15000000")
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    private val robotRadius = 44f
    private var robotX = 0f
    private var robotY = 0f
    private var robotAngle = 0f  // degrees; 0 = up (forward)
    private var currentDirection = "stop"

    private val trail = ArrayDeque<PointF>()
    private var animator: ValueAnimator? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        robotX = w / 2f
        robotY = h / 2f
    }

    fun setDirection(direction: String) {
        currentDirection = direction
        val (dx, dy, angle) = when (direction) {
            "forward" -> Triple(0f, -110f, 0f)
            "back"    -> Triple(0f, 110f, 180f)
            "left"    -> Triple(-110f, 0f, -90f)
            "right"   -> Triple(110f, 0f, 90f)
            else      -> { invalidate(); return }  // "stop" — just redraw
        }
        animateMovement(dx, dy, angle)
    }

    private fun animateMovement(dx: Float, dy: Float, targetAngle: Float) {
        animator?.cancel()

        trail.addLast(PointF(robotX, robotY))
        if (trail.size > 8) trail.removeFirst()

        val startX = robotX
        val startY = robotY
        val startAngle = robotAngle
        val endX = (robotX + dx).coerceIn(robotRadius, width - robotRadius)
        val endY = (robotY + dy).coerceIn(robotRadius, height - robotRadius)

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 700
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { anim ->
                val t = anim.animatedFraction
                robotX = startX + (endX - startX) * t
                robotY = startY + (endY - startY) * t
                robotAngle = startAngle + (targetAngle - startAngle) * t
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Background grid
        val step = 40f
        var x = 0f
        while (x <= width) { canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint); x += step }
        var y = 0f
        while (y <= height) { canvas.drawLine(0f, y, width.toFloat(), y, gridPaint); y += step }

        // Trail
        if (trail.size > 1) {
            val path = Path()
            path.moveTo(trail.first().x, trail.first().y)
            for (i in 1 until trail.size) path.lineTo(trail[i].x, trail[i].y)
            path.lineTo(robotX, robotY)
            canvas.drawPath(path, trailPaint)
        }

        // Robot body + arrow
        canvas.save()
        canvas.translate(robotX, robotY)
        canvas.rotate(robotAngle)

        // Shadow
        val shadowPaint = Paint(bodyPaint).apply {
            color = Color.parseColor("#33000000")
            maskFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawCircle(4f, 6f, robotRadius, shadowPaint)

        // Body
        canvas.drawCircle(0f, 0f, robotRadius, bodyPaint)

        // Directional arrow (triangle pointing up = forward)
        val arrowPath = Path().apply {
            moveTo(0f, -robotRadius * 0.65f)
            lineTo(-robotRadius * 0.38f, robotRadius * 0.32f)
            lineTo(robotRadius * 0.38f, robotRadius * 0.32f)
            close()
        }
        canvas.drawPath(arrowPath, arrowPaint)
        canvas.restore()

        // Direction label at bottom
        val label = when (currentDirection) {
            "forward" -> "Moving Forward"
            "back"    -> "Moving Back"
            "left"    -> "Turning Left"
            "right"   -> "Turning Right"
            "stop"    -> "Stopped"
            else      -> ""
        }
        canvas.drawText(label, width / 2f, height - 14f, labelPaint)
    }

    fun reset() {
        animator?.cancel()
        trail.clear()
        robotX = width / 2f
        robotY = height / 2f
        robotAngle = 0f
        currentDirection = "stop"
        invalidate()
    }
}
