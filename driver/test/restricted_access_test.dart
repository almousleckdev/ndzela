import 'package:driver/app/router.dart';
import 'package:driver/core/money/money.dart';
import 'package:driver/core/widgets/navigation/app_bottom_bar.dart';
import 'package:driver/features/auth/presentation/login_screen.dart';
import 'package:driver/features/contract/presentation/home_screen.dart';
import 'package:driver/features/payments/domain/arrears_summary.dart';
import 'package:driver/features/payments/presentation/payment_screen.dart';
import 'package:driver/features/support/presentation/support_message_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:mocktail/mocktail.dart';

import 'support/fakes.dart';
import 'support/phone_view.dart';

const _locked = ArrearsSummary(
  unpaidDays: 3,
  total: Money(69000, 'XAF'),
  locked: true,
);
const _suspendedAndPaid = ArrearsSummary(
  unpaidDays: 0,
  total: Money(0, 'XAF'),
  suspended: true,
);
const _clear = ArrearsSummary(unpaidDays: 0, total: Money(0, 'XAF'));

void main() {
  Future<FakeBackend> openApp(
    WidgetTester tester,
    ArrearsSummary arrears, {
    bool signedIn = true,
  }) async {
    usePhone(tester);
    final backend = FakeBackend(arrears: arrears, signedIn: signedIn);
    await tester.pumpWidget(appWith(backend));
    await tester.pumpAndSettle();
    return backend;
  }

  testWidgets(
    'a driver with three unpaid days lands on the payment screen, with no way to the other tabs',
    (tester) async {
      await openApp(tester, _locked);

      expect(find.byType(PaymentScreen), findsOneWidget);
      expect(find.byType(HomeScreen), findsNothing);
      expect(find.byType(AppBottomBar), findsNothing);
      expect(find.textContaining('3 jours de loyer impayés'), findsOneWidget);
      expect(
        find.text('Le paiement couvre la totalité de vos loyers en retard.'),
        findsOneWidget,
      );
    },
  );

  testWidgets(
    'a restricted driver cannot reach any other screen, not even by navigating',
    (tester) async {
      await openApp(tester, _locked);
      final router = GoRouter.of(tester.element(find.byType(PaymentScreen)));

      for (final route in [
        Routes.home,
        Routes.history,
        Routes.profile,
        Routes.paymentDetail('p1'),
      ]) {
        router.go(route);
        await tester.pumpAndSettle();
        expect(find.byType(PaymentScreen), findsOneWidget, reason: route);
      }
    },
  );

  testWidgets(
    'a locked driver can still write to support, then come back to the payment screen',
    (tester) async {
      await openApp(tester, _locked);
      final router = GoRouter.of(tester.element(find.byType(PaymentScreen)));

      router.push(Routes.supportMessage, extra: 'Bonjour');
      await tester.pumpAndSettle();
      expect(find.byType(SupportMessageScreen), findsOneWidget);

      await tester.tap(find.byTooltip('Retour'));
      await tester.pumpAndSettle();
      expect(find.byType(PaymentScreen), findsOneWidget);
    },
  );

  testWidgets(
    'signing in as a locked driver goes straight to the payment screen',
    (tester) async {
      final backend = FakeBackend(arrears: _locked);
      usePhone(tester);
      await tester.pumpWidget(appWith(backend));
      await tester.pumpAndSettle();
      expect(find.byType(LoginScreen), findsOneWidget);

      await tester.enterText(
        find.byType(TextFormField).at(0),
        'awa@ndzela.test',
      );
      await tester.enterText(find.byType(TextFormField).at(1), 'Secret123');
      await tester.tap(find.text('Se connecter'));
      await tester.pumpAndSettle();

      expect(find.byType(PaymentScreen), findsOneWidget);
      expect(find.byType(HomeScreen), findsNothing);
    },
  );

  testWidgets(
    'a restricted driver can still sign out from the payment screen',
    (tester) async {
      final backend = await openApp(tester, _locked);

      await tester.tap(find.byTooltip('Se déconnecter'));
      await tester.pumpAndSettle();
      expect(find.text('Se déconnecter ?'), findsOneWidget);

      await tester.tap(find.text('Annuler'));
      await tester.pumpAndSettle();
      verifyNever(() => backend.auth.signOut());
      expect(find.byType(PaymentScreen), findsOneWidget);

      await tester.tap(find.byTooltip('Se déconnecter'));
      await tester.pumpAndSettle();
      await tester.tap(
        find.descendant(
          of: find.byType(AlertDialog),
          matching: find.text('Se déconnecter'),
        ),
      );
      await tester.pumpAndSettle();

      verify(() => backend.auth.signOut()).called(1);
      expect(find.byType(LoginScreen), findsOneWidget);
    },
  );

  testWidgets('paying everything unlocks the app and brings the tabs back', (
    tester,
  ) async {
    final backend = await openApp(tester, _locked);
    await tester.tap(find.text('Moov Money'));
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, 'Payer 71 415 FCFA'));
    await tester.pump(const Duration(milliseconds: 100));
    when(() => backend.payments.fetchArrears()).thenAnswer((_) async => _clear);

    await tester.pump(const Duration(seconds: 3));
    await tester.pump(const Duration(milliseconds: 200));
    await tester.pumpAndSettle();

    expect(find.text('Paiement confirmé'), findsOneWidget);
    expect(find.byType(AppBottomBar), findsOneWidget);

    await tester.tap(find.text("Retour à l'accueil"));
    await tester.pumpAndSettle();
    expect(find.byType(HomeScreen), findsOneWidget);
  });

  testWidgets(
    'a suspended driver who has paid everything stays restricted and is told to contact Ndzela',
    (tester) async {
      await openApp(tester, _suspendedAndPaid);

      expect(find.byType(PaymentScreen), findsOneWidget);
      expect(find.byType(AppBottomBar), findsNothing);
      expect(
        find.text(
          'Votre compte est suspendu. Contactez Ndzela pour le réactiver.',
        ),
        findsOneWidget,
      );
      expect(find.text('Aucun loyer à régulariser'), findsOneWidget);
    },
  );

  testWidgets(
    'a driver under the limit is not restricted and may leave the payment screen',
    (tester) async {
      await openApp(
        tester,
        const ArrearsSummary(unpaidDays: 2, total: Money(46000, 'XAF')),
      );

      expect(find.byType(HomeScreen), findsOneWidget);
      expect(find.byType(AppBottomBar), findsOneWidget);
    },
  );
}
