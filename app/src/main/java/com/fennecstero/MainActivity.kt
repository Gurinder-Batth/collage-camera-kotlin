package com.fennecstero

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.util.Log
import android.util.Rational
import android.util.Size
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private val REQUEST_GALLERY: Int = 300
    private val REQUEST_STORAGE_PERMISSION_CODE: Int = 203
    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var viewFinder: PreviewView
    private lateinit var viewFinder2: PreviewView
    private lateinit var uriImage1: Uri
    private lateinit var uriImage2: Uri

    private var imageOneCaptured = false
    private var imageTwoCaptured = false
    private lateinit var soundPool: SoundPool
    private var shutterSoundId: Int = 0

    private var backCameraOpen: Int = 0

    private var zoomRatio = 2f // Initial zoom ratio

    private lateinit var whatClickId: TextView
    private lateinit var linearLayoutCamera: LinearLayout
    private lateinit var previewImageLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewFinder = findViewById(R.id.viewFinder);
        linearLayoutCamera= findViewById(R.id.linear_layout_camera)
        previewImageLayout = findViewById(R.id.previewImageLayout)


        // hide the action bar
        supportActionBar?.hide()

        flashToggle(true)
        borderToggle(true)
        roateVerticalToggle(true)

        window.statusBarColor = resources.getColor(android.R.color.black)
        // Check if the app has permission to read and write external storage

        whatClickId = findViewById(R.id.whatClickId)

        // Check camera permissions if all permission granted
        // start camera else ask for the permission
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        // set on click listener for the button of capture photo
        // it calls a method which is implemented below
        findViewById<ImageView>(R.id.camera_capture_button).setOnClickListener {
            // Vibrate for 50 milliseconds
            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    // For API levels below 26
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }
            }
            takePhoto()
        }

        // Retrieve the URI from the intent
        val uri: Uri? = intent.data
        // Check if the URI is not null
        if (uri != null) {
            // Do something with the URI, such as displaying it
            Log.d("MainActivity", "Received URI: $uri")
            uriImage1 = uri
            imageOneCaptured = true
            // set the saved uri to the image view
            // set the saved uri to the image view
            findViewById<ImageView>(R.id.iv_capture).visibility = View.VISIBLE
            findViewById<ImageView>(R.id.iv_capture).setImageURI(uri)
            findViewById<ImageView>(R.id.iv_capture2).visibility = View.GONE
        }

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Initialize the SoundPool
        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()

        // Load the shutter sound from the raw resource
        shutterSoundId = soundPool.load(this, R.raw.camera_13695, 1)


        linearLayoutCamera.visibility = View.VISIBLE
        previewImageLayout.visibility = View.GONE

        gridStateUpdate()
    }

    public fun gridStateUpdate() {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        var isGridOn = sharedPreferences.getBoolean("grid", false)
        if (isGridOn) {
            findViewById<com.fennecstero.GridOverlayView>(R.id.gridOverlay).visibility = View.VISIBLE;
        }
        else {
            findViewById<com.fennecstero.GridOverlayView>(R.id.gridOverlay).visibility = View.GONE;
        }
    }



    private fun takePhoto() {
        // Get a stable reference of the
        // modifiable image capture use case
        val imageCapture = imageCapture ?: return

        val tmpDir = File(this.cacheDir, "tmp")
        if (!tmpDir.exists()) {
            tmpDir.mkdirs()
        }


        // Create time-stamped output file to hold the image
        val photoFile = File(
            tmpDir,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()



        // Initialize SharedPreferences
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        var isFlashEnabled = sharedPreferences.getBoolean("flashEnabled", true)

        var isshutterSound = sharedPreferences.getBoolean("shutterSound", false)


        // Configure flash mode (assuming you have a toggle or setting for flash)
        val flashMode = if (isFlashEnabled) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
        imageCapture.flashMode = flashMode
        // Set up image capture listener,
        // which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {

                    if (isshutterSound) {
                        playShutterSound()
                    }

                    val savedUri = Uri.fromFile(photoFile)





                    if (imageOneCaptured == false && imageTwoCaptured == false) {
                        imageOneCaptured = true
                         uriImage1 = savedUri
                        // set the saved uri to the image view
                        findViewById<ImageView>(R.id.iv_capture).visibility = View.VISIBLE
                        findViewById<ImageView>(R.id.iv_capture).setImageURI(savedUri)
                        findViewById<ImageView>(R.id.iv_capture2).visibility = View.GONE
                        whatClickId.text = "RIGHT CLICK"
                        linearLayoutCamera.visibility = View.VISIBLE
                        previewImageLayout.visibility = View.GONE
                    }
                    else {
                            // set the saved uri to the image view
                            findViewById<ImageView>(R.id.iv_capture).visibility = View.VISIBLE
                            findViewById<ImageView>(R.id.iv_capture2).setImageURI(savedUri)
                            findViewById<ImageView>(R.id.iv_capture2).visibility = View.VISIBLE
                            viewFinder.visibility = View.GONE
                            uriImage2 = savedUri
                            whatClickId.text = "PREVIEW"
                            linearLayoutCamera.visibility = View.GONE
                            previewImageLayout.visibility = View.VISIBLE
                    }
                    // iv_capture2

                    val msg = "Photo capture succeeded: $savedUri"
//                    Toast.makeText(baseContext, msg, Toast.LENGTH_LONG).show()
                    Log.d(TAG, msg)
//                    startCamera2()
                }
            })
    }

    public fun cameraSwitch(view: View) {
        if (backCameraOpen == 0) {
            backCameraOpen = 1;
        }
        else {
            backCameraOpen = 0;
        }
        startCamera();
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            // Define your desired aspect ratio (4:3)
            val aspectRatio = Rational(4, 3)
            val resolution = Size(
                2048,  // Set width to a fixed value (e.g., 2048)
                1536   // Set height according to 4:3 aspect ratio
            )

            imageCapture = ImageCapture.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)  // Ensure the aspect ratio is 4:3
