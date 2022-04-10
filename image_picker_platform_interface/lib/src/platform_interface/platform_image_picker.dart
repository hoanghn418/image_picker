import 'package:image_picker_platform_interface/image_picker_platform_interface.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

abstract class ImagePickerPlatform extends PlatformInterface {
  /// Constructs a [ImagePickerPlatform].
  ImagePickerPlatform() : super(token: _token);

  static final Object _token = Object();

  static ImagePickerPlatform _instance = MethodChannelImagePicker();

  /// The default instance of [ImagePickerPlatform] to use.
  ///
  /// Defaults to [MethodChannelImagePicker].
  static ImagePickerPlatform get instance => _instance;

  /// Platform-specific plugins should set this with their own platform-specific
  /// class that extends [ImagePickerPlatform] when they register themselves.
  static set instance(ImagePickerPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<int> getImageCount() {
    throw UnimplementedError('getImageCount() has not been implemented.');
  }

  Future<Map<String, dynamic>> getImage(int index) {
    throw UnimplementedError('getImage() has not been implemented.');
  }
}
