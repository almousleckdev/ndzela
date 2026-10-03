import '../../../core/money/money.dart';

// What paying would cost
class PaymentQuote {
  const PaymentQuote({
    required this.amount,
    required this.fee,
    this.accountHolder,
  });

  final Money amount;
  final Money fee;
  final String? accountHolder;

  Money get total => Money(amount.amount + fee.amount, amount.currency);
}
