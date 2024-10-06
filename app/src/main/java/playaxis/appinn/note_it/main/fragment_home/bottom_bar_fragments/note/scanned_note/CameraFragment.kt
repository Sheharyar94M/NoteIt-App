package playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.note.scanned_note

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Context.CAMERA_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Point
import android.hardware.camera2.CameraManager
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Display
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatImageView
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.launch
import playaxis.appinn.note_it.R
import playaxis.appinn.note_it.databinding.FragmentCameraBinding
import playaxis.appinn.note_it.extensions.navigateSafe
import playaxis.appinn.note_it.main.fragment_home.HomeViewModel
import playaxis.appinn.note_it.main.viewModels.MainViewModel
import playaxis.appinn.note_it.utils.MainUtils
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


const val KEY_EVENT_EXTRA = "key_event_extra"
const val KEY_EVENT_ACTION = "key_event_action"

class CameraFragment : Fragment() {

    private lateinit var binding: FragmentCameraBinding

    private var displayId: Int = -1
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private lateinit var windowManager: WindowManager
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cameraManager: CameraManager
    private var cameraId: String? = null
    private lateinit var broadcastManager: LocalBroadcastManager
    private var preview: Preview? = null

    private val viewModelHome: HomeViewModel by activityViewModels()
    private val viewModelMain: MainViewModel by activityViewModels()
    private var REQUEST_CODE_STORAGE = 3

    private var torchOn = false
    private val args: CameraFragmentArgs by navArgs()

    private val displayManager by lazy {
        requireContext().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }

