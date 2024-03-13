package com.example.drawingapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

class DrawingView(content: Context, attrs: AttributeSet) : View(content, attrs) {

    // Path currently being drawn.
    private var mDrawPath: CustomPath? = null
    // Bitmap on which drawing takes place.
    private var mCanvasBitmap: Bitmap? = null
    // Paint for drawing paths.
    private var mDrawPaint: Paint? = null
    // Canvas paint for the entire canvas.
    private var mCanvasPaint: Paint? = null
    // Current brush size for drawing.
    private var mBrushSize: Float = 0.toFloat()
    // Default drawing color.
    private var color = Color.BLACK
    // Canvas object to draw on.
    private var canvas: Canvas? = null
    // List to store all the paths drawn.
    private val mPath = ArrayList<CustomPath>()
    // List to store paths that are undone.
    private val mUndoPath = ArrayList<CustomPath>()

    // Setup drawing properties when the view is created.
    init {
        setUpDrawing()
    }

    // Function to handle undo action by removing the last path added.
    fun onClickUndo() {
        if (mPath.size > 0) {
            mUndoPath.add(mPath.removeAt(mPath.size - 1))
            invalidate() // Redraw to reflect the change.
        }
    }

    // Sets up the default settings for the drawing tool.
    private fun setUpDrawing() {
        mDrawPaint = Paint()
        mDrawPath = CustomPath(color, mBrushSize)
        // Setup the paint for drawing.
        mDrawPaint!!.color = color
        mDrawPaint!!.style = Paint.Style.STROKE
        mDrawPaint!!.strokeJoin = Paint.Join.ROUND
        mDrawPaint!!.strokeCap = Paint.Cap.ROUND
        // Paint object for canvas, with dither flag for better color blending.
        mCanvasPaint = Paint(Paint.DITHER_FLAG)
    }

    // Called when the size of the view changes, for example, at creation or rotation.
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Create a bitmap and canvas based on the new size.
        mCanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(mCanvasBitmap!!)
    }

    // Draws the paths and the bitmap on the canvas.
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        // Draw the bitmap that represents the drawing by the user.
        canvas?.drawBitmap(mCanvasBitmap!!, 0f, 0f, mCanvasPaint)

        // Draw each path added to mPath.
        for (path in mPath) {
            mDrawPaint!!.strokeWidth = path.brushThickness
            mDrawPaint!!.color = path.color
            canvas?.drawPath(path, mDrawPaint!!)
        }

        // Draw the current path.
        if (!mDrawPath!!.isEmpty) {
            mDrawPaint!!.strokeWidth = mDrawPath!!.brushThickness
            mDrawPaint!!.color = mDrawPath!!.color
            canvas?.drawPath(mDrawPath!!, mDrawPaint!!)
        }
    }

    // Handles user touch events to draw on the canvas.
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchX = event?.x
        val touchY = event?.y

        // Process touch events and draw lines accordingly.
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                mDrawPath!!.color = color
                mDrawPath!!.brushThickness = mBrushSize

                mDrawPath!!.reset()
                if (touchX != null && touchY != null) {
                    mDrawPath!!.moveTo(touchX, touchY)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (touchX != null && touchY != null) {
                    mDrawPath!!.lineTo(touchX, touchY)
                }
            }
            MotionEvent.ACTION_UP -> {
                // Add path to mPath and prepare for the next drawing.
                mPath.add(mDrawPath!!)
                mDrawPath = CustomPath(color, mBrushSize)
            }
            else -> return false
        }
        invalidate() // Redraw the view.
        return true
    }

    // Sets the brush size, adjusting for screen density.
    fun setSizeBrush(newSize: Float) {
        mBrushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, newSize, resources.displayMetrics)
        mDrawPaint!!.strokeWidth = mBrushSize
    }

    // Sets the drawing color.
    fun setColor(newColor: String) {
        color = Color.parseColor(newColor)
        mDrawPaint!!.color = color
    }

    // CustomPath is an inner class extending Path to include color and brush thickness.
    internal inner class CustomPath(var color: Int, var brushThickness: Float) : Path() {

    }
}