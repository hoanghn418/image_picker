package dev.hoanghn.image_picker

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.core.app.ActivityCompat
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry
import java.io.ByteArrayOutputStream


/**
 * Created by hoanghn on 4/9/22.
 */
class ImagePickerDelegate(
    private val activity: Activity
) : PluginRegistry.RequestPermissionsResultListener {
    private val permissionManager: PermissionManager by lazy {
        object : PermissionManager {
            override fun isPermissionGranted(permissionName: String): Boolean =
                (ActivityCompat.checkSelfPermission(activity, permissionName)
                        == PackageManager.PERMISSION_GRANTED)

            override fun askForPermission(permissionName: String, requestCode: Int) =
                ActivityCompat.requestPermissions(activity, arrayOf(permissionName), requestCode)
        }
    }

    private val methodCalls: HashMap<Int, MethodCall> = hashMapOf()
    private val pendingResults: HashMap<Int, MethodChannel.Result> = hashMapOf()

    private fun setPendingMethodCallAndResult(
        key: Int, call: MethodCall, result: MethodChannel.Result
    ) {
        if (pendingResults[key] != null) return

        methodCalls[key] = call
        pendingResults[key] = result
    }

    private fun clearMethodCallAndResult(key: Int) {
        methodCalls.remove(key)
        pendingResults.remove(key)
    }

    fun getGalleryImageCount(methodCall: MethodCall, result: MethodChannel.Result) {
        if (!permissionManager.isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            permissionManager.askForPermission(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                REQUEST_CODE_GET_GALLERY_IMAGE_COUNT
            )
            setPendingMethodCallAndResult(REQUEST_CODE_GET_GALLERY_IMAGE_COUNT, methodCall, result)
            return
        }

        result.success(countForGalleryImage())
    }

    fun getGalleryItem(methodCall: MethodCall, result: MethodChannel.Result) {
        if (!permissionManager.isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            permissionManager.askForPermission(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                REQUEST_CODE_GET_GALLERY_ITEM
            )
            setPendingMethodCallAndResult(REQUEST_CODE_GET_GALLERY_ITEM, methodCall, result)
            return
        }

        startGetGalleryItem(methodCall, result)
    }

    private fun startGetGalleryItem(
        call: MethodCall,
        result: MethodChannel.Result
    ) {
        val index = (call.arguments as? Int) ?: 0
        dataForGalleryItem(index) { data, id, created, location ->
            result.success(
                mapOf<String, Any>(
                    "data" to data,
                    "id" to id,
                    "created" to created,
                    "location" to location,
                )
            )
        }
    }

    private fun dataForGalleryItem(
        index: Int, completion: (ByteArray, String, Int, String)
        -> Unit
    ) {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val orderBy = MediaStore.Images.Media.DATE_TAKEN
        val cursor = activity.contentResolver.query(uri, columns, null, null, "$orderBy DESC")
        cursor?.apply {
            moveToPosition(index)

            val idIndex = getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val createdIndex = getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val latitudeIndex = getColumnIndexOrThrow(MediaStore.Images.Media.LATITUDE)
            val longitudeIndex = getColumnIndexOrThrow(MediaStore.Images.Media.LONGITUDE)

            val id = getString(idIndex)
            val created = getInt(createdIndex)
            val latitude = getDouble(latitudeIndex)
            val longitude = getDouble(longitudeIndex)

            val imageUri = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                id.toLong()
            )
            try {
                val bmp = if (Build.VERSION.SDK_INT < 28) {
                    MediaStore.Images.Media.getBitmap(activity.contentResolver, imageUri)
                } else {
                    val source: ImageDecoder.Source =
                        ImageDecoder.createSource(activity.contentResolver, imageUri)
                    ImageDecoder.decodeBitmap(source)
                }

                val stream = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                val data = stream.toByteArray()

                completion(data, id, created, "$latitude, $longitude")
            } catch (e: Exception) {
                e.printStackTrace()
            }

            close()
        }
    }

    private val columns = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DATE_ADDED,
        MediaStore.Images.Media.LATITUDE,
        MediaStore.Images.Media.LONGITUDE
    )

    private fun countForGalleryImage(): Int {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val cursor = activity.contentResolver.query(uri, columns, null, null, null);
        val count = cursor?.count ?: 0
        cursor?.close()
        return count
    }

    private fun onRequestGetGalleryImageCount(permissionGranted: Boolean) {
        val result = pendingResults[REQUEST_CODE_GET_GALLERY_IMAGE_COUNT] ?: return

        if (permissionGranted) {
            result.success(countForGalleryImage())
        } else {
            result.error(
                "storage_access_denied",
                "The user did not allow storage access.",
                null,
            )
        }
        clearMethodCallAndResult(REQUEST_CODE_GET_GALLERY_IMAGE_COUNT)
    }

    private fun onRequestGetGalleryItem(permissionGranted: Boolean) {
        val call = methodCalls[REQUEST_CODE_GET_GALLERY_ITEM] ?: return
        val result = pendingResults[REQUEST_CODE_GET_GALLERY_ITEM] ?: return

        if (permissionGranted) {
            startGetGalleryItem(call, result)
        } else {
            result.error(
                "storage_access_denied",
                "The user did not allow storage access.",
                null,
            )
        }
        clearMethodCallAndResult(REQUEST_CODE_GET_GALLERY_ITEM)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {
        when (requestCode) {
            REQUEST_CODE_GET_GALLERY_IMAGE_COUNT -> {
                val permissionGranted =
                    grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
                onRequestGetGalleryImageCount(permissionGranted)
            }
            REQUEST_CODE_GET_GALLERY_ITEM -> {
                val permissionGranted =
                    grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
                onRequestGetGalleryItem(permissionGranted)
            }
            else -> return false
        }

        return true
    }

    companion object {
        private const val REQUEST_CODE_GET_GALLERY_IMAGE_COUNT = 1000
        private const val REQUEST_CODE_GET_GALLERY_ITEM = 1001
    }
}

interface PermissionManager {
    fun isPermissionGranted(permissionName: String): Boolean
    fun askForPermission(permissionName: String, requestCode: Int)
}
