import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/error/app_exception.dart';
import '../../../core/platform/document_picker.dart';
import '../domain/document_kind.dart';
import 'profile_providers.dart';

// The document being added, so only its row shows the progress; null when nothing is being sent.
class DocumentUploadController extends Notifier<DocumentKind?> {
  @override
  DocumentKind? build() => null;

  // Asks the phone for a file and sends it; returns false when the driver chose nothing.
  Future<bool> add(DocumentKind kind) async {
    final picked = await _pick();
    if (picked == null) return false;
    state = kind;
    try {
      await ref
          .read(profileRepositoryProvider)
          .addDocument(
            kind: kind,
            filename: picked.filename,
            bytes: picked.bytes,
          );
      ref.invalidate(documentsProvider);
      return true;
    } finally {
      state = null;
    }
  }

  Future<PickedDocument?> _pick() async {
    try {
      return await ref.read(documentPickerProvider)();
    } on DocumentTooLarge {
      throw const AppException(code: 'FILE_TOO_LARGE');
    }
  }
}

final documentUploadControllerProvider =
    NotifierProvider.autoDispose<DocumentUploadController, DocumentKind?>(
      DocumentUploadController.new,
    );