    // Initialize the ActivityResultLauncher in your fragment
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Get the URI of the selected image
                val data = result.data
                if (data != null) {

                    if(args.scanImage){

                        //need to set the clicked image to the viewModel so that image could be scanned
                        viewModelHome.scanImage = data.data.toString()
                        findNavController().navigateSafe(CameraFragmentDirections.actionCameraFragmentToScanFragment())
                    }
                    else {

                        viewModelHome.imageNote.add(data.data.toString())
                        Log.d(TAG, "Photo capture succeeded: ${viewModelHome.imageNote}")
                        //moving back
                        findNavController().popBackStack()
                    }
                }
                else {
                    viewModelHome.imageNote.clear()
                }
            }
        }

    /** Volume down button receiver used to trigger shutter */
    private val volumeDownReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getIntExtra(KEY_EVENT_EXTRA, KeyEvent.KEYCODE_UNKNOWN)) {
                // When the volume down button is pressed, simulate a shutter button click
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    binding.cameraButton.simulateClick()
                }
            }
        }
    }
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = view?.let { view ->

            if (displayId == this@CameraFragment.displayId) {
                Log.d(TAG, "Rotation changed: ${view.display.rotation}")
                imageCapture?.targetRotation = view.display.rotation
            }
        } ?: Unit
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        binding = FragmentCameraBinding.inflate(inflater, container, false)

        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        broadcastManager = LocalBroadcastManager.getInstance(requireContext())

        // Set up the intent filter that will receive events from our main activity
        val filter = IntentFilter().apply { addAction(KEY_EVENT_ACTION) }
        broadcastManager.registerReceiver(volumeDownReceiver, filter)

        // Every time the orientation of device changes, update rotation for use cases
        displayManager.registerDisplayListener(displayListener, null)

        // Initialize WindowManager to retrieve display metrics
        windowManager = requireActivity().windowManager

        // Initialize CameraManager
        cameraManager = requireActivity().getSystemService(CAMERA_SERVICE) as CameraManager

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Wait for the views to be properly laid out
        binding.viewFinder.post {

            // Keep track of the display in which this view is attached
            displayId = binding.viewFinder.display.displayId

            // Build UI controls
            updateCameraUi()

            // Set up the camera and its use cases
            lifecycleScope.launch {
                setUpCamera()
            }
        }

        binding.cameraGalleryButton.setOnClickListener {

            //take runtime image collection permission and add the image to the parent layout background
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.READ_MEDIA_IMAGES
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    //opening the gallery
                    openGallery()
                } else {
                    // Request the permissions
                    ActivityCompat.requestPermissions(
                        requireActivity(),
                        arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                        REQUEST_CODE_STORAGE
                    )
                }
            } else {

                if (MainUtils.hasStoragePermission(requireContext())) {
                    //opening the gallery
                    openGallery()
                } else {
                    //requesting the Gallery permission
                    MainUtils.getStoragePermission(requireActivity())
                }
            }
        }

        binding.cameraTorchButton.setOnClickListener {
            //toggle torch
            toggleTorch()
        }
    }

    /** Initialize CameraX, and prepare to bind the camera use cases  */
    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener(
            {
                // The provider is now ready and can be used
                cameraProvider = cameraProviderFuture.get()

                // Select lensFacing depending on the available cameras
                lensFacing = when {
                    hasBackCamera() -> CameraSelector.LENS_FACING_BACK
                    hasFrontCamera() -> CameraSelector.LENS_FACING_FRONT
                    else -> throw IllegalStateException("Back and front camera are unavailable")
                }

                // Build and bind the camera use cases
                bindCameraUseCases()
            },
            ContextCompat.getMainExecutor(requireContext())
        )
    }

    /** Declare and bind preview, capture and analysis use cases */
    private fun bindCameraUseCases() {

        // Get screen metrics used to setup camera for full screen resolution
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            // Call getCurrentWindowMetrics() method
            val metrics = windowManager.currentWindowMetrics.bounds
            Log.d(TAG, "Screen metrics: ${metrics.width()} x ${metrics.height()}")

            val screenAspectRatio = aspectRatio(metrics.width(), metrics.height())
            Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")

            val rotation = binding.viewFinder.display.rotation

            // CameraProvider
            val cameraProvider =
                cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // Preview
            preview = Preview.Builder()
                // We request aspect ratio but no resolution
                .setTargetAspectRatio(screenAspectRatio)
                // Set initial target rotation
                .setTargetRotation(rotation)
                .build()

            // ImageCapture
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                // We request aspect ratio but no resolution to match preview config, but letting
                // CameraX optimize for whatever specific resolution best fits our use cases
                .setTargetAspectRatio(screenAspectRatio)
                // Set initial target rotation, we will have to call this again if rotation changes
                // during the lifecycle of this use case
                .setTargetRotation(rotation)
                .build()

            // Must unbind the use-cases before rebinding them
            cameraProvider.unbindAll()

            if (camera != null) {
                // Must remove observers from the previous camera instance
                removeCameraStateObservers(camera!!.cameraInfo)
            }

            try {
                // A variable number of use-cases can be passed here -
                // camera provides access to CameraControl & CameraInfo
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

                // Attach the viewfinder's surface provider to preview use case
                preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        } else {

            // Handle the case for older Android versions
            val display: Display = requireActivity().windowManager.defaultDisplay
            val size = Point()
            display.getSize(size)
            val width: Int = size.x
            val height: Int = size.y

            Log.d(TAG, "Screen metrics: $width x $height")

            val screenAspectRatio = aspectRatio(width, height)
            Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")

            val rotation = binding.viewFinder.display.rotation

            // CameraProvider
            val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // Preview
            preview = Preview.Builder()
                // We request aspect ratio but no resolution
                .setTargetAspectRatio(screenAspectRatio)
                // Set initial target rotation
                .setTargetRotation(rotation)
                .build()

            // ImageCapture
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                // We request aspect ratio but no resolution to match preview config, but letting
                // CameraX optimize for whatever specific resolution best fits our use cases
                .setTargetAspectRatio(screenAspectRatio)
                // Set initial target rotation, we will have to call this again if rotation changes
                // during the lifecycle of this use case
                .setTargetRotation(rotation)
                .build()

            // Must unbind the use-cases before rebinding them
            cameraProvider.unbindAll()

            if (camera != null) {
                // Must remove observers from the previous camera instance
                removeCameraStateObservers(camera!!.cameraInfo)
            }

            try {
                // A variable number of use-cases can be passed here -
                // camera provides access to CameraControl & CameraInfo
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

                // Attach the viewfinder's surface provider to preview use case
                preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }
    }

    private fun removeCameraStateObservers(cameraInfo: CameraInfo) {
        cameraInfo.cameraState.removeObservers(viewLifecycleOwner)
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    /** Method used to re-draw the camera UI controls, called every time configuration changes. */
    private fun updateCameraUi() {

        // Remove previous UI if any
        binding.root.let {
            binding.root.removeView(it)
        }

        // Listener for button used to capture photo
        binding.cameraButton.setOnClickListener {

            // Get a stable reference of the modifiable image capture use case
            imageCapture?.let { imageCapture ->

                // Create time stamped name and MediaStore entry.
                val name = SimpleDateFormat(FILENAME, Locale.US)
                    .format(System.currentTimeMillis())

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, PHOTO_TYPE)
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                        val appName = requireContext().resources.getString(R.string.app_name)
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/${appName}")
                    }
                }

                // Create output options object which contains file + metadata
                val outputOptions = ImageCapture.OutputFileOptions
                    .Builder(
                        requireContext().contentResolver,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    )
                    .build()

                // Setup image capture listener which is triggered after photo has been taken
                imageCapture.takePicture(
                    outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
                        override fun onError(exc: ImageCaptureException) {
                            Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                        }

                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {

                            requireActivity().runOnUiThread {

                                //to next screen
                                if(args.scanImage){

                                    //need to set the clicked image to the viewModel so that image could be scanned
                                    viewModelHome.scanImage = output.savedUri.toString()
                                    findNavController().navigateSafe(CameraFragmentDirections.actionCameraFragmentToScanFragment())
                                }
                                else {

                                    val host = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_main_fragment_container) as NavHostFragment
                                    val navController = host.navController
                                    navController.setGraph(R.navigation.main_navigation)

                                    val previousFragId = navController.previousBackStackEntry!!.destination.id
                                    val previousFragment = navController.graph.findNode(previousFragId)

                                    viewModelHome.imageNote.add(output.savedUri.toString())
                                    Log.d(TAG, "Photo capture succeeded: ${viewModelHome.imageNote}")

                                    //fire the event to handle the view
                                    viewModelMain.bottomBarItemsListenerEvent("add_note")

                                    if (previousFragment!!.label == "fragment_home"){

                                        //creating note for it
                                        viewModelHome.createNote()
                                        //navigate to AddEditFragment
                                        navController.navigateSafe(CameraFragmentDirections.actionCameraFragmentToAddEditFragment())
                                    }
                                    else{
                                        //moving back
                                        findNavController().popBackStack()
                                    }
                                }
                            }
                        }
                    })
            }
        }
    }

    /** Returns true if the device has an available back camera. False otherwise */
    private fun hasBackCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }

    /** Returns true if the device has an available front camera. False otherwise */
    private fun hasFrontCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
    }

    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(galleryIntent)
    }

    private fun toggleTorch() {

        if (torchOn) {

            torchOn = false
            camera!!.cameraControl.enableTorch(false)
            binding.cameraTorchButton.setImageDrawable(ContextCompat.getDrawable(requireContext(),R.drawable.torch_off_icon))
        }
        else{

            torchOn = true
            binding.cameraTorchButton.setImageDrawable(ContextCompat.getDrawable(requireContext(),R.drawable.torch_on_icon))
            camera!!.cameraControl.enableTorch(true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Shut down our background executor
        cameraExecutor.shutdown()

        // Unregister the broadcast receivers and listeners
        broadcastManager.unregisterReceiver(volumeDownReceiver)
        displayManager.unregisterDisplayListener(displayListener)
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_TYPE = "image/jpeg"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }
}

/** Milliseconds used for UI animations */
const val ANIMATION_FAST_MILLIS = 50L
private fun AppCompatImageView.simulateClick(delay: Long = ANIMATION_FAST_MILLIS) {
    performClick()
    isPressed = true
    invalidate()
    postDelayed({
        invalidate()
        isPressed = false
    }, delay)
}