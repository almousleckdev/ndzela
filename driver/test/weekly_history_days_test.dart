import 'package:driver/core/money/money.dart';
import 'package:driver/features/payments/domain/arrears_summary.dart';
import 'package:driver/features/payments/domain/rent_day.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'support/fakes.dart';
import 'support/phone_view.dart';

Finder cell(RegExp label) => find.byWidgetPredicate(
  (widget) =>
      widget is Semantics && label.hasMatch(widget.properties.label ?? ''),
);

void main() {
  final now = DateTime.now();
  final thisMonday = DateTime(now.year, now.month, now.day - (now.weekday - 1));
  final lastMonday = thisMonday.subtract(const Duration(days: 7));

  RentDay day(int offset, RentDayStatus status) => RentDay(
    date: lastMonday.add(Duration(days: offset)),
    status: status,
    amount: const Money(23000, 'XAF'),
  );

  Future<void> openHome(WidgetTester tester, List<RentDay> days) async {
    usePhone(tester);
    final backend = FakeBackend(
      signedIn: true,
      arrears: ArrearsSummary(
        unpaidDays: 1,
        total: const Money(23000, 'XAF'),
        days: days,
      ),
    );
    await tester.pumpWidget(appWith(backend));
    await tester.pumpAndSettle();
  }

  testWidgets(
    'the weekly history shows each rent day of the week as paid or unpaid, in order',
    (tester) async {
      await openHome(tester, [
        day(0, RentDayStatus.paidOnTime),
        day(1, RentDayStatus.paidLate),
        day(2, RentDayStatus.unpaid),
        day(3, RentDayStatus.awaiting),
      ]);

      expect(find.text('Historique hebdomadaire'), findsOneWidget);
      expect(cell(RegExp(r'^Lun\b.*: payé$')), findsOneWidget);
      expect(cell(RegExp(r'^Mar\b.*: payé en retard$')), findsOneWidget);
      expect(cell(RegExp(r'^Mer\b.*: impayé$')), findsOneWidget);
      expect(cell(RegExp(r'^Jeu\b.*: à régler$')), findsOneWidget);
    },
  );

  testWidgets('a week counts what is paid, unpaid and still to settle', (
    tester,
  ) async {
    await openHome(tester, [
      day(0, RentDayStatus.paidOnTime),
      day(1, RentDayStatus.paidLate),
      day(2, RentDayStatus.unpaid),
      day(3, RentDayStatus.awaiting),
    ]);

    expect(find.text('2 payés · 1 impayé · 1 à régler'), findsOneWidget);
  });

  testWidgets('the days of a week appear in the order of the week', (
    tester,
  ) async {
    await openHome(tester, [
      day(2, RentDayStatus.unpaid),
      day(0, RentDayStatus.paidOnTime),
      day(1, RentDayStatus.paidOnTime),
    ]);

    final monday = tester.getTopLeft(cell(RegExp(r'^Lun\b')).at(1)).dx;
    final tuesday = tester.getTopLeft(cell(RegExp(r'^Mar\b')).at(1)).dx;
    final wednesday = tester.getTopLeft(cell(RegExp(r'^Mer\b')).at(1)).dx;

    expect(monday < tuesday, isTrue);
    expect(tuesday < wednesday, isTrue);
  });
}
