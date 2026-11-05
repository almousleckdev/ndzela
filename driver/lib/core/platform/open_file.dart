import 'dart:io';
import 'dart:typed_data';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:open_filex/open_filex.dart';
import 'package:path_provider/path_provider.dart';

typedef FileOpener =
    Future<void> Function({required Uint8List bytes, required String filename});

// Shows a file in the phone's own viewer.
Future<void> openFileWithSystem({
  required Uint8List bytes,
  required String filename,
}) async {
  final directory = await getTemporaryDirectory();
  final file = await File(
    '${directory.path}/$filename',
  ).writeAsBytes(bytes, flush: true);
  final result = await OpenFilex.open(file.path);
  if (result.type != ResultType.done) {
    throw const FileSystemException('No viewer for this file');
  }
}

// A provider so tests can replace the viewer.
final fileOpenerProvider = Provider<FileOpener>((ref) => openFileWithSystem);
