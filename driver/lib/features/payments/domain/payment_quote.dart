import '../../../core/money/money.dart';
import 'payment_option.dart';

// What paying would cost
class PaymentQuote {
  const PaymentQuote({
    required this.amount,
    required this.fee,
    this.accountHolder,
    this.days = 0,
    this.options = const [],
  });

  final Money amount;
  final Money fee;
  final String? accountHolder;

  // The days the amount covers, and every number of days that can be paid
  final int days;
  final List<PaymentOption> options;

  Money get total => Money(amount.amount + fee.amount, amount.currency);
}
