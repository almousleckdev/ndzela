import '../../../core/money/money.dart';

class ArrearsSummary {
  const ArrearsSummary({
    required this.unpaidDays,
    required this.total,
    this.locked = false,
    this.suspended = false,
  });

  final int unpaidDays;
  final Money total;

  // Too many unpaid days
  final bool locked;

  // The account was suspended
  final bool suspended;

  bool get isLate => unpaidDays > 0;

  bool get restricted => locked || suspended;
}
