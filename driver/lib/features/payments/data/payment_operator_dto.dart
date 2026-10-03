import '../domain/payment_operator.dart';

class PaymentOperatorDto {
  const PaymentOperatorDto({required this.code, required this.label});

  factory PaymentOperatorDto.fromJson(Map<String, dynamic> json) =>
      PaymentOperatorDto(
        code: json['code'] as String,
        label: json['label'] as String,
      );

  final String code;
  final String label;

  PaymentOperator toDomain() => PaymentOperator(code: code, label: label);
}
