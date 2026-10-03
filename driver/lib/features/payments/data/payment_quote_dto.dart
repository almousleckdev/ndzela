import '../../../core/money/money.dart';
import '../domain/payment_quote.dart';

class PaymentQuoteDto {
  const PaymentQuoteDto({
    required this.amount,
    required this.fee,
    required this.currency,
    this.accountHolder,
  });

  factory PaymentQuoteDto.fromJson(Map<String, dynamic> json) =>
      PaymentQuoteDto(
        amount: (json['amount'] as num).toInt(),
        fee: (json['fee'] as num).toDouble(),
        currency: json['currency'] as String,
        accountHolder: json['accountHolder'] as String?,
      );

  final int amount;
  final double fee;
  final String currency;
  final String? accountHolder;

  // The operator charges fractions of a unit; the driver is shown the next whole unit.
  PaymentQuote toDomain() => PaymentQuote(
    amount: Money(amount, currency),
    fee: Money(fee.ceil(), currency),
    accountHolder: accountHolder,
  );
}
