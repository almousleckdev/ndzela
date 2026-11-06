import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

import 'support/fakes.dart';
import 'support/phone_view.dart';
import 'support/sample_data.dart';

void main() {
  Future<FakeBackend> openPay(
    WidgetTester tester, {
    double height = 4800,
  }) async {
    usePhone(tester, height: height);
    final backend = FakeBackend(signedIn: true);
    await tester.pumpWidget(appWith(backend));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Régulariser maintenant'));
    await tester.pumpAndSettle();
    return backend;
  }

  Finder payButton(String label) => find.widgetWithText(FilledButton, label);

  testWidgets('the operator appears from the number typed', (tester) async {
    await openPay(tester);

    expect(find.text('Airtel Money'), findsWidgets);
    expect(find.text('Moov Money'), findsNothing);

    await tester.enterText(find.byType(TextFormField), '062821855');
    await tester.pump(const Duration(seconds: 1));

    expect(find.text('Moov Money'), findsWidgets);
    expect(find.text('Airtel Money'), findsNothing);
    expect(find.byType(Image), findsOneWidget);

    await tester.enterText(find.byType(TextFormField), '05');
    await tester.pump(const Duration(seconds: 1));

    expect(find.text('Moov Money'), findsNothing);
    expect(find.text('Airtel Money'), findsNothing);
  });

  testWidgets('a disabled Payer button says what is missing', (tester) async {
    await openPay(tester);

    await tester.enterText(find.byType(TextFormField), '07');
    await tester.pump();
    expect(
      find.text('Saisissez un numéro valide (8 à 15 chiffres).'),
      findsOneWidget,
    );

    await tester.enterText(find.byType(TextFormField), '05123456');
    await tester.pump();
    expect(
      find.text('Ce numéro n’est ni Airtel (07) ni Moov (06).'),
      findsOneWidget,
    );
  });

  testWidgets(
    'when the fee cannot be calculated the summary says so and offers to retry',
    (tester) async {
      final backend = await openPay(tester);
      when(
        () => backend.payments.quote(
          operatorCode: any(named: 'operatorCode'),
          phone: any(named: 'phone'),
          days: any(named: 'days'),
        ),
      ).thenAnswer(
        (_) async => throw apiError(502, 'PAYMENT_PROVIDER_UNAVAILABLE'),
      );

      await tester.enterText(find.byType(TextFormField), '062821855');
    await tester.pump(const Duration(seconds: 1));
      await tester.pumpAndSettle();

      expect(
        find.text(
          'Le service de paiement est indisponible. Réessayez dans un instant.',
        ),
        findsOneWidget,
      );
      expect(tester.widget<FilledButton>(payButton('Payer')).onPressed, isNull);

      when(
        () => backend.payments.quote(
          operatorCode: any(named: 'operatorCode'),
          phone: any(named: 'phone'),
          days: any(named: 'days'),
        ),
      ).thenAnswer((_) async => sampleQuote);
      await tester.tap(find.text('Réessayer'));
      await tester.pumpAndSettle();

      expect(
        tester.widget<FilledButton>(payButton('Payer 71 415 FCFA')).onPressed,
        isNotNull,
      );
    },
  );

  testWidgets('tapping outside the number field closes the keyboard', (
    tester,
  ) async {
    await openPay(tester);

    await tester.showKeyboard(find.byType(TextFormField));
    expect(tester.testTextInput.isVisible, isTrue);

    await tester.tap(find.text('Récapitulatif'));
    await tester.pumpAndSettle();

    expect(tester.testTextInput.isVisible, isFalse);
  });

  testWidgets('scrolling the form closes the keyboard', (tester) async {
    await openPay(tester, height: 1500);

    await tester.showKeyboard(find.byType(TextFormField));
    expect(tester.testTextInput.isVisible, isTrue);

    await tester.drag(find.byType(ListView).first, const Offset(0, -80));
    await tester.pumpAndSettle();

    expect(tester.testTextInput.isVisible, isFalse);
  });
}
