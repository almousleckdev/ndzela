import '../../../core/money/money.dart';
import 'payment_attempt.dart';
import 'payment_status.dart';

// What the driver paid during one payment week: from Monday to Sunday noon, the last warning. Only confirmed payments count.
class WeekPayments {
  const WeekPayments({required this.start, required this.payments});

  // Monday, 00:00
  final DateTime start;

  // Newest first
  final List<PaymentAttempt> payments;

  DateTime get closesAt => DateTime(start.year, start.month, start.day + 6, 12);

  Money get total => Money(
    payments.fold(0, (sum, payment) => sum + payment.amount.amount),
    payments.isEmpty ? 'XAF' : payments.first.amount.currency,
  );

  // The week of `now`; a payment made after Sunday noon stays with the week it closed until Monday starts the next one.
  static WeekPayments of(List<PaymentAttempt> attempts, DateTime now) {
    final start = DateTime(now.year, now.month, now.day - (now.weekday - 1));
    final end = DateTime(start.year, start.month, start.day + 7);
    final inWeek = attempts.where((a) {
      final at = a.completedAt ?? a.createdAt;
      return a.status == PaymentStatus.success &&
          !at.isBefore(start) &&
          at.isBefore(end);
    }).toList()..sort((a, b) => _at(b).compareTo(_at(a)));
    return WeekPayments(start: start, payments: inWeek);
  }

  static DateTime _at(PaymentAttempt a) => a.completedAt ?? a.createdAt;
}
