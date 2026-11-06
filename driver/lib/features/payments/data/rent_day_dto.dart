import '../../../core/money/money.dart';
import '../domain/rent_day.dart';

class RentDayDto {
  const RentDayDto({
    required this.day,
    required this.amount,
    required this.status,
  });

  factory RentDayDto.fromJson(Map<String, dynamic> json) => RentDayDto(
    day: json['day'] as String,
    amount: (json['amount'] as num).toInt(),
    status: json['status'] as String,
  );

  final String day;
  final int amount;
  final String status;

  RentDay toDomain(String currency) => RentDay(
    date: DateTime.parse(day),
    status: switch (status) {
      'PAID_ON_TIME' => RentDayStatus.paidOnTime,
      'PAID_LATE' => RentDayStatus.paidLate,
      'AWAITING' => RentDayStatus.awaiting,
      _ => RentDayStatus.unpaid,
    },
    amount: Money(amount, currency),
  );
}
