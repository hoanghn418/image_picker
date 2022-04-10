import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:image_picker_android/image_picker_android.dart';

void main() {
  const channel = MethodChannel('hoanghn418.github.io/image_picker');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async => 42);
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getImageCount', () async {
    expect(await ImagePickerAndroidPlatform().getImageCount(), 42);
  });
}
