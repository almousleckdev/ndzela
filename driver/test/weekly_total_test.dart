import 'package:driver/features/payments/domain/payment_status.dart';
import 'package:driver/features/payments/domain/weekly_total.dart';
import 'package:flutter_test/flutter_test.dart';

import 'support/fakes.dart';

void main() {
  test('confirmed payments are summed per week, newest week first', () {
    final paid = FakeBackend().finishedAttempt; // 69 000 on Thu 1 Oct 2026
    final failed = FakeBackend(
      finalStatus: PaymentStatus.failed,
    ).finishedAttempt;

    final weeks = WeeklyTotal.lastWeeks([paid, failed], DateTime(2026, 10, 8));

    expect(weeks, hasLength(4));
    expect(weeks[0].start, DateTime(2026, 10, 5));
    expect(weeks[0].paid.amount, 0);
    expect(weeks[1].start, DateTime(2026, 9, 28));
    expect(weeks[1].paid.amount, 69000);
    expect(weeks[1].payments, 1);
    expect(weeks[2].payments, 0);
  });
}
