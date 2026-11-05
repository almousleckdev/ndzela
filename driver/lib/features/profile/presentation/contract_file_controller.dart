import 'dart:typed_data';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/platform/open_file.dart';
import '../../../core/platform/share_file.dart';
import 'profile_providers.dart';

const _filename = 'contrat-ndzela.pdf';

// Opens the driver's contract in the phone's viewer, or hands it to the share sheet to be saved.
class ContractFileController extends AsyncNotifier<void> {
  @override
  Future<void> build() async {}

  Future<void> open() => _run(
    (pdf) => ref.read(fileOpenerProvider)(bytes: pdf, filename: _filename),
  );

  Future<void> download() => _run(
    (pdf) => ref.read(fileSharerProvider)(
      bytes: pdf,
      filename: _filename,
      mimeType: 'application/pdf',
      subject: 'Mon contrat NdZela',
    ),
  );

  Future<void> _run(Future<void> Function(Uint8List pdf) use) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      final pdf = await ref.read(profileRepositoryProvider).fetchContractPdf();
      await use(pdf);
    });
  }
}

final contractFileControllerProvider =
    AsyncNotifierProvider.autoDispose<ContractFileController, void>(
      ContractFileController.new,
    );
