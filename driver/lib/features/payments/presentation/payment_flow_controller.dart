import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../domain/payment_attempt.dart';
import '../domain/payment_flow_state.dart';
import 'payments_providers.dart';

// Follows a payment from "approve on your phone" to the operator's answer
class PaymentFlowController extends Notifier<PaymentFlowState> {
  static const _pollEvery = Duration(seconds: 3);
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
    _timer = Timer.periodic(_pollEvery, (_) => _poll(attempt, startedAt));
  }

  // Back to the form, e.g.
  void reset() {
    _timer?.cancel();
    state = const PaymentFlowIdle();
  }

  // Asks the server again after the app stopped waiting.
  Future<void> recheck() async {
    final current = state;
    if (current is! PaymentFlowStalled) return;
    await _fetch(current.attempt);
  }

  Future<void> _poll(PaymentAttempt attempt, DateTime startedAt) async {
    if (DateTime.now().difference(startedAt) >
        ref.read(paymentGiveUpAfterProvider)) {
      _timer?.cancel();
      state = PaymentFlowStalled(attempt);
      return;
    }
    await _fetch(attempt);
  }

  Future<void> _fetch(PaymentAttempt attempt) async {
    try {
      final latest = await ref
          .read(paymentsRepositoryProvider)
          .fetchAttempt(attempt.id);
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

// How long the app waits for the operator before it says the confirmation is pending; a provider so tests can shorten it.
final paymentGiveUpAfterProvider = Provider<Duration>(
  (ref) => const Duration(minutes: 10),
);

final paymentFlowControllerProvider =
    NotifierProvider<PaymentFlowController, PaymentFlowState>(
      PaymentFlowController.new,
    );
