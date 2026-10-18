import '../../../core/money/money.dart';
import '../domain/payment_quote.dart';
import 'payment_option_dto.dart';

class PaymentQuoteDto {
  const PaymentQuoteDto({
    required this.amount,
    required this.fee,
    required this.currency,
    this.accountHolder,
    this.days = 0,
    this.options = const [],
  });

  factory PaymentQuoteDto.fromJson(Map<String, dynamic> json) =>
      PaymentQuoteDto(
        amount: (json['amount'] as num).toInt(),
        fee: (json['fee'] as num).toDouble(),
        currency: json['currency'] as String,
        accountHolder: json['accountHolder'] as String?,
        days: json['days'] as int? ?? 0,
        options: [
          for (final item in json['options'] as List<dynamic>? ?? const [])
            PaymentOptionDto.fromJson(item as Map<String, dynamic>),
        ],
      );

  final int amount;
  final double fee;
  final String currency;
  final String? accountHolder;
  final int days;
  final List<PaymentOptionDto> options;

  // The operator charges fractions of a unit; the driver is shown the next whole unit.
  PaymentQuote toDomain() => PaymentQuote(
    amount: Money(amount, currency),
    fee: Money(fee.ceil(), currency),
    accountHolder: accountHolder,
    days: days,
    options: [for (final option in options) option.toDomain(currency)],
  );
}
