import 'package:driver/core/money/money.dart';
import 'package:driver/features/payments/domain/payment_attempt.dart';
import 'package:driver/features/payments/domain/payment_status.dart';
import 'package:driver/features/payments/domain/week_payments.dart';
import 'package:flutter_test/flutter_test.dart';

PaymentAttempt _payment(
  String id,
  DateTime at, {
  PaymentStatus status = PaymentStatus.success,
  int amount = 23000,
}) => PaymentAttempt(
  id: id,
  status: status,
  amount: Money(amount, 'XAF'),
  operatorCode: 'MOOV_MONEY',
  phone: '062820745',
  reference: 'REF-$id',
  createdAt: at,
  completedAt: at,
);

void main() {
  // Friday 6 November 2026
  final now = DateTime(2026, 11, 6, 17, 0);

  test('the week runs from Monday 00:00 and closes on Sunday at noon', () {
    final week = WeekPayments.of(const [], now);

    expect(week.start, DateTime(2026, 11, 2));
    expect(week.closesAt, DateTime(2026, 11, 8, 12));
  });

  test(
    'only the confirmed payments of this week are listed, newest first, with their total',
    () {
      final week = WeekPayments.of([
        _payment('last-week', DateTime(2026, 11, 1, 10)),
        _payment('monday', DateTime(2026, 11, 2, 0, 5)),
        _payment(
          'failed',
          DateTime(2026, 11, 4, 9),
          status: PaymentStatus.failed,
        ),
        _payment('wednesday', DateTime(2026, 11, 4, 11), amount: 46000),
        _payment('october', DateTime(2026, 10, 14, 9)),
      ], now);

      expect(week.payments.map((p) => p.id), ['wednesday', 'monday']);
      expect(week.total.amount, 69000);
    },
  );

  test(
    'a payment made on Sunday afternoon stays with the week it closed until Monday starts the next',
    () {
      final sunday = DateTime(2026, 11, 8, 15);
      final week = WeekPayments.of([
        _payment('late', DateTime(2026, 11, 8, 14)),
      ], sunday);

      expect(week.start, DateTime(2026, 11, 2));
      expect(week.payments.map((p) => p.id), ['late']);
      expect(
        WeekPayments.of([
          _payment('late', DateTime(2026, 11, 8, 14)),
        ], DateTime(2026, 11, 9, 8)).payments,
        isEmpty,
      );
    },
  );
}
