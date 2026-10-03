import '../../../core/money/money.dart';
import '../domain/arrears_summary.dart';

class ArrearsDto {
  const ArrearsDto({
    required this.unpaidDays,
    required this.totalDue,
    required this.currency,
    required this.locked,
    required this.suspended,
  });

  factory ArrearsDto.fromJson(Map<String, dynamic> json) => ArrearsDto(
    unpaidDays: json['unpaidDays'] as int,
    totalDue: json['totalDue'] as int,
    currency: json['currency'] as String,
    locked: json['locked'] as bool? ?? false,
    suspended: json['suspended'] as bool? ?? false,
  );

  final int unpaidDays;
  final int totalDue;
  final String currency;
  final bool locked;
  final bool suspended;

  ArrearsSummary toDomain() => ArrearsSummary(
    unpaidDays: unpaidDays,
    total: Money(totalDue, currency),
    locked: locked,
    suspended: suspended,
  );
}
