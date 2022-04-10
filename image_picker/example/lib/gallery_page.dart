import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';

import 'gallery_image.dart';

class GalleryPage extends StatefulWidget {
  const GalleryPage({Key? key}) : super(key: key);

  @override
  State<StatefulWidget> createState() => _GalleryPageState();
}

class _GalleryPageState extends State<GalleryPage> {
  final ImagePicker _imagePicker = ImagePicker(imagePickerPlatform: null);

  final _numberOfColumns = 4;
  final _title = "Gallery";

  final _selectedItems = List<GalleryImage>.empty(growable: true);
  final _itemCache = <int, GalleryImage>{};

  Future<GalleryImage> _getItem(int index) async {
    if (_itemCache[index] != null) {
      return _itemCache[index]!;
    }

    var item = await _imagePicker.getImage(index);
    var galleryImage = GalleryImage(
      bytes: item['data'],
      id: item['id'],
      dateCreated: item['created'],
      location: item['location'],
    );
    _itemCache[index] = galleryImage;

    return galleryImage;
  }

  _selectItem(int index) async {
    var galleryImage = await _getItem(index);

    setState(() {
      if (_isSelected(galleryImage.id)) {
        _selectedItems.removeWhere((anItem) => anItem.id == galleryImage.id);
      } else {
        _selectedItems.add(galleryImage);
      }
    });
  }

  _isSelected(String id) {
    return _selectedItems.where((item) => item.id == id).isNotEmpty;
  }

  var _numberOfItems = 0;

  @override
  void initState() {
    super.initState();
    _imagePicker.getImageCount().then((count) => setState(() {
          _numberOfItems = count;
        }));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_title),
      ),
      body: GridView.builder(
          gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: _numberOfColumns),
          itemCount: _numberOfItems,
          itemBuilder: (context, index) {
            return _buildItem(index);
          }),
    );
  }

  _buildItem(int index) => GestureDetector(
      onTap: () {
        _selectItem(index);
      },
      child: Card(
        elevation: 2.0,
        child: FutureBuilder(
            future: _getItem(index),
            builder: (context, snapshot) {
              var item = snapshot.data as GalleryImage?;
              if (item != null) {
                return Container(
                  child: Image.memory(item.bytes, fit: BoxFit.cover),
                  decoration: BoxDecoration(
                      border: Border.all(
                          color: Theme.of(context).primaryColor,
                          width: 2,
                          style: _isSelected(item.id)
                              ? BorderStyle.solid
                              : BorderStyle.none)),
                );
              }
              return Container();
            }),
      ));
}
