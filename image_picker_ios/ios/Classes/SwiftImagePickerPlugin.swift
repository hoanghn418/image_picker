import Flutter
import UIKit
import Photos

public class SwiftImagePickerPlugin: NSObject, FlutterPlugin {
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "hoanghn418.github.io/image_picker", binaryMessenger: registrar.messenger())
        let instance = SwiftImagePickerPlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
    }
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        switch (call.method) {
        case "getImageCount": result(self.getGalleryImageCount())
        case "getImage":
            let index = call.arguments as? Int ?? 0
            self.dataForGalleryItem(index: index, completion: { (data, id, created, location) in
                result([
                    "data": data ?? Data(),
                    "id": id,
                    "created": created,
                    "location": location
                ])
            })
        default: result(FlutterError(code: "0", message: nil, details: nil))
        }
    }
    
    func dataForGalleryItem(index: Int, completion: @escaping (Data?, String, Int, String) -> Void) {
        let fetchOptions = PHFetchOptions()
        fetchOptions.includeHiddenAssets = true
        
        let collection: PHFetchResult = PHAsset.fetchAssets(with: fetchOptions)
        if (index >= collection.count) {
            return
        }
        
        let asset = collection.object(at: index)
        
        let options = PHImageRequestOptions()
        // options.deliveryMode = .fastFormat // Commented because getting image error
        options.isSynchronous = true
        
        let imageSize = CGSize(width: 250,
                               height: 250)
        
        let imageManager = PHCachingImageManager()
        imageManager.requestImage(for: asset, targetSize: imageSize, contentMode: .aspectFit, options: options) { (image, info) in
            if let image = image {
                let data = image.jpegData(compressionQuality: 0.9)
                completion(data,
                           asset.localIdentifier,
                           Int(asset.creationDate?.timeIntervalSince1970 ?? 0),
                           "\(asset.location ?? CLLocation())")
            } else {
                completion(nil, "", 0, "")
            }
        }
    }
    
    func getGalleryImageCount() -> Int {
        let fetchOptions = PHFetchOptions()
        fetchOptions.includeHiddenAssets = true
        
        let collection: PHFetchResult = PHAsset.fetchAssets(with: fetchOptions)
        return collection.count
    }
}
