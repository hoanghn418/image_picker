import 'package:flutter_test/flutter_test.dart';
import 'package:image_picker/image_picker.dart';
import 'package:mocktail/mocktail.dart';

class MockImagePickerPlatform extends Mock implements ImagePickerPlatform {}

void main() {
  group('ImagePicker', () {
    test('can be instantiated', () {
      expect(
        ImagePicker(imagePickerPlatform: MockImagePickerPlatform()),
        isNotNull,
      );
    });
  });
}
