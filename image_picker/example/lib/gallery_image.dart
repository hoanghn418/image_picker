import 'dart:typed_data';

class GalleryImage {
  GalleryImage({
    required this.id,
    required this.bytes,
    required this.dateCreated,
    required this.location,
  });

  String id;
  Uint8List bytes;
  int dateCreated;
  String location;
}
