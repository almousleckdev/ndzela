import '../../../core/money/money.dart';
import '../domain/payment_attempt.dart';
import '../domain/payment_status.dart';

class PaymentAttemptDto {
  const PaymentAttemptDto({
    required this.id,
    required this.status,
    required this.amount,
    required this.currency,
    required this.operatorCode,
    required this.phone,
    required this.reference,
    required this.createdAt,
    this.beneficiary,
    this.providerReference,
    this.fee,
    this.completedAt,
  });

  factory PaymentAttemptDto.fromJson(Map<String, dynamic> json) =>
      PaymentAttemptDto(
        id: json['id'] as String,
        status: json['status'] as String,
        amount: (json['amount'] as num).toInt(),
        fee: (json['fee'] as num?)?.toDouble(),
        currency: json['currency'] as String,
        operatorCode: json['operatorCode'] as String,
        phone: json['phone'] as String? ?? '',
        reference: json['reference'] as String? ?? '',
        beneficiary: json['beneficiary'] as String?,
        providerReference: json['providerReference'] as String?,
        createdAt: json['createdAt'] as String,
        completedAt: json['completedAt'] as String?,
      );

  final String id;
  final String status;
  final int amount;
  final double? fee;
  final String currency;
  final String operatorCode;
  final String phone;
  final String reference;
  final String? beneficiary;
  final String? providerReference;
  final String createdAt;
  final String? completedAt;

  PaymentAttempt toDomain() => PaymentAttempt(
    id: id,
    status: PaymentStatus.fromCode(status),
    amount: Money(amount, currency),
    fee: fee == null ? null : Money(fee!.ceil(), currency),
    operatorCode: operatorCode,
    phone: phone,
    reference: reference,
    beneficiary: beneficiary,
    providerReference: providerReference,
    createdAt: DateTime.parse(createdAt).toLocal(),
    completedAt: completedAt == null
        ? null
        : DateTime.parse(completedAt!).toLocal(),
  );
}
