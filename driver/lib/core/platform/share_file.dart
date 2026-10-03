import 'dart:io';
import 'dart:typed_data';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:path_provider/path_provider.dart';
import 'package:share_plus/share_plus.dart';

typedef FileSharer =
    Future<void> Function({
      required Uint8List bytes,
      required String filename,
      required String mimeType,
      String? subject,
    });

// Hands a file to the phone's own share sheet (messaging apps, e-mail, save to files, print).
Future<void> shareFileWithSystem({
  required Uint8List bytes,
  required String filename,
  required String mimeType,
  String? subject,
}) async {
  final directory = await getTemporaryDirectory();
  final file = await File(
    '${directory.path}/$filename',
  ).writeAsBytes(bytes, flush: true);
  await SharePlus.instance.share(
    ShareParams(
      files: [XFile(file.path, mimeType: mimeType)],
      subject: subject,
    ),
  );
}

// A provider so tests can replace the share sheet.
final fileSharerProvider = Provider<FileSharer>((ref) => shareFileWithSystem);
