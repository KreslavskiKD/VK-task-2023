package com.example.analogclockexample.clock


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import com.example.analogclockexample.R
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min


class AnalogClock @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    View(context, attrs, defStyleAttr) {

    private val intentReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (timeZone == null && Intent.ACTION_TIMEZONE_CHANGED == intent.action) {
                val tz = intent.getStringExtra("time-zone")
                time = Calendar.getInstance(TimeZone.getTimeZone(tz))
            }
            onTimeChanged()
        }
    }

    private val clockTick: Runnable = object : Runnable {
        override fun run() {
            onTimeChanged()
            if (enableSeconds) {
                val now = System.currentTimeMillis()
                val delay = DateUtils.SECOND_IN_MILLIS - now % DateUtils.SECOND_IN_MILLIS
                postDelayed(this, delay)
            }
        }
    }

    private var dial: Drawable
    private var hourHand: Drawable
    private var minuteHand: Drawable
    private var secondHand: Drawable
    private var time: Calendar
    private val descFormat: String
    private var timeZone: TimeZone? = null
    private var enableSeconds = true

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_TIME_TICK)
        filter.addAction(Intent.ACTION_TIME_CHANGED)
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED)
        context.registerReceiver(intentReceiver, filter)
        time = Calendar.getInstance(timeZone ?: TimeZone.getDefault())
        onTimeChanged()
        if (enableSeconds) clockTick.run()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        context.unregisterReceiver(intentReceiver)
        removeCallbacks(clockTick)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minWidth = max(dial.intrinsicWidth, suggestedMinimumWidth)
        val minHeight = max(dial.intrinsicHeight, suggestedMinimumHeight)
        setMeasuredDimension(
            getDefaultSize(minWidth, widthMeasureSpec),
            getDefaultSize(minHeight, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width
        val h = height
        val saveCount = canvas.save()
        canvas.translate((w / 2).toFloat(), (h / 2).toFloat())
        val scale = min(
            w.toFloat() / dial.intrinsicWidth,
            h.toFloat() / dial.intrinsicHeight
        )
        if (scale < 1f) {
            canvas.scale(scale, scale, 0f, 0f)
        }
        dial.draw(canvas)

        val hourAngle = time[Calendar.HOUR] * 30f
        canvas.rotate(hourAngle, 0f, 0f)
        hourHand.draw(canvas)

        val minuteAngle = time[Calendar.MINUTE] * 6f
        canvas.rotate(minuteAngle - hourAngle, 0f, 0f)
        minuteHand.draw(canvas)

        if (enableSeconds) {
            val secondAngle = time[Calendar.SECOND] * 6f
            canvas.rotate(secondAngle - minuteAngle, 0f, 0f)
            secondHand.draw(canvas)
        }
        canvas.restoreToCount(saveCount)
    }

    override fun verifyDrawable(who: Drawable): Boolean =
        dial === who || hourHand === who || minuteHand === who || secondHand === who || super.verifyDrawable(who)


    private fun initDrawable(drawable: Drawable) {
        val midX = drawable.intrinsicWidth / 2
        val midY = drawable.intrinsicHeight / 2
        drawable.setBounds(-midX, -midY, midX, midY)
    }

    private fun onTimeChanged() {
        time.timeInMillis = System.currentTimeMillis()
        contentDescription = DateFormat.format(descFormat, time)
        invalidate()
    }

    init {
        context.resources
        val a = context.obtainStyledAttributes(attrs, R.styleable.AnalogClock)
        time = Calendar.getInstance()
        descFormat = (DateFormat.getTimeFormat(context) as SimpleDateFormat).toLocalizedPattern()
        enableSeconds = a.getBoolean(R.styleable.AnalogClock_showSecondHand, true)
        dial = a.getDrawable(R.styleable.AnalogClock_dial) ?: AppCompatResources.getDrawable(context, R.drawable.ic_dial)!!
        hourHand = a.getDrawable(R.styleable.AnalogClock_hour) ?: AppCompatResources.getDrawable(context, R.drawable.hour)!!
        minuteHand = a.getDrawable(R.styleable.AnalogClock_minute) ?: AppCompatResources.getDrawable(context, R.drawable.minute)!!
        secondHand = a.getDrawable(R.styleable.AnalogClock_second) ?: AppCompatResources.getDrawable(context, R.drawable.second)!!
        initDrawable(dial)
        initDrawable(hourHand)
        initDrawable(minuteHand)
        initDrawable(secondHand)
    }
}