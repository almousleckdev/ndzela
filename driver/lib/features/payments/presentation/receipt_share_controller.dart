import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/platform/share_file.dart';
import '../domain/payment_attempt.dart';
import 'payments_providers.dart';

// Downloads a payment's receipt and offers it to the phone's share sheet.
class ReceiptShareController extends AsyncNotifier<void> {
  @override
  Future<void> build() async {}

  Future<void> share(PaymentAttempt attempt) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      final pdf = await ref
          .read(paymentsRepositoryProvider)
          .fetchReceipt(attempt.id);
      await ref.read(fileSharerProvider)(
        bytes: pdf,
        filename: 'recu-${attempt.reference}.pdf',
        mimeType: 'application/pdf',
        subject: 'Reçu de paiement ${attempt.reference}',
      );
    });
  }
}

final receiptShareControllerProvider =
    AsyncNotifierProvider.autoDispose<ReceiptShareController, void>(
      ReceiptShareController.new,
    );
