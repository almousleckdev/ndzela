import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/error/app_exception.dart';
import '../../../core/platform/document_picker.dart';
import 'profile_providers.dart';

// True while a new profile picture is being sent.
class PhotoUploadController extends Notifier<bool> {
  @override
  bool build() => false;

  // Asks the phone for a picture and sends it; returns false when the driver chose nothing.
  Future<bool> change() async {
    final PickedDocument? picked;
    try {
      picked = await ref.read(photoPickerProvider)();
    } on DocumentTooLarge {
      throw const AppException(code: 'FILE_TOO_LARGE');
    }
    if (picked == null) return false;
    state = true;
    try {
      await ref
          .read(profileRepositoryProvider)
          .changePhoto(filename: picked.filename, bytes: picked.bytes);
      ref.invalidate(driverPhotoProvider);
      return true;
    } finally {
      state = false;
    }
  }
}

final photoUploadControllerProvider =
    NotifierProvider.autoDispose<PhotoUploadController, bool>(
      PhotoUploadController.new,
    );
