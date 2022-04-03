import 'dart:async';

import 'package:flutter/services.dart';
import 'package:image_picker_platform_interface/image_picker_platform_interface.dart';

class ImagePickerAndroidPlatform extends ImagePickerPlatform {
  static const _channel = MethodChannel('image_picker');

  /// Sample [MethodChannel] invokation.
  @override
  Future<String?> getPlatformVersion() async {
    final version = await _channel.invokeMethod<String?>('getPlatformVersion');
    return version;
  }
}
