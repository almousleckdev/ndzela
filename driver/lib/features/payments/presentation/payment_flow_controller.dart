import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../domain/payment_attempt.dart';
import '../domain/payment_flow_state.dart';
import 'payments_providers.dart';

// Follows a payment from "approve on your phone" to the operator's answer
class PaymentFlowController extends Notifier<PaymentFlowState> {
  static const _pollEvery = Duration(seconds: 3);
  static const _giveUpAfter = Duration(minutes: 10);

  Timer? _timer;

  @override
  PaymentFlowState build() {
    ref.onDispose(() => _timer?.cancel());
    return const PaymentFlowIdle();
  }

  void track(PaymentAttempt attempt) {
    _timer?.cancel();
    if (attempt.status.isFinal) {
      _finish(attempt);
      return;
    }
    state = PaymentFlowTracking(attempt);
    final startedAt = DateTime.now();
    _timer = Timer.periodic(_pollEvery, (_) => _poll(attempt.id, startedAt));
  }

  // Back to the form, e.g.
  void reset() {
    _timer?.cancel();
    state = const PaymentFlowIdle();
  }

  Future<void> _poll(String id, DateTime startedAt) async {
    if (DateTime.now().difference(startedAt) > _giveUpAfter) {
      _timer?.cancel();
      return;
    }
    try {
      final latest = await ref
          .read(paymentsRepositoryProvider)
          .fetchAttempt(id);
      if (latest.status.isFinal) await _finish(latest);
    } on Exception {
      return;
    }
  }

  Future<void> _finish(PaymentAttempt attempt) async {
    _timer?.cancel();
    ref
      ..invalidate(arrearsProvider)
      ..invalidate(paymentHistoryProvider);
    try {
      // The new arrears (and so the lock) are known before the result is shown.
      await ref.read(arrearsProvider.future);
    } on Exception {
      // The result is still shown; the next refresh brings the arrears up to date.
    }
    state = PaymentFlowFinished(attempt);
  }
}

final paymentFlowControllerProvider =
    NotifierProvider<PaymentFlowController, PaymentFlowState>(
      PaymentFlowController.new,
    );
