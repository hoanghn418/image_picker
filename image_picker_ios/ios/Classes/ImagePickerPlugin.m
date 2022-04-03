#import "ImagePickerPlugin.h"
#if __has_include(<image_picker_ios/image_picker_ios-Swift.h>)
#import <image_picker_ios/image_picker_ios-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "image_picker_ios-Swift.h"
#endif

@implementation ImagePickerPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftImagePickerPlugin registerWithRegistrar:registrar];
}
@end
