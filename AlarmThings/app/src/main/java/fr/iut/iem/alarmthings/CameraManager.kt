package fr.iut.iem.alarmthings

import android.content.Context
import android.content.Context.CAMERA_SERVICE
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.util.Log
import java.util.*


class CameraManager {
    private val TAG = CameraManager::class.java!!.simpleName

    private val IMAGE_WIDTH = 320
    private val IMAGE_HEIGHT = 240
    private val MAX_IMAGES = 1

    private var cameraDevice: CameraDevice? = null

    private var captureSession: CameraCaptureSession? = null

    /**
     * An [ImageReader] that handles still image capture.
     */
    private var imageReader: ImageReader? = null

    private object Holder { val INSTANCE = CameraManager() }

    companion object {
        val instance: CameraManager by lazy { Holder.INSTANCE }
    }

    private constructor()
    /**
     * Initialize the camera device
     */
    fun initializeCamera(context: Context,
                         backgroundHandler: Handler,
                         imageAvailableListener: ImageReader.OnImageAvailableListener) {
        // Discover the camera instance
        val manager = context.getSystemService(CAMERA_SERVICE) as android.hardware.camera2.CameraManager
        var camIds = arrayOf<String>()
        try {
            camIds = manager.cameraIdList
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Cam access exception getting IDs", e)
        }

        if (camIds.isEmpty()) {
            Log.e(TAG, "No cameras found")
            return
        }
        val id = camIds[0]
        Log.d(TAG, "Using camera id $id")

        // Initialize the image processor
        imageReader = ImageReader.newInstance(IMAGE_WIDTH, IMAGE_HEIGHT,
                ImageFormat.JPEG, MAX_IMAGES)
        imageReader!!.setOnImageAvailableListener(
                imageAvailableListener, backgroundHandler)

        // Open the camera resource
        try {
            manager.openCamera(id, stateCallback, backgroundHandler)
        } catch (cae: CameraAccessException) {
            Log.d(TAG, "Camera access exception", cae)
        }

    }

    /**
     * Callback handling device state changes
     */
    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(cameraDevice: CameraDevice) {
            Log.d(TAG, "Opened camera.")
            this@CameraManager.cameraDevice = cameraDevice
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            Log.d(TAG, "Camera disconnected, closing.")
            cameraDevice.close()
        }

        override fun onError(cameraDevice: CameraDevice, i: Int) {
            Log.d(TAG, "Camera device error, closing.")
            cameraDevice.close()
        }

        override fun onClosed(cameraDevice: CameraDevice) {
            Log.d(TAG, "Closed camera, releasing")
            this@CameraManager.cameraDevice = null
        }
    }

    /**
     * Begin a still image capture
     */
    fun takePicture() {
        if (cameraDevice == null) {
            Log.e(TAG, "Cannot capture image. Camera not initialized.")
            return
        }

        // Here, we create a CameraCaptureSession for capturing still images.
        try {
            cameraDevice!!.createCaptureSession(
                    Collections.singletonList(imageReader!!.surface),
                    sessionCallback, null)
        } catch (cae: CameraAccessException) {
            Log.e(TAG, "access exception while preparing pic", cae)
        }

    }

    /**
     * Callback handling session state changes
     */
    private val sessionCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
            // The camera is already closed
            if (cameraDevice == null) {
                return
            }

            // When the session is ready, we start capture.
            captureSession = cameraCaptureSession
            triggerImageCapture()
        }

        override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
            Log.e(TAG, "Failed to configure camera")
        }
    }

    /**
     * Execute a new capture request within the active session
     */
    private fun triggerImageCapture() {
        try {
            val captureBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(imageReader!!.surface)
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            Log.d(TAG, "Session initialized.")
            captureSession!!.capture(captureBuilder.build(), captureCallback, null)
        } catch (cae: CameraAccessException) {
            Log.e(TAG, "camera capture exception", cae)
        }
    }

    /**
     * Callback handling capture session events
     */
    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {

        override fun onCaptureProgressed(session: CameraCaptureSession,
                                         request: CaptureRequest,
                                         partialResult: CaptureResult) {
            Log.d(TAG, "Partial result")
        }

        override fun onCaptureCompleted(session: CameraCaptureSession,
                                        request: CaptureRequest,
                                        result: TotalCaptureResult) {
            if (session != null) {
                session.close()
                captureSession = null
                Log.d(TAG, "CaptureSession closed")
            }
        }
    }


    /**
     * Close the camera resources
     */
    fun shutDown() {
        if (cameraDevice != null) {
            cameraDevice!!.close()
        }
    }

    /**
     * Helpful debugging method:  Dump all supported camera formats to log.  You don't need to run
     * this for normal operation, but it's very helpful when porting this code to different
     * hardware.
     */
    fun dumpFormatInfo(context: Context) {
        val manager = context.getSystemService(CAMERA_SERVICE) as android.hardware.camera2.CameraManager
        var camIds = arrayOf<String>()
        try {
            camIds = manager.cameraIdList
        } catch (e: CameraAccessException) {
            Log.d(TAG, "Cam access exception getting IDs")
        }

        if (camIds.isEmpty()) {
            Log.d(TAG, "No cameras found")
        }
        val id = camIds[0]
        Log.d(TAG, "Using camera id $id")
        try {
            val characteristics = manager.getCameraCharacteristics(id)
            val configs = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            for (format in configs!!.outputFormats) {
                Log.d(TAG, "Getting sizes for format: $format")
                for (s in configs!!.getOutputSizes(format)) {
                    Log.d(TAG, "\t" + s.toString())
                }
            }
            val effects = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS)
            for (effect in effects!!) {
                Log.d(TAG, "Effect available: $effect")
            }
        } catch (e: CameraAccessException) {
            Log.d(TAG, "Cam access exception getting characteristics.")
        }

    }
}
