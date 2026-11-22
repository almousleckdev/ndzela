import 'dart:typed_data';

import 'package:file_picker/file_picker.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class PickedDocument {
  const PickedDocument({required this.filename, required this.bytes});

  final String filename;
  final Uint8List bytes;
}

typedef DocumentPicker = Future<PickedDocument?> Function();

const _maxBytes = 5 * 1024 * 1024;

// Lets the driver choose a photo or a PDF from the phone; null when nothing was chosen.
Future<PickedDocument?> pickDocumentFromPhone() =>
    _pick(const ['jpg', 'jpeg', 'png', 'pdf']);

// Lets the driver choose a picture of themselves (an image, never a PDF).
Future<PickedDocument?> pickPhotoFromPhone() =>
    _pick(const ['jpg', 'jpeg', 'png']);

Future<PickedDocument?> _pick(List<String> extensions) async {
  final file = await FilePicker.pickFile(
    type: FileType.custom,
    allowedExtensions: extensions,
  );
  if (file == null) return null;
  if (await file.length() case final size? when size > _maxBytes) {
    throw const DocumentTooLarge();
  }
  return PickedDocument(filename: file.name, bytes: await file.readAsBytes());
}

class DocumentTooLarge implements Exception {
  const DocumentTooLarge();
}

// A provider so tests can replace the phone's picker.
final documentPickerProvider = Provider<DocumentPicker>(
  (ref) => pickDocumentFromPhone,
);

final photoPickerProvider = Provider<DocumentPicker>(
  (ref) => pickPhotoFromPhone,
);
