import '../../../core/money/money.dart';
import 'payment_option.dart';
import 'rent_day.dart';

class ArrearsSummary {
  const ArrearsSummary({
    required this.unpaidDays,
    required this.total,
    this.locked = false,
    this.suspended = false,
    this.options = const [],
    this.weekClosesAt,
    this.weekSettled = true,
    this.days = const [],
  });

  final int unpaidDays;
  final Money total;

  // Too many unpaid days
  final bool locked;

  // The account was suspended
  final bool suspended;

  // The numbers of unpaid days that can be paid, oldest first
  final List<PaymentOption> options;

  // When the week's rents must be settled (Sunday noon), and whether they are
  final DateTime? weekClosesAt;
  final bool weekSettled;

  // The rent days of the last weeks, oldest first
  final List<RentDay> days;

  bool get isLate => unpaidDays > 0;

  bool get restricted => locked || suspended;

  // The week's rents are not settled and the weekend is near
  bool showsWeekWarning(DateTime now) =>
      !weekSettled && weekClosesAt != null && now.weekday >= DateTime.friday;
}
