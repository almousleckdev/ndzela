import 'dart:io';

import 'package:path_provider/path_provider.dart';

// Contracts and receipts are opened from the phone's temporary folder; they hold personal data, so they are removed when a session ends and when the app starts.
Future<void> clearTemporaryFiles() async {
  try {
    final directory = await getTemporaryDirectory();
    if (!await directory.exists()) return;
    await for (final entry in directory.list()) {
      final name = entry.uri.pathSegments.where((s) => s.isNotEmpty).last;
      if (entry is File &&
          (name.endsWith('.pdf') ||
              name.endsWith('.png') ||
              name.endsWith('.jpg'))) {
        await entry.delete();
      }
    }
  } on FileSystemException {
    return;
  }
}
