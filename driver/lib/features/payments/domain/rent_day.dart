import '../../../core/money/money.dart';

// How a rent day stands: paid on time or late, unpaid, or ended with its deadline still to come
enum RentDayStatus { paidOnTime, paidLate, unpaid, awaiting }

class RentDay {
  const RentDay({
    required this.date,
    required this.status,
    required this.amount,
  });

  final DateTime date;
  final RentDayStatus status;
  final Money amount;
}
