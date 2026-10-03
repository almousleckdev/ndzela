import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../domain/mobile_money_number.dart';
import '../domain/payment_operator.dart';
import '../domain/payment_request.dart';
import 'payment_flow_controller.dart';
import 'payments_providers.dart';

class PaymentFormState {
  const PaymentFormState({
    this.operator,
    this.phone = '',
    this.quotedPhone = '',
    this.submitting = false,
  });

  final PaymentOperator? operator;
  final String phone;

  // The number the fee quote is asked for
  final String quotedPhone;
  final bool submitting;

  bool get canSubmit =>
      operator != null &&
      isValidMobileMoneyNumber(phone) &&
      phone == quotedPhone &&
      !submitting;

  // The key of the fee quote for what is chosen so far, or null while the choice is incomplete.
  ({String operator, String phone})? get quoteKey =>
      operator != null && isValidMobileMoneyNumber(quotedPhone)
      ? (operator: operator!.code, phone: quotedPhone)
      : null;

  PaymentFormState copyWith({
    PaymentOperator? operator,
    String? phone,
    String? quotedPhone,
    bool? submitting,
  }) => PaymentFormState(
    operator: operator ?? this.operator,
    phone: phone ?? this.phone,
    quotedPhone: quotedPhone ?? this.quotedPhone,
    submitting: submitting ?? this.submitting,
  );
}

class PaymentFormController extends Notifier<PaymentFormState> {
  static const _typingPause = Duration(milliseconds: 600);

  Timer? _pause;

  @override
  PaymentFormState build() {
    ref.onDispose(() => _pause?.cancel());
    return const PaymentFormState();
  }

  void selectOperator(PaymentOperator operator) =>
      state = state.copyWith(operator: operator);

  // The driver's own number, put in the field when the form opens
  void prefillPhone(String phone) =>
      state = state.copyWith(phone: phone, quotedPhone: phone);

  // Typing
  void changePhone(String phone) {
    state = state.copyWith(phone: phone);
    _pause?.cancel();
    _pause = Timer(
      _typingPause,
      () => state = state.copyWith(quotedPhone: phone),
    );
  }

  // Starts the payment on the server and hands it to the flow, which follows it to the end.
  Future<void> submit() async {
    if (!state.canSubmit) return;
    state = state.copyWith(submitting: true);
    try {
      final attempt = await ref
          .read(paymentsRepositoryProvider)
          .start(PaymentRequest(operator: state.operator!, phone: state.phone));
      ref.read(paymentFlowControllerProvider.notifier).track(attempt);
    } finally {
      state = state.copyWith(submitting: false);
    }
  }
}

final paymentFormControllerProvider =
    NotifierProvider.autoDispose<PaymentFormController, PaymentFormState>(
      PaymentFormController.new,
    );
