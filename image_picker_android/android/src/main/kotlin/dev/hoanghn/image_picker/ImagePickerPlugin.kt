package dev.hoanghn.image_picker

import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

/** ImagePickerPlugin */
class ImagePickerPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel

    private var activityPluginBinding: ActivityPluginBinding? = null
    private var imagePickerDelegate: ImagePickerDelegate? = null

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel =
            MethodChannel(flutterPluginBinding.binaryMessenger, "hoanghn418.github.io/image_picker")
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        if (activityPluginBinding?.activity == null) {
            result.error(
                "no_activity",
                "image_picker plugin requires a foreground activity.",
                null
            )
            return
        }

        when (call.method) {
            "getImageCount" -> imagePickerDelegate?.getGalleryImageCount(call, result)
            "getImage" -> imagePickerDelegate?.getGalleryItem(call, result)
            else -> result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activityPluginBinding = binding

        imagePickerDelegate = ImagePickerDelegate(binding.activity)
        binding.addRequestPermissionsResultListener(imagePickerDelegate!!)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity()
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onDetachedFromActivity() {
        if (imagePickerDelegate != null)
            activityPluginBinding?.removeRequestPermissionsResultListener(imagePickerDelegate!!)
        imagePickerDelegate = null

        activityPluginBinding = null
    }
}
