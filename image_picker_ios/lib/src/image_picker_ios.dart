import 'dart:async';

import 'package:flutter/services.dart';
import 'package:image_picker_platform_interface/image_picker_platform_interface.dart';

class ImagePickerIosPlatform extends ImagePickerPlatform {
  static const _channel = MethodChannel('image_picker');

  /// Sample [MethodChannel] invokation.
  static Future<String?> get platformVersion async {
    final version = await _channel.invokeMethod<String?>('getPlatformVersion');
    return version;
  }
}
