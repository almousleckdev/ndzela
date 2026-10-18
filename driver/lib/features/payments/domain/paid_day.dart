import '../../../core/money/money.dart';

// A rent day a payment settled
class PaidDay {
  const PaidDay({
    required this.day,
    required this.amount,
    required this.onTime,
    this.isPenalty = false,
  });

  final DateTime day;
  final Money amount;
  final bool onTime;
  final bool isPenalty;
}
