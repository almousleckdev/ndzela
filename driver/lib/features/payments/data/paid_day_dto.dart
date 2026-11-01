import '../../../core/money/money.dart';
import '../domain/paid_day.dart';

class PaidDayDto {
  const PaidDayDto({
    required this.day,
    required this.kind,
    required this.amount,
    required this.onTime,
  });

  factory PaidDayDto.fromJson(Map<String, dynamic> json) => PaidDayDto(
    day: json['day'] as String,
    kind: json['kind'] as String,
    amount: (json['amount'] as num).toInt(),
    onTime: json['onTime'] as bool,
  );

  final String day;
  final String kind;
  final int amount;
  final bool onTime;

  PaidDay toDomain(String currency) => PaidDay(
    day: DateTime.parse(day),
    amount: Money(amount, currency),
    onTime: onTime,
    isPenalty: kind == 'PENALTY',
  );
}
