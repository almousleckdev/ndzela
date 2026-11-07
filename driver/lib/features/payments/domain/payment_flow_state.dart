import 'payment_attempt.dart';

sealed class PaymentFlowState {
  const PaymentFlowState();
}

// No payment in progress
class PaymentFlowIdle extends PaymentFlowState {
  const PaymentFlowIdle();
}

// The driver was asked to approve on their phone; the app follows the payment until the operator answers.
class PaymentFlowTracking extends PaymentFlowState {
  const PaymentFlowTracking(this.attempt);

  final PaymentAttempt attempt;
}

// The operator has not answered after a long wait; the server keeps checking, the driver can check again or leave.
class PaymentFlowStalled extends PaymentFlowState {
  const PaymentFlowStalled(this.attempt);

  final PaymentAttempt attempt;
}

class PaymentFlowFinished extends PaymentFlowState {
  const PaymentFlowFinished(this.attempt);

  final PaymentAttempt attempt;
}
