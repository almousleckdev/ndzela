import 'package:driver/core/money/money.dart';
import 'package:driver/features/payments/domain/rent_day.dart';
import 'package:driver/features/payments/domain/week_days.dart';
import 'package:flutter_test/flutter_test.dart';

RentDay day(int dayOfMonth, RentDayStatus status) => RentDay(
  date: DateTime(2026, 11, dayOfMonth),
  status: status,
  amount: const Money(23000, 'XAF'),
);

void main() {
  // Friday 6 November 2026
  final now = DateTime(2026, 11, 6, 15);

  test(
    'a week lists Monday to Saturday in order, leaving empty the days with no rent',
    () {
      final weeks = WeekDays.lastWeeks([
        day(3, RentDayStatus.unpaid),
        day(2, RentDayStatus.paidOnTime),
        day(5, RentDayStatus.paidLate),
      ], now);

      final current = weeks.first;
      expect(current.start, DateTime(2026, 11, 2));
      expect(current.days.length, 6);
      expect(current.days.map((d) => d?.status).toList(), [
        RentDayStatus.paidOnTime,
        RentDayStatus.unpaid,
        null,
        RentDayStatus.paidLate,
        null,
        null,
      ]);
    },
  );

  test('the weeks come newest first and each keeps its own days', () {
    final weeks = WeekDays.lastWeeks([
      day(2, RentDayStatus.paidOnTime),
      RentDay(
        date: DateTime(2026, 10, 27),
        status: RentDayStatus.unpaid,
        amount: const Money(23000, 'XAF'),
      ),
    ], now);

    expect(weeks.length, 4);
    expect(weeks.map((w) => w.start).toList(), [
      DateTime(2026, 11, 2),
      DateTime(2026, 10, 26),
      DateTime(2026, 10, 19),
      DateTime(2026, 10, 12),
    ]);
    expect(weeks[1].days[1]?.status, RentDayStatus.unpaid);
  });

  test('the week counts its paid, unpaid and awaiting days', () {
    final week = WeekDays.lastWeeks([
      day(2, RentDayStatus.paidOnTime),
      day(3, RentDayStatus.paidLate),
      day(4, RentDayStatus.unpaid),
      day(5, RentDayStatus.awaiting),
    ], now).first;

    expect(week.paid, 2);
    expect(week.unpaid, 1);
    expect(week.awaiting, 1);
  });

  test('a day that is not a rent day of the window is ignored', () {
    final weeks = WeekDays.lastWeeks([
      RentDay(
        date: DateTime(2026, 8, 1),
        status: RentDayStatus.unpaid,
        amount: const Money(23000, 'XAF'),
      ),
    ], now);

    expect(weeks.every((w) => w.days.every((d) => d == null)), isTrue);
  });
}
