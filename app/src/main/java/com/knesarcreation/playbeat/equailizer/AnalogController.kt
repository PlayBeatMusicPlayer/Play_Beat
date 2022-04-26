package com.knesarcreation.playbeat.equailizer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View


class AnalogController : View {
    var midx = 0f
    var midy = 0f
    var textPaint: Paint? = null
    var circlePaint: Paint? = null
    var circlePaint2: Paint? = null
    var linePaint: Paint? = null
    var angle: String? = null
    var currdeg = 0f
    var deg = 3f
    var downdeg = 0f
    var progressColor = 0
    var lineColor = 0
    var mListener: onProgressChangedListener? = null
    var label: String? = null

    override fun isEnabled(): Boolean {
        return super.isEnabled()
    }

    interface onProgressChangedListener {
        fun onProgressChanged(progress: Int)
    }

    fun setOnProgressChangedListener(listener: onProgressChangedListener?) {
        mListener = listener
    }

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    private fun init() {
        textPaint = Paint()
        textPaint!!.color = Color.WHITE
        textPaint!!.style = Paint.Style.FILL
        textPaint!!.textSize = 33f
        textPaint!!.isFakeBoldText = true
        textPaint!!.textAlign = Paint.Align.CENTER
        circlePaint = Paint()
        circlePaint!!.color = Color.parseColor("#34000000")
        circlePaint!!.style = Paint.Style.FILL_AND_STROKE
        circlePaint2 = Paint()
        circlePaint2!!.color = EqualizerFragment.themeColor
        //        circlePaint2.setColor(Color.parseColor("#FFA036"));
        circlePaint2!!.style = Paint.Style.FILL
        linePaint = Paint()
        linePaint!!.color = EqualizerFragment.themeColor
        //        linePaint.setColor(Color.parseColor("#FFA036"));
        linePaint!!.strokeWidth = 7f
        angle = "0.0"
        label = "Label"
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        midx = (canvas.width / 2).toFloat()
        midy = (canvas.height / 2).toFloat()
        val ang = 0
        var x = 0f
        var y = 0f
        val radius = (Math.min(midx, midy) * (14.5.toFloat() / 16)).toInt()
        val deg2 = Math.max(3f, deg)
        val deg3 = Math.min(deg, 21f)
        for (i in deg2.toInt()..21) {
            val tmp = i.toFloat() / 24
            x = midx + (radius * Math.sin(2 * Math.PI * (1.0 - tmp))).toFloat()
            y = midy + (radius * Math.cos(2 * Math.PI * (1.0 - tmp))).toFloat()
            circlePaint!!.color = Color.parseColor("#34000000")
            canvas.drawCircle(x, y, radius.toFloat() / 15, circlePaint!!)
        }
        var i = 3
        while (i <= deg3) {
            val tmp = i.toFloat() / 24
            x = midx + (radius * Math.sin(2 * Math.PI * (1.0 - tmp))).toFloat()
            y = midy + (radius * Math.cos(2 * Math.PI * (1.0 - tmp))).toFloat()
            canvas.drawCircle(x, y, radius.toFloat() / 15, circlePaint2!!)
            i++
        }
        val tmp2 = deg / 24
        val x1 =
            midx + (radius * (2.toFloat() / 5) * Math.sin(2 * Math.PI * (1.0 - tmp2))).toFloat()
        val y1 =
            midy + (radius * (2.toFloat() / 5) * Math.cos(2 * Math.PI * (1.0 - tmp2))).toFloat()
        val x2 =
            midx + (radius * (3.toFloat() / 5) * Math.sin(2 * Math.PI * (1.0 - tmp2))).toFloat()
        val y2 =
            midy + (radius * (3.toFloat() / 5) * Math.cos(2 * Math.PI * (1.0 - tmp2))).toFloat()
        circlePaint!!.color = Color.parseColor("#34353535")
        canvas.drawCircle(midx, midy, radius * (13.toFloat() / 15), circlePaint!!)
        circlePaint!!.color = Color.parseColor("#34000000")
        canvas.drawCircle(midx, midy, radius * (11.toFloat() / 15), circlePaint!!)
        canvas.drawText(label!!, midx, midy + (radius * 1.1).toFloat(), textPaint!!)
        canvas.drawLine(x1, y1, x2, y2, linePaint!!)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if(isEnabled) {
            mListener!!.onProgressChanged((deg - 2).toInt())
            if (e.action == MotionEvent.ACTION_DOWN) {
                val dx = e.x - midx
                val dy = e.y - midy
                downdeg = (Math.atan2(dy.toDouble(), dx.toDouble()) * 180 / Math.PI).toFloat()
                downdeg -= 90f
                if (downdeg < 0) {
                    downdeg += 360f
                }
                downdeg = Math.floor((downdeg / 15).toDouble()).toFloat()
                return true
            }
            if (e.action == MotionEvent.ACTION_MOVE) {
                val dx = e.x - midx
                val dy = e.y - midy
                currdeg = (Math.atan2(dy.toDouble(), dx.toDouble()) * 180 / Math.PI).toFloat()
                currdeg -= 90f
                if (currdeg < 0) {
                    currdeg += 360f
                }
                currdeg = Math.floor((currdeg / 15).toDouble()).toFloat()
                if (currdeg == 0f && downdeg == 23f) {
                    deg++
                    if (deg > 21) {
                        deg = 21f
                    }
                    downdeg = currdeg
                } else if (currdeg == 23f && downdeg == 0f) {
                    deg--
                    if (deg < 3) {
                        deg = 3f
                    }
                    downdeg = currdeg
                } else {
                    deg += currdeg - downdeg
                    if (deg > 21) {
                        deg = 21f
                    }
                    if (deg < 3) {
                        deg = 3f
                    }
                    downdeg = currdeg
                }
                angle = deg.toString()
                invalidate()
                return true
            }
        }
        return e.action == MotionEvent.ACTION_UP || super.onTouchEvent(e)

    }

    var progress: Int
        get() = (deg - 2).toInt()
        set(param) {
            deg = (param + 2).toFloat()
        }
}