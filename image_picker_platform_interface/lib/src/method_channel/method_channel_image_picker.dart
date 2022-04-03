import 'package:flutter/services.dart';
import 'package:image_picker_platform_interface/image_picker_platform_interface.dart';

/// An implementation of [ImagePickerPlatform]
/// that uses a `MethodChannel` to communicate with the native code.
///
/// The `image_picker` plugin code
/// itself never talks to the native code directly.
/// It delegates all calls to an instance of a class
/// that extends the [ImagePickerPlatform].
///
/// The architecture above allows for platforms that communicate differently
/// with the native side (like web) to have a common interface to extend.
///
/// This is the instance that runs when the native side talks
/// to your Flutter app through MethodChannels (Android and iOS platforms).
class MethodChannelImagePicker extends ImagePickerPlatform {
  static const MethodChannel _channel = MethodChannel('image_picker');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await _channel.invokeMethod<String?>('getPlatformVersion');
    return version;
  }
}
