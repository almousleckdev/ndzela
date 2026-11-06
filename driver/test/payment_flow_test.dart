import 'package:driver/features/payments/domain/payment_request.dart';
import 'package:driver/features/payments/domain/payment_status.dart';
import 'package:driver/features/support/presentation/support_message_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

import 'support/fakes.dart';
import 'support/phone_view.dart';

void main() {
  Future<FakeBackend> openPay(
    WidgetTester tester, {
    PaymentStatus finalStatus = PaymentStatus.success,
  }) async {
    usePhone(tester);
    final backend = FakeBackend(signedIn: true, finalStatus: finalStatus);
    await tester.pumpWidget(appWith(backend));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Régulariser maintenant'));
    await tester.pumpAndSettle();
    return backend;
  }

  Finder payButton(String label) => find.widgetWithText(FilledButton, label);

  Future<void> payWithTheNumberOnFile(WidgetTester tester) async {
    await tester.pumpAndSettle();
    await tester.tap(payButton('Payer 71 415 FCFA'));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 100));
  }

  testWidgets(
    'the summary shows the amount, the operator fee on top and the total once the number gives an operator',
    (tester) async {
      await openPay(tester);
      await tester.pumpAndSettle();

      expect(find.text('Frais estimés'), findsOneWidget);
      expect(find.text('2 415 FCFA'), findsOneWidget);
      expect(find.text('71 415 FCFA'), findsOneWidget);
      expect(find.text('Awa Nze'), findsWidgets);
      expect(
        tester.widget<FilledButton>(payButton('Payer 71 415 FCFA')).onPressed,
        isNotNull,
      );
    },
  );

  testWidgets(
    'the form starts from the local number and the quote follows the number only after the driver stops typing',
    (tester) async {
      final backend = await openPay(tester);
      expect(find.widgetWithText(TextFormField, '074111111'), findsOneWidget);

      await tester.pumpAndSettle();
      verify(
        () => backend.payments.quote(
          operatorCode: 'AIRTEL_MONEY',
          phone: '074111111',
        ),
      ).called(1);

      await tester.enterText(find.byType(TextFormField), '0775');
      await tester.enterText(find.byType(TextFormField), '07756715');
      await tester.enterText(find.byType(TextFormField), '077567157');
      await tester.pump(const Duration(milliseconds: 100));
      verifyNever(
        () => backend.payments.quote(
          operatorCode: any(named: 'operatorCode'),
          phone: '07756715',
        ),
      );
      expect(
        tester.widget<FilledButton>(payButton('Payer 71 415 FCFA')).onPressed,
        isNull,
      );

      await tester.pump(const Duration(milliseconds: 700));
      await tester.pumpAndSettle();
      verify(
        () => backend.payments.quote(
          operatorCode: 'AIRTEL_MONEY',
          phone: '077567157',
        ),
      ).called(1);
      verifyNever(
        () => backend.payments.quote(
          operatorCode: any(named: 'operatorCode'),
          phone: '07756715',
        ),
      );
      expect(payButton('Payer 71 415 FCFA'), findsOneWidget);
    },
  );

  testWidgets(
    'paying starts the payment on the server and waits for the phone',
    (tester) async {
      final backend = await openPay(tester);

      await payWithTheNumberOnFile(tester);

      final request =
          verify(() => backend.payments.start(captureAny())).captured.single
              as PaymentRequest;
      expect(request.operator.code, 'AIRTEL_MONEY');
      expect(request.phone, '074111111');
      expect(find.text('Validez sur votre téléphone'), findsOneWidget);
      expect(find.text('Régulariser maintenant'), findsNothing);
    },
  );

  testWidgets(
    'when the operator confirms, the driver sees the confirmation with the fee and goes home',
    (tester) async {
      await openPay(tester);
      await payWithTheNumberOnFile(tester);

      await tester.pump(const Duration(seconds: 3));
      await tester.pump(const Duration(milliseconds: 100));

      expect(find.text('Paiement confirmé'), findsOneWidget);
      expect(find.text('2 415 FCFA'), findsOneWidget);
      // The receipt: who paid whom, when, how much in all, and the references.
      for (final text in [
        'Date et heure',
        'Confirmé le',
        'Expéditeur',
        'Awa Nze',
        '074111111',
        'Moov Money',
        'Bénéficiaire',
        'NDZELA GABON',
        'Total payé',
        '71 415 FCFA',
        'NDZREF123456',
        'PAY0110261325109',
      ]) {
        expect(find.text(text), findsOneWidget, reason: text);
      }

      await tester.tap(find.text("Retour à l'accueil"));
      await tester.pumpAndSettle();
      expect(find.text('Bonjour,'), findsOneWidget);
    },
  );

  testWidgets('a refused payment says so and lets the driver try again', (
    tester,
  ) async {
    await openPay(tester, finalStatus: PaymentStatus.failed);
    await payWithTheNumberOnFile(tester);

    await tester.pump(const Duration(seconds: 3));
    await tester.pump(const Duration(milliseconds: 100));

    expect(find.text("Le paiement n'a pas abouti"), findsOneWidget);
    expect(find.text('Paiement confirmé'), findsNothing);

    await tester.tap(find.text('Réessayer'));
    await tester.pumpAndSettle();
    expect(find.text('Récapitulatif'), findsOneWidget);
  });

  testWidgets(
    'a failed payment lists the usual causes and offers support with the reference already written',
    (tester) async {
      await openPay(tester, finalStatus: PaymentStatus.failed);
      await payWithTheNumberOnFile(tester);
      await tester.pump(const Duration(seconds: 3));
      await tester.pump(const Duration(milliseconds: 100));

      expect(find.textContaining('Causes possibles'), findsOneWidget);
      expect(
        find.textContaining('solde Mobile Money insuffisant'),
        findsOneWidget,
      );
      expect(find.text('Réessayer'), findsOneWidget);

      await tester.tap(find.text('Contacter le support'));
      await tester.pumpAndSettle();

      expect(find.byType(SupportMessageScreen), findsOneWidget);
      expect(find.textContaining('NDZREF123456'), findsOneWidget);
    },
  );

  testWidgets(
    'an expired payment explains that a debit will still be recorded',
    (tester) async {
      await openPay(tester, finalStatus: PaymentStatus.expired);
      await payWithTheNumberOnFile(tester);

      await tester.pump(const Duration(seconds: 3));
      await tester.pump(const Duration(milliseconds: 100));

      expect(
        find.textContaining('sera enregistré automatiquement'),
        findsOneWidget,
      );
    },
  );

  testWidgets(
    'the server refusing the payment shows its reason and keeps the form',
    (tester) async {
      final backend = await openPay(tester);
      when(
        () => backend.payments.start(any()),
      ).thenThrow(apiError(422, 'PAYMENT_IN_PROGRESS'));

      await tester.pumpAndSettle();
      await tester.tap(payButton('Payer 71 415 FCFA'));
      await tester.pumpAndSettle();

      expect(
        find.text('Un paiement est déjà en attente de confirmation.'),
        findsOneWidget,
      );
      expect(find.text('Récapitulatif'), findsOneWidget);
    },
  );
}
