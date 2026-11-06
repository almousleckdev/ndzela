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
Future<PickedDocument?> pickDocumentFromPhone() async {
  final file = await FilePicker.pickFile(
    type: FileType.custom,
    allowedExtensions: const ['jpg', 'jpeg', 'png', 'pdf'],
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
