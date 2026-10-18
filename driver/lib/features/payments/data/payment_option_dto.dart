import '../../../core/money/money.dart';
import '../domain/payment_option.dart';

class PaymentOptionDto {
  const PaymentOptionDto({required this.days, required this.amount});

  factory PaymentOptionDto.fromJson(Map<String, dynamic> json) =>
      PaymentOptionDto(
        days: json['days'] as int,
        amount: (json['amount'] as num).toInt(),
      );

  final int days;
  final int amount;

  PaymentOption toDomain(String currency) =>
      PaymentOption(days: days, amount: Money(amount, currency));
}
