import '../../../core/money/money.dart';
import 'payment_option.dart';

class ArrearsSummary {
  const ArrearsSummary({
    required this.unpaidDays,
    required this.total,
    this.locked = false,
    this.suspended = false,
    this.options = const [],
    this.settleBy,
  });

  final int unpaidDays;
  final Money total;

  // Too many unpaid days
  final bool locked;

  // The account was suspended
  final bool suspended;

  // The numbers of unpaid days that can be paid, oldest first
  final List<PaymentOption> options;

  // When the oldest rent day still to settle is due, noon of that day
  final DateTime? settleBy;

  bool get isLate => unpaidDays > 0;

  // A day open since midnight can be paid before it is late
  bool get canPay => unpaidDays > 0 || options.isNotEmpty;

  // How many rent days paying everything settles
  int get daysToSettle => options.isEmpty ? unpaidDays : options.last.days;

  // What paying everything costs
  Money get amountToSettle => options.isEmpty ? total : options.last.amount;

  bool get restricted => locked || suspended;
}
