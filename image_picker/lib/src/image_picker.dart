import 'package:image_picker_platform_interface/image_picker_platform_interface.dart';

class ImagePicker {
  ImagePicker({
    required ImagePickerPlatform? imagePickerPlatform,
  }) : _imagePickerPlatform =
            imagePickerPlatform ?? ImagePickerPlatform.instance;

  final ImagePickerPlatform _imagePickerPlatform;

  Future<int> getImageCount() async {
    return _imagePickerPlatform.getImageCount();
  }

  Future<Map<String, dynamic>> getImage(int index) async {
    return _imagePickerPlatform.getImage(index);
  }
}
