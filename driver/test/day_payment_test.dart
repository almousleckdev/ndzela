import 'package:driver/core/money/money.dart';
import 'package:driver/core/widgets/navigation/nav_item.dart';
import 'package:driver/features/payments/domain/arrears_summary.dart';
import 'package:driver/features/payments/domain/paid_day.dart';
import 'package:driver/features/payments/domain/payment_attempt.dart';
import 'package:driver/features/payments/domain/payment_option.dart';
import 'package:driver/features/payments/domain/payment_request.dart';
import 'package:driver/features/payments/domain/payment_status.dart';
import 'package:driver/features/payments/presentation/payments_providers.dart';
import 'package:driver/features/payments/presentation/widgets/payment_form.dart';
import 'package:driver/features/payments/presentation/widgets/payment_history_tile.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

import 'support/fakes.dart';
import 'support/phone_view.dart';

const _rent = Money(23000, 'XAF');

PaymentOption _option(int days) =>
    PaymentOption(days: days, amount: Money(23000 * days, 'XAF'));

ArrearsSummary _owing(int days, List<PaymentOption> options) =>
    ArrearsSummary(unpaidDays: days, total: _rent * days, options: options);

// The outermost scrolling area of the open screen
final _scrollable = find.byType(Scrollable).first;

void main() {
  Future<FakeBackend> openPay(
    WidgetTester tester,
    ArrearsSummary arrears,
  ) async {
    usePhone(tester);
    final backend = FakeBackend(signedIn: true, arrears: arrears);
    await tester.pumpWidget(appWith(backend));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Régulariser maintenant'));
    await tester.pumpAndSettle();
    return backend;
  }

  testWidgets('with two unpaid days the driver can pay one or both', (
    tester,
  ) async {
    await openPay(tester, _owing(2, [_option(1), _option(2)]));

    expect(find.text('Jours à régler'), findsOneWidget);
    expect(find.text('1 jour'), findsOneWidget);
    expect(find.text('2 jours'), findsOneWidget);
    expect(find.text('23 000 FCFA'), findsOneWidget);
    expect(find.text('46 000 FCFA'), findsWidgets);
  });

  testWidgets('from three unpaid days all of them must be paid together', (
    tester,
  ) async {
    await openPay(tester, _owing(3, [_option(3)]));

    expect(find.text('Jours à régler'), findsNothing);
    expect(
      find.text('Vos 3 jours de loyer en retard se règlent ensemble.'),
      findsOneWidget,
    );
  });

  testWidgets('the form sends the number of days, never an amount', (
    tester,
  ) async {
    final backend = await openPay(tester, _owing(2, [_option(1), _option(2)]));

    await tester.tap(find.text('1 jour'));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextFormField), '062821855');
    await tester.pump(const Duration(seconds: 1));
    await tester.pumpAndSettle();
    verify(
      () => backend.payments.quote(
        operatorCode: 'MOOV_MONEY',
        phone: '062821855',
        days: 1,
      ),
    ).called(1);
    final pay = find.widgetWithText(FilledButton, 'Payer 71 415 FCFA');
    await tester.scrollUntilVisible(pay, 200, scrollable: _scrollable);
    await tester.tap(pay);
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 100));

    final request =
        verify(() => backend.payments.start(captureAny())).captured.single
            as PaymentRequest;
    expect(request.days, 1);
  });

  testWidgets('one unpaid day is explained in the singular', (tester) async {
    await openPay(tester, _owing(1, [_option(1)]));

    expect(
      find.text('Votre jour de loyer en retard se règle en une fois.'),
      findsOneWidget,
    );
  });

  testWidgets(
    'a day count the account no longer offers is dropped before paying',
    (tester) async {
      final backend = await openPay(
        tester,
        _owing(2, [_option(1), _option(2)]),
      );
      await tester.tap(find.text('1 jour'));
      await tester.pumpAndSettle();

      // A new day became late meanwhile: the only choice left is to pay all three.
      when(
        () => backend.payments.fetchArrears(),
      ).thenAnswer((_) async => _owing(3, [_option(3)]));
      ProviderScope.containerOf(
        tester.element(find.byType(PaymentForm)),
      ).invalidate(arrearsProvider);
      await tester.pumpAndSettle();

      await tester.enterText(find.byType(TextFormField), '062821855');
    await tester.pump(const Duration(seconds: 1));
      await tester.pumpAndSettle();
      final pay = find.widgetWithText(FilledButton, 'Payer 71 415 FCFA');
      await tester.scrollUntilVisible(pay, 200, scrollable: _scrollable);
      await tester.tap(pay);
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 100));

      final request =
          verify(() => backend.payments.start(captureAny())).captured.single
              as PaymentRequest;
      expect(request.days, isNull);
    },
  );

  testWidgets('a confirmed payment lists the days it settled and when', (
    tester,
  ) async {
    usePhone(tester);
    final paid = PaymentAttempt(
      id: 'p1',
      status: PaymentStatus.success,
      amount: _rent,
      operatorCode: 'MOOV_MONEY',
      phone: '074111111',
      reference: 'NDZREF123456',
      createdAt: DateTime(2026, 10, 15, 11, 3),
      completedAt: DateTime(2026, 10, 15, 11, 5),
      days: [
        PaidDay(day: DateTime(2026, 10, 13), amount: _rent, onTime: false),
      ],
    );
    final backend = FakeBackend(signedIn: true, history: [paid]);
    when(
      () => backend.payments.fetchAttempt(any()),
    ).thenAnswer((_) async => paid);
    await tester.pumpWidget(appWith(backend));
    await tester.pumpAndSettle();
    await tester.tap(find.byType(NavItem).at(2));
    await tester.pumpAndSettle();
    await tester.tap(find.byType(PaymentHistoryTile));
    await tester.pumpAndSettle();

    await tester.scrollUntilVisible(
      find.text('Loyer du 13 octobre 2026'),
      200,
      scrollable: _scrollable,
    );
    expect(find.text('Loyer du 13 octobre 2026'), findsOneWidget);
    expect(find.text('23 000 FCFA · en retard'), findsOneWidget);
    expect(find.text('Payé le'), findsOneWidget);
  });

  group('the week warning', () {
    final closes = DateTime(2026, 10, 18, 12);
    final unsettled = ArrearsSummary(
      unpaidDays: 1,
      total: _rent,
      weekClosesAt: closes,
      weekSettled: false,
    );

    test('appears from Friday when the week is not settled', () {
      expect(unsettled.showsWeekWarning(DateTime(2026, 10, 16, 9)), isTrue);
      expect(unsettled.showsWeekWarning(DateTime(2026, 10, 17, 9)), isTrue);
      expect(unsettled.showsWeekWarning(DateTime(2026, 10, 18, 9)), isTrue);
    });

    test('stays quiet earlier in the week', () {
      expect(unsettled.showsWeekWarning(DateTime(2026, 10, 15, 9)), isFalse);
    });

    test('stays quiet once the week is settled', () {
      final settled = ArrearsSummary(
        unpaidDays: 0,
        total: const Money(0, 'XAF'),
        weekClosesAt: closes,
      );
      expect(settled.showsWeekWarning(DateTime(2026, 10, 17, 9)), isFalse);
    });
  });
}
