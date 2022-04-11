package dev.hoanghn.image_picker

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.core.app.ActivityCompat
import androidx.exifinterface.media.ExifInterface
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
            override fun isPermissionGranted(permissionNames: Array<String>): Boolean =
                permissionNames.all {
                    ActivityCompat.checkSelfPermission(
                        activity,
                        it
                    ) == PackageManager.PERMISSION_GRANTED
                }

            override fun askForPermission(permissionNames: Array<String>, requestCode: Int) =
                ActivityCompat.requestPermissions(activity, permissionNames, requestCode)
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
        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        if (!permissionManager.isPermissionGranted(permissions)) {
            permissionManager.askForPermission(
                permissions,
                REQUEST_CODE_GET_GALLERY_IMAGE_COUNT
            )
            setPendingMethodCallAndResult(REQUEST_CODE_GET_GALLERY_IMAGE_COUNT, methodCall, result)
            return
        }

        result.success(countForGalleryImage())
    }

    fun getGalleryItem(methodCall: MethodCall, result: MethodChannel.Result) {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_MEDIA_LOCATION
            )
        else
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        if (!permissionManager.isPermissionGranted(permissions)) {
            permissionManager.askForPermission(
                permissions,
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
        val contentResolver = activity.contentResolver
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"
        val cursor = contentResolver.query(uri, columns, null, null, sortOrder)
        cursor?.apply {
            moveToPosition(index)

            val idIndex = getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val createdIndex = getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            @Suppress("DEPRECATION")
            val latitudeIndex = getColumnIndexOrThrow(MediaStore.Images.Media.LATITUDE)

            @Suppress("DEPRECATION")
            val longitudeIndex = getColumnIndexOrThrow(MediaStore.Images.Media.LONGITUDE)

            val id = getString(idIndex)
            val created = getInt(createdIndex)

            var outputStream: ByteArrayOutputStream? = null
            try {
                val thumbnail: Bitmap
                val latLongArr = doubleArrayOf(0.0, 0.0)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val imageUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id.toLong()
                    )
                    val thumbSize = activity.dip2px(100f)
                    thumbnail = contentResolver.loadThumbnail(
                        imageUri,
                        Size(thumbSize, thumbSize),
                        null,
                    )

                    // must request ACCESS_MEDIA_LOCATION permission prior to call this function, else
                    // UnsupportedOperationException: Caller must hold ACCESS_MEDIA_LOCATION permission to access original
                    contentResolver.openInputStream(imageUri).use { stream ->
                        stream?.also {
                            ExifInterface(stream).run {
                                latLongArr[0] = latLong?.get(0) ?: 0.0
                                latLongArr[1] = latLong?.get(1) ?: 0.0
                            }
                        }
                    }
                } else {
                    thumbnail = MediaStore.Images.Thumbnails.getThumbnail(
                        contentResolver,
                        id.toLong(),
                        MediaStore.Images.Thumbnails.MINI_KIND,
                        null
                    )

                    latLongArr[0] = getDouble(latitudeIndex)
                    latLongArr[1] = getDouble(longitudeIndex)
                }

                outputStream = ByteArrayOutputStream()
                thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                val data = outputStream.toByteArray()

                completion(data, id, created, "${latLongArr[0]}, ${latLongArr[1]}")
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                outputStream?.close()
            }

            close()
        }
    }

    private val columns = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DATE_ADDED,
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.LATITUDE, // return value on device below Android Q
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.LONGITUDE, // return value on device below Android Q
    )

    private fun countForGalleryImage(): Int {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val cursor = activity.contentResolver.query(uri, columns, null, null, null)
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
    fun isPermissionGranted(permissionNames: Array<String>): Boolean
    fun askForPermission(permissionNames: Array<String>, requestCode: Int)
}

/**
 * Convert dip or dp value to px value to keep the size unchanged
 */
fun Context.dip2px(dipValue: Float): Int {
    val scale: Float = resources.displayMetrics.density
    return (dipValue * scale + 0.5f).toInt()
}
