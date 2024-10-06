package playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.draw.canvas

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.annotation.IntRange
import playaxis.appinn.note_it.application.QuickNotepad
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.draw.helper.PathCommand
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.draw.helper.SerializableDrawingPath
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.draw.interfaces.TouchEventListeners
import java.io.Serializable
import kotlin.math.pow
import kotlin.math.sqrt


class DrawingView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var mDrawPath: CustomPath? = null
    private var mCanvasBitmap: Bitmap? = null
    private var mDrawPaint: Paint? = null
    private var mCanvasPaint: Paint? = null
    private var mBrushSize: Int = 8
    private var currentColor = Color.BLACK
    private var canvas: Canvas? = null
    private var mAlpha: Int = 255
    private var mPaths = ArrayList<CustomPath>()
    private var mUndoPath = ArrayList<CustomPath>()
    private var previousTime: Long = 0
    private var previousPosition: Pair<Float, Float>? = null
    private var currentSize = 0

    var pathClickEvent: Boolean = false
    private var isDrawingEnabled: Boolean = true

    private var pathEffects = arrayOf<PathEffect?>(null, null, null, null) // Array to hold path effects
    private var currentPathEffect: PathEffect? = null // Variable to hold the current path effect

    lateinit var touchEventListeners: TouchEventListeners

    init {
        setUpDrawing()
    }

    private fun setUpDrawing() {
        mDrawPaint = Paint().apply {
            color = currentColor
            style = Paint.Style.STROKE
            alpha = mAlpha
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
        mDrawPath = CustomPath(currentColor, mBrushSize, mAlpha)
        mCanvasPaint = Paint(Paint.DITHER_FLAG)
        currentSize = mBrushSize

        mCanvasBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        canvas = Canvas(mCanvasBitmap!!)
    }

    // Method to select the pen type and apply the corresponding path effect
    fun selectPenType(type: Int) {
        currentPathEffect = when (type) {
            1 -> CornerPathEffect(10f) // Ink Pen effect
            2 -> DashPathEffect(floatArrayOf(10f, 5f), 0f) // Marker effect
            3 -> ComposePathEffect(
                DashPathEffect(floatArrayOf(0f, 0f), 0f),
                CornerPathEffect(10f)
            ) // Highlighter effect
            else -> null // No effect
        }
        Log.d("DrawingView", "Selected pen type: $type, pathEffect: $currentPathEffect")
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(mCanvasBitmap!!, 0f, 0f, mCanvasPaint)

        for (path in mPaths) {
            mDrawPaint!!.apply {
                strokeWidth = path.brushThickness.toFloat()
                color = path.color
                alpha = path.alpha
                pathEffect = path.pathEffect
            }
            canvas.drawPath(path, mDrawPaint!!)
        }

        if (!mDrawPath!!.isEmpty) {
            mDrawPaint!!.apply {
                strokeWidth = mDrawPath!!.brushThickness.toFloat()
                color = mDrawPath!!.color
                alpha = mDrawPath!!.alpha
                pathEffect = mDrawPath!!.pathEffect
            }
            canvas.drawPath(mDrawPath!!, mDrawPaint!!)
        }
    }

    @SuppressLint("ClickableViewAccessibility", "SuspiciousIndentation")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchX = event?.x
        val touchY = event?.y
        touchEventListeners.onTouchEvent(event)

        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                mDrawPath!!.apply {
                    color = currentColor
                    brushThickness = mBrushSize
                    alpha = mAlpha
                    pathEffect = currentPathEffect
                    reset()
                }
                if (isDrawingEnabled) {
                    touchX?.let { x ->
                        touchY?.let { y ->
                            mDrawPath!!.moveTo(x, y)
                            mDrawPath!!.lineTo(x, y)
                        }
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                pathClickEvent = false

                if (isDrawingEnabled) {
                    touchX?.let { x ->
                        touchY?.let { y ->
                            mDrawPath!!.lineTo(x, y)
                        }
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                if (isDrawingEnabled) {
                    touchX?.let { x ->
                        touchY?.let { y ->
                            if (mDrawPath!!.isEmpty) {
                                mDrawPath!!.moveTo(x, y)
                                mDrawPath!!.lineTo(x, y)
                            }
                            mPaths.add(mDrawPath!!)
                        }
                    }
                    mDrawPath = CustomPath(currentColor, mBrushSize, mAlpha).apply {
                        pathEffect = currentPathEffect
                    }
                } else {
                    touchX?.let { x ->
                        touchY?.let { y ->
                            val iterator = mPaths.iterator()
                            while (iterator.hasNext()) {
                                val path = iterator.next()
                                if (isClickOnPath(path, x, y)) {
                                    iterator.remove()
                                    break
                                }
                            }
                        }
                    }
                }
            }

            else -> return false
        }

        invalidate()
        return true
    }

    private fun isClickOnPath(path: CustomPath, x: Float, y: Float, threshold: Float = 20f): Boolean {
        val pathMeasure = PathMeasure(path, false)
        val pathLength = pathMeasure.length
        val coords = FloatArray(2)

        if (pathLength == 0f && mPaths.isNotEmpty()) {
            for (command in path.getCommands()) {
                val commandX = command.x
                val commandY = command.y
                if (sqrt((commandX - x).pow(2) + (commandY - y).pow(2)) <= threshold)
                    return true
            }
        } else {
            var distance = 0f
            while (distance <= pathLength) {
                pathMeasure.getPosTan(distance, coords, null)
                val pathX = coords[0]
                val pathY = coords[1]
                if (sqrt((pathX - x).pow(2) + (pathY - y).pow(2)) <= threshold)
                    return true
                distance += 1f // Adjust step size as needed for precision
            }
        }

        return false
    }

    private fun calculateSpeed(currentPosition: Pair<Float, Float>): Float {
        val currentTime = System.currentTimeMillis()
        val timeInterval = (currentTime - previousTime).toFloat() / 1000  // Convert to seconds

        Log.i("timeInterval: ", timeInterval.toString())
        val distance = previousPosition?.let {
            sqrt(
                (currentPosition.first - it.first).pow(2) +
                        (currentPosition.second - it.second).pow(2)
            )
        } ?: 0f

        val speed = if (timeInterval > 2) {
            previousTime = currentTime
            distance / timeInterval
        }
        else
            0f

        previousPosition = currentPosition
        return speed
    }

    fun enableDrawing(enable: Boolean) {
        isDrawingEnabled = enable
    }

    @SuppressLint("SupportAnnotationUsage")
    @IntRange(from = 0, to = 200)
    fun setSizeForBrush(newSize: Int) {
        mBrushSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            newSize.toFloat(),
            resources.displayMetrics
        ).toInt()
        currentSize = mBrushSize
        mDrawPaint!!.strokeWidth = mBrushSize.toFloat()
    }

    fun setBrushColor(newColor: Int) {
        currentColor = newColor
        mDrawPaint!!.color = currentColor
    }

    fun restoreDrawing(mPaths: ArrayList<CustomPath>) {
        this.mPaths = mPaths
    }

    fun setBrushAlpha(alpha: Int) {
        mAlpha = alpha
        mDrawPaint!!.alpha = alpha
    }

    fun erase(colorBackground: Int = Color.WHITE) {
        mAlpha = 255
        mDrawPaint!!.alpha = 255
        currentColor = colorBackground
        mDrawPaint!!.color = colorBackground
    }

    fun undo() {
        if (mPaths.size > 0) {
            mUndoPath.add(mPaths[mPaths.size - 1])
            mPaths.removeAt(mPaths.size - 1)
            invalidate()
        }
    }

    /**
     * will redo the undo-ed strokes
     **/
    fun redo() {
        if (mUndoPath.size > 0) {
            mPaths.add(mUndoPath[mUndoPath.size - 1])
            mUndoPath.removeAt(mUndoPath.size - 1)
            invalidate()
        }
    }

    fun imagePathSize(): Int = mPaths.size
    fun undoPathSize(): Int = mUndoPath.size

    /**
     * will remove all the stores but not those saved in redo()
     */
    fun clearDrawingBoard() {
        mPaths.clear()
        invalidate()

    }

    fun getDrawing(): ArrayList<CustomPath> {
        return mPaths
    }

    inner class CustomPath(var color: Int, var brushThickness: Int, var alpha: Int) : Path(), Serializable {

        private val commands = mutableListOf<PathCommand>()
        var pathEffect: PathEffect? = null // Add a variable to hold the path effect

        override fun moveTo(x: Float, y: Float) {
            super.moveTo(x, y)
            commands.add(PathCommand("moveTo", x, y))
        }

        override fun lineTo(x: Float, y: Float) {
            super.lineTo(x, y)
            commands.add(PathCommand("lineTo", x, y))
        }

        // Add similar wrappers for other Path methods like quadTo, cubicTo if used
        fun serialize(): SerializableDrawingPath {
            return SerializableDrawingPath(color, brushThickness, alpha, commands)
        }

        fun getCommands(): List<PathCommand> {
            return commands
        }
    }

    companion object {

        val SIMPLE = 0
        val INK_PEN = 1
        val MARKER_PEN = 2
        val HIGHLIGHTER_PEN = 3

        fun deserialize(data: SerializableDrawingPath): CustomPath {
            val path = DrawingView(QuickNotepad.appContext, null).CustomPath(
                data.color,
                data.brushThickness,
                data.alpha
            )
            for (command in data.pathCommands) {
                when (command.type) {
                    "moveTo" -> path.moveTo(command.x, command.y)
                    "lineTo" -> path.lineTo(command.x, command.y)
                    // handle other commands similarly
                }
            }
            return path
        }
    }
}