//                .setTargetResolution(resolution)\
//                .setTargetAspectRatio(aspectRatio)
                .build()

            // Select back camera as a default
            val cameraSelector = if (backCameraOpen == 0) {
                CameraSelector.DEFAULT_BACK_CAMERA
            } else {
                CameraSelector.DEFAULT_FRONT_CAMERA
            }

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Log.e(MainActivity.TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startCamera2() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {

            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder2.getSurfaceProvider())
                }

            imageCapture = ImageCapture.Builder()
                .setTargetResolution(Size(250, 625)) // Set your desired low resolution
                .build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
               var camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

                // Example of how to zoom in by a factor of 2
//                val cameraControl = camera.cameraControl
//                cameraControl.setZoomRatio(3f)

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    // creates a folder inside internal storage
    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    // checks the camera permission
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            // If all permissions granted , then start Camera
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                // If permissions are not granted,
                // present a toast to notify the user that
                // the permissions were not granted.
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    companion object {
        private const val TAG = "CameraXGFG"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 20
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    fun savePhotoCollage(view: View) {
        try {
            val progressDialog = ProgressDialog(this)
            progressDialog.setMessage("Saving image to gallery...")
            progressDialog.setCancelable(false)
            progressDialog.show()

            val imageHelper = ImageManipulationHelper(this)
            val collageMaker = CollageMaker(this)

            val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            var isseparatePhotos = sharedPreferences.getBoolean("separatePhotos", false)

            // Create and save the collage using the old image URIs
            Thread {
                val timestamp = System.currentTimeMillis()
                val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val formattedTimestamp = sdf.format(Date(timestamp))

                // Initialize SharedPreferences
                val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                var borderCheck = sharedPreferences.getBoolean("borderOn", false)

                var roateVertical = sharedPreferences.getBoolean("roateVertical", false)

                if (roateVertical) {
                    if (borderCheck) {
                        collageMaker.saveImagesIn45Canvas(uriImage1, uriImage2, "${formattedTimestamp}_collage.png");
                    }
                    collageMaker.createAndSaveCollage(uriImage1, uriImage2, "${formattedTimestamp}_collage.png", borderCheck, true)
                }
                else {
                    if (borderCheck) {
                        collageMaker.saveImagesIn45Canvas(uriImage1, uriImage2, "${formattedTimestamp}_collage.png");
                    }
                    else {
                        collageMaker.createAndSaveCollage(uriImage1, uriImage2, "${formattedTimestamp}_collage.png", borderCheck, false)
                    }
                }

                if (isseparatePhotos) {
                    // Save image1 and image2 separately
                    saveImageToGallery(uriImage1, "${formattedTimestamp}_image1.png")
                    saveImageToGallery(uriImage2, "${formattedTimestamp}_image2.png")
                }


                runOnUiThread {
                    progressDialog.dismiss()

                    if (!isseparatePhotos) {
//                        deleteFileFromUri(contentResolver, uriImage1)
//                        deleteFileFromUri(contentResolver, uriImage2)
                    }
                    Toast.makeText(this@MainActivity, "Images saved to gallery", Toast.LENGTH_SHORT).show()


                }
            }.start()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
    }


    private fun saveImageToGallery(uri: Uri, fileName: String) {
        val resolver = contentResolver

        // Load the bitmap from the URI
        val bitmap = MediaStore.Images.Media.getBitmap(resolver, uri)

        // Resize and crop the bitmap to a 4:3 aspect ratio
        val croppedBitmap = cropToAspectRatio(bitmap, 4, 5)

        // Create content values for the image
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        // Insert the new image and get the URI
        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        imageUri?.let {
            val outputStream = resolver.openOutputStream(it)
            outputStream?.let { stream ->
                croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.close()
            }
        }
    }

    private fun cropToAspectRatio(bitmap: Bitmap, aspectX: Int, aspectY: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val newWidth: Int
        val newHeight: Int

        if (width * aspectY > height * aspectX) {
            // Crop width to maintain aspect ratio
            newWidth = height * aspectX / aspectY
            newHeight = height
        } else {
            // Crop height to maintain aspect ratio
            newWidth = width
            newHeight = width * aspectY / aspectX
        }

        val xOffset = (width - newWidth) / 2
        val yOffset = (height - newHeight) / 2

        return Bitmap.createBitmap(bitmap, xOffset, yOffset, newWidth, newHeight)
    }


    private fun deleteImage() {
        val contentResolver = contentResolver
        val selection = MediaStore.Images.Media._ID + "=?"
        val selectionArgs = arrayOf(ContentUris.parseId(uriImage1).toString())
        contentResolver.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection, selectionArgs)

        val contentResolver1 = contentResolver
        val selection1 = MediaStore.Images.Media._ID + "=?"
        val selectionArgs1 = arrayOf(ContentUris.parseId(uriImage2).toString())
        contentResolver1.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection1, selectionArgs1)
    }

    public fun destoryAct(view: View) {
        if (::uriImage1.isInitialized) {
            deleteFileFromUri(contentResolver, uriImage1)
        }
        if (::uriImage2.isInitialized) {
            deleteFileFromUri(contentResolver, uriImage2)
        }

        val intent = Intent(this@MainActivity, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    public fun flashOn(view: View) {
        flashToggle(false)
    }

    public fun flashToggle(readOnly: Boolean) {
        // Initialize SharedPreferences
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        var isFlashEnabled = sharedPreferences.getBoolean("flashEnabled", true)
        // Save the toggle state to SharedPreferences
        if (!readOnly) {
            isFlashEnabled = !isFlashEnabled
            editor.putBoolean("flashEnabled", isFlashEnabled)
            editor.apply()
        }
        var flashTxt:String = "On";
        if (!isFlashEnabled) {
            flashTxt = "Off";
        }
        findViewById<TextView>(R.id.textViewFlash).setText("Flash " + flashTxt);
        if (isFlashEnabled) {
            findViewById<TextView>(R.id.textViewFlash).setTextColor(getResources().getColor(R.color.active));
            findViewById<ImageView>(R.id.flash_on).setImageDrawable(getResources().getDrawable(R.drawable.torch_svgrepo_com_active));
        }
        else {
            findViewById<TextView>(R.id.textViewFlash).setTextColor(getResources().getColor(R.color.white));
            findViewById<ImageView>(R.id.flash_on).setImageDrawable(getResources().getDrawable(R.drawable.torch_svgrepo_com));
        }

    }



    public fun borderOn(view: View) {
        borderToggle(false)
    }

    public fun roateVertical(view: View) {
        roateVerticalToggle(false)
    }

    public fun roateVerticalToggle(readOnly: Boolean) {
        // Initialize SharedPreferences
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        var roateVertical = sharedPreferences.getBoolean("roateVertical", false)
        // Save the toggle state to SharedPreferences
        if (!readOnly) {
            roateVertical = !roateVertical
            editor.putBoolean("roateVertical", roateVertical)
            editor.apply()
        }
        var flashTxt:String = "On";
        if (!roateVertical) {
            flashTxt = "Off";
        }
        findViewById<TextView>(R.id.borderOnTxt).setText("roateVertical " + flashTxt);

        if (roateVertical) {
            findViewById<TextView>(R.id.roateVerticalTxt).setText("Horizontal");
            findViewById<TextView>(R.id.roateVerticalTxt).setTextColor(getResources().getColor(R.color.active));
            findViewById<ImageView>(R.id.roateVertical).setImageDrawable(getResources().getDrawable(R.drawable.horizontal_svgrepo_com));
        }
        else {
            findViewById<TextView>(R.id.roateVerticalTxt).setText("Vertical");
            findViewById<TextView>(R.id.roateVerticalTxt).setTextColor(getResources().getColor(R.color.white));
            findViewById<ImageView>(R.id.roateVertical).setImageDrawable(getResources().getDrawable(R.drawable.vertical_svgrepo_com));

            borderToggle(true)
        }
    }

    public fun borderToggle(readOnly: Boolean) {
        // Initialize SharedPreferences
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        var isBorderOn = sharedPreferences.getBoolean("borderOn", false)
       // Save the toggle state to SharedPreferences
        if (!readOnly) {
            isBorderOn = !isBorderOn
            editor.putBoolean("borderOn", isBorderOn)
            editor.apply()
        }
        var flashTxt:String = "On";
         borderOnView(isBorderOn)
        if (!isBorderOn) {
            flashTxt = "Off";
        }
        findViewById<TextView>(R.id.borderOnTxt).setText("Border " + flashTxt);

        if (isBorderOn) {
            findViewById<TextView>(R.id.borderOnTxt).setTextColor(getResources().getColor(R.color.active));
            findViewById<ImageView>(R.id.border).setImageDrawable(getResources().getDrawable(R.drawable.border_radius_22_jan_active));
            findViewById<LinearLayout>(R.id.previewImageLayout).setBackgroundColor(getResources().getColor(R.color.white));
        }
        else {
            findViewById<TextView>(R.id.borderOnTxt).setTextColor(getResources().getColor(R.color.white));
            findViewById<ImageView>(R.id.border).setImageDrawable(getResources().getDrawable(R.drawable.border_radius_22_jan));
            findViewById<LinearLayout>(R.id.previewImageLayout).setBackgroundColor(getResources().getColor(R.color.black));
        }
    }

    public fun borderOnView(isBorderOn: Boolean) {

        // Find your LinearLayout and ImageView
        val linearLayout = findViewById<LinearLayout>(R.id.previewImageLayout)
        val imageView1 = findViewById<ImageView>(R.id.iv_capture)
        val imageView2 = findViewById<ImageView>(R.id.iv_capture2)

        // Convert dp to pixels
        val density = resources.displayMetrics.density
        var paddingInDp = 8
        if (!isBorderOn) {
            paddingInDp = 0
        }
        val paddingInPixels = (paddingInDp * density).toInt()

        var marginInDp = 2.5f
        if (!isBorderOn) {
            marginInDp = 0f
        }
        val marginInPixels = (marginInDp * density).toInt()

        // Set padding for LinearLayout
        linearLayout.setPadding(paddingInPixels, paddingInPixels, paddingInPixels, paddingInPixels)

        // Set margins for ImageView1
        val layoutParams1 = imageView1.layoutParams as LinearLayout.LayoutParams
        layoutParams1.setMargins(0, 0, marginInPixels, 0) // Right margin
        imageView1.layoutParams = layoutParams1

        // Set margins for ImageView2
        val layoutParams2 = imageView2.layoutParams as LinearLayout.LayoutParams
        layoutParams2.setMargins(marginInPixels, 0, 0, 0) // Left margin
        imageView2.layoutParams = layoutParams2    }


    // Define a function to delete a file from URI
    fun deleteFileFromUri(contentResolver: ContentResolver, fileUri: Uri) {
        if (fileUri == null) {
            return
        }
        try {
            // Check if the URI scheme is content://
            if(fileUri.scheme == "file") {
                val file = File(fileUri.path ?: "")
                if (file.exists()) {
                    if (file.delete()) {
                        Log.d("FileDeleted", "File deleted successfully")
                    } else {
                        Log.e("FileDeleteError", "Failed to delete file")
                    }
                } else {
                    Log.e("FileNotFoundError", "File does not exist")
                }
            } else {
                Log.e("InvalidURIScheme", "Expected file:// scheme")
            }
        } catch (e: Exception) {
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            // Handle any exceptions that may occur during deletion
            e.printStackTrace()
        }
    }

    fun openGallery(view: View) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.type = "image/*"
        startActivity(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_GALLERY && resultCode == Activity.RESULT_OK && data != null) {
            val selectedImageUri: Uri? = data.data
            // Now you can work with the selected image URI
        }
    }

    fun swap(view: View) {



        // Check if both URIs are initialized
        if (::uriImage1.isInitialized && ::uriImage2.isInitialized) {

            // Show the ProgressDialog
            // Show the ProgressDialog
            val progressDialog = ProgressDialog(this)
            progressDialog.setMessage("Loading...")
            progressDialog.setCancelable(false)
            progressDialog.show()

            // Dismiss the ProgressDialog after 1 second

            // Dismiss the ProgressDialog after 1 second
            Handler().postDelayed(Runnable {
                if (progressDialog.isShowing) {
                    progressDialog.dismiss()
                }
            }, 1000) // 1000 milliseconds = 1 second

            // Swap the URIs using a temporary variable
            val tempUri = uriImage1
            uriImage1 = uriImage2
            uriImage2 = tempUri

            findViewById<ImageView>(R.id.iv_capture).visibility = View.VISIBLE
            findViewById<ImageView>(R.id.iv_capture2).visibility = View.VISIBLE

            findViewById<ImageView>(R.id.iv_capture).setImageURI(uriImage1)
            findViewById<ImageView>(R.id.iv_capture2).setImageURI(uriImage2)
            // Now uriImage1 contains the original value of uriImage2 and vice versa
        } else {
            // Handle case where one or both URIs are not initialized
            Log.e("Swap", "URIs are not initialized")
        }

    }

    fun settings(view: View) {
        val intent = Intent(this@MainActivity, SettingsActivity::class.java)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()

        // Execute functions when activity gains focus
        flashToggle(true)
        borderToggle(true)
        gridStateUpdate()
    }

    private fun playShutterSound() {
        // Play the shutter sound
        soundPool.play(shutterSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
    }


}
