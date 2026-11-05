import '../../../core/money/money.dart';
import 'payment_attempt.dart';
import 'payment_status.dart';

// What the driver paid during one week (Monday to Sunday).
class WeeklyTotal {
  const WeeklyTotal({
    required this.start,
    required this.paid,
    required this.payments,
    required this.days,
  });

  final DateTime start;
  final Money paid;
  final int payments;

  // Rent days those payments settled
  final int days;

  DateTime get end => start.add(const Duration(days: 6));

  // The last `weeks` weeks ending with the one of `now`, newest first; only confirmed payments count.
  static List<WeeklyTotal> lastWeeks(
    List<PaymentAttempt> attempts,
    DateTime now, {
    int weeks = 4,
  }) {
    final confirmed = attempts.where((a) => a.status == PaymentStatus.success);
    final currency = confirmed.isEmpty
        ? 'XAF'
        : confirmed.first.amount.currency;
    final thisMonday = _monday(now);
    return [
      for (var i = 0; i < weeks; i++)
        _week(thisMonday.subtract(Duration(days: 7 * i)), confirmed, currency),
    ];
  }

  static WeeklyTotal _week(
    DateTime start,
    Iterable<PaymentAttempt> confirmed,
    String currency,
  ) {
    final inWeek = confirmed.where(
      (a) => _monday(a.completedAt ?? a.createdAt) == start,
    );
    return WeeklyTotal(
      start: start,
      paid: Money(inWeek.fold(0, (sum, a) => sum + a.amount.amount), currency),
      payments: inWeek.length,
      days: inWeek.fold(0, (sum, a) => sum + a.days.length),
    );
  }

  static DateTime _monday(DateTime date) =>
      DateTime(date.year, date.month, date.day - (date.weekday - 1));
}
