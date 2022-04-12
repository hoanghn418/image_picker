import 'dart:async';

import 'package:flutter/services.dart';
import 'package:image_picker_platform_interface/image_picker_platform_interface.dart';

class ImagePickerIosPlatform extends ImagePickerPlatform {
  static const MethodChannel _channel =
      MethodChannel('hoanghn418.github.io/image_picker');

  /// Registers this class as the default instance of [ImagePickerPlatform].
  static void registerWith() {
    ImagePickerPlatform.instance = ImagePickerIosPlatform();
  }

  @override
  Future<int> getImageCount() async {
    final response = await _channel.invokeMethod<int>('getImageCount');
    return response!;
  }

  @override
  Future<Map<String, dynamic>> getImage(int index) async {
    final response = await _channel.invokeMethod<Map>('getImage', index);
    return Map<String, dynamic>.from(response!);
  }
}
