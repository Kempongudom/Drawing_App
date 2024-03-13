package com.example.drawingapp

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

// Main activity of the app, which manages UI and user interactions
class MainActivity : AppCompatActivity() {

    // Fields to hold references to the drawing view and the currently selected paint color
    private var drawingView: DrawingView? = null
    private var mImageButtonCurrentPaint: ImageButton? = null

    // Launcher for picking an image from the gallery
    val openGalleyLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // If the result is OK and there's data, set the image as the background
            if (result.resultCode == RESULT_OK && result.data != null) {
                val ivBackgroundImage: ImageView = findViewById(R.id.iv_background)
                ivBackgroundImage.setImageURI(result.data?.data)
            }
        }

    // Launcher for requesting permissions
    val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permission ->
            permission.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value

                if (isGranted) {
                    // Permission granted, allow selecting an image from the gallery
                    Toast.makeText(this, "Permission granted now you can read storage files", Toast.LENGTH_LONG).show()
                    val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleyLauncher.launch(pickIntent)
                } else {
                    // Permission denied, show a denial message
                    if (permissionName == android.Manifest.permission.READ_MEDIA_IMAGES) {
                        Toast.makeText(this, "Permission to read storage files denied", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize the drawing view and set default brush size
        drawingView = findViewById(R.id.drawing_view)
        drawingView?.setSizeBrush(20.toFloat())

        // Set the initially selected paint color
        val linearLayoutPaintColors: LinearLayout = findViewById(R.id.ll_paint_colors)
        mImageButtonCurrentPaint = linearLayoutPaintColors[4] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_pressed))

        // Set up brush size chooser dialog
        val ib_brush: ImageButton = findViewById(R.id.ib_brush)
        ib_brush.setOnClickListener {
            showBrushChooserDialog()
        }

        // Request storage permission and pick image from gallery
        val ibGalley: ImageButton = findViewById(R.id.ib_galley)
        ibGalley.setOnClickListener {
            requestStoragePermission()
        }

        // Undo the last stroke
        val ib_undo: ImageButton = findViewById(R.id.ib_undo)
        ib_undo.setOnClickListener {
            drawingView?.onClickUndo()
        }

        // Save the current drawing
        val ib_save: ImageButton = findViewById(R.id.ib_save)
        ib_save.setOnClickListener {
            if (isReadStorageAllowed()) {
                lifecycleScope.launch {
                    val flDrawingView: FrameLayout = findViewById(R.id.fl_drawing_view_container)
                    saveBitmapFile(getBitmapFromView(flDrawingView))
                }
            }
        }
    }

    // Shows a dialog allowing the user to choose the brush size
    private fun showBrushChooserDialog() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush Size:")
        val smallBtn: ImageButton = brushDialog.findViewById(R.id.ib_small_brush)
        smallBtn.setOnClickListener {
            drawingView?.setSizeBrush(10.toFloat())
            brushDialog.dismiss()
        }

        val mediumBtn: ImageButton = brushDialog.findViewById(R.id.ib_medium_brush)
        mediumBtn.setOnClickListener {
            drawingView?.setSizeBrush(20.toFloat())
            brushDialog.dismiss()
        }

        val bigBtn: ImageButton = brushDialog.findViewById(R.id.ib_big_brush)
        bigBtn.setOnClickListener {
            drawingView?.setSizeBrush(30.toFloat())
            brushDialog.dismiss()
        }

        brushDialog.show()
    }

    // Change the paint color based on the selected color
    fun paintClicked(view: View) {
        if (view !== mImageButtonCurrentPaint) {
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView?.setColor(colorTag)

            imageButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_pressed))
            mImageButtonCurrentPaint!!.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_normal))
            mImageButtonCurrentPaint = view
        }
    }

    // Shows a dialog with a message explaining why the permission is needed
    private fun showRationalDialog(title: String, message: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title).setMessage(message).setPositiveButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    // Checks if the read storage permission has been granted
    private fun isReadStorageAllowed(): Boolean {
        val result = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES)
        return result == PackageManager.PERMISSION_GRANTED
    }

    // Requests the read storage permission
    private fun requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_MEDIA_IMAGES)) {
            showRationalDialog("Drawing App", "Drawing app needs to access your storage")
        } else {
            requestPermission.launch(arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES, android.Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
    }

    // Creates a bitmap from the given view
    private fun getBitmapFromView(view: View): Bitmap {
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return returnedBitmap
    }

    // Saves the bitmap to a file
    private suspend fun saveBitmapFile(mBitmap: Bitmap): String {
        var result = ""
        withContext(Dispatchers.IO) {
            if (mBitmap != null) {
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
                    val f = File(externalCacheDir?.absoluteFile.toString() + File.separator + "DrawingApp" + System.currentTimeMillis() / 1000 + ".png")
                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()
                    result = f.absolutePath
                    runOnUiThread {
                        if (result.isNotEmpty()) {
                            Toast.makeText(this@MainActivity, "File saved successfully: $result", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@MainActivity, "Something went wrong while saving the file", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }
}
