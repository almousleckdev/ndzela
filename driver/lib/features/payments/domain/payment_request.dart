import 'payment_operator.dart';

// What the driver chooses; the amount is decided by the server from the driver's arrears.
class PaymentRequest {
  const PaymentRequest({required this.operator, required this.phone});

  final PaymentOperator operator;
  final String phone;
}
