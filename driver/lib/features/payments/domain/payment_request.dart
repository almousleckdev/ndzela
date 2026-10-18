import 'payment_operator.dart';

// What the driver chooses: how, and how many days; the amount is decided by the server.
class PaymentRequest {
  const PaymentRequest({
    required this.operator,
    required this.phone,
    this.days,
  });

  final PaymentOperator operator;
  final String phone;

  // The oldest unpaid days to pay; null pays all of them
  final int? days;
}
