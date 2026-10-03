import '../../../core/money/money.dart';
import 'payment_status.dart';

// A mobile-money payment the driver started, from request to the operator's answer.
class PaymentAttempt {
  const PaymentAttempt({
    required this.id,
    required this.status,
    required this.amount,
    required this.operatorCode,
    required this.createdAt,
    required this.phone,
    required this.reference,
    this.beneficiary,
    this.providerReference,
    this.fee,
    this.completedAt,
  });

  final String id;
  final PaymentStatus status;
  final Money amount;
  final Money? fee;
  final String operatorCode;
  final String phone;

  // Our reference for the payment, and the operator network's transaction number once there is one.
  final String reference;
  final String? providerReference;

  // Who received the money
  final String? beneficiary;
  final DateTime createdAt;
  final DateTime? completedAt;

  // What left the driver's account
  Money get totalPaid =>
      Money(amount.amount + (fee?.amount ?? 0), amount.currency);
}
