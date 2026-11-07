import '../../../core/money/money.dart';
import '../domain/arrears_summary.dart';
import 'payment_option_dto.dart';

class ArrearsDto {
  const ArrearsDto({
    required this.unpaidDays,
    required this.totalDue,
    required this.currency,
    required this.locked,
    required this.suspended,
    this.options = const [],
    this.settleBy,
  });

  factory ArrearsDto.fromJson(Map<String, dynamic> json) => ArrearsDto(
    unpaidDays: json['unpaidDays'] as int,
    totalDue: json['totalDue'] as int,
    currency: json['currency'] as String,
    locked: json['locked'] as bool? ?? false,
    suspended: json['suspended'] as bool? ?? false,
    options: [
      for (final item in json['options'] as List<dynamic>? ?? const [])
        PaymentOptionDto.fromJson(item as Map<String, dynamic>),
    ],
    settleBy: json['settleBy'] as String?,
  );

  final int unpaidDays;
  final int totalDue;
  final String currency;
  final bool locked;
  final bool suspended;
  final List<PaymentOptionDto> options;
  final String? settleBy;

  ArrearsSummary toDomain() => ArrearsSummary(
    unpaidDays: unpaidDays,
    total: Money(totalDue, currency),
    locked: locked,
    suspended: suspended,
    options: [for (final option in options) option.toDomain(currency)],
    settleBy: settleBy == null ? null : DateTime.parse(settleBy!).toLocal(),
  );
}
