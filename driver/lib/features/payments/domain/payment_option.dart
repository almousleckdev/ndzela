import '../../../core/money/money.dart';

// What paying the oldest `days` unpaid days costs, as the server computed it
class PaymentOption {
  const PaymentOption({required this.days, required this.amount});

  final int days;
  final Money amount;

  @override
  bool operator ==(Object other) =>
      other is PaymentOption && other.days == days && other.amount == amount;

  @override
  int get hashCode => Object.hash(days, amount);
}
