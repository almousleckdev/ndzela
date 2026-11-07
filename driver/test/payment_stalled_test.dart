import 'package:driver/features/payments/domain/payment_status.dart';
import 'package:driver/features/payments/presentation/payment_flow_controller.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

import 'support/fakes.dart';
import 'support/phone_view.dart';

void main() {
  Future<FakeBackend> payAndWait(WidgetTester tester) async {
    usePhone(tester);
    final backend = FakeBackend(
      signedIn: true,
      finalStatus: PaymentStatus.pending,
    );
    await tester.pumpWidget(
      appWith(
        backend,
        overrides: [
          paymentGiveUpAfterProvider.overrideWithValue(Duration.zero),
        ],
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text('Régulariser maintenant'));
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, 'Payer 71 415 FCFA'));
    await tester.pump();
    await tester.pump(const Duration(seconds: 3));
    await tester.pump(const Duration(milliseconds: 100));
    return backend;
  }

  testWidgets(
    'when the operator never answers the driver is told, not left on a spinner',
    (tester) async {
      await payAndWait(tester);

      expect(find.text('Confirmation en attente'), findsOneWidget);
      expect(find.text('Validez sur votre téléphone'), findsNothing);
    },
  );

  testWidgets(
    'checking again shows the confirmation once the operator has answered',
    (tester) async {
      final backend = await payAndWait(tester);
      when(
        () => backend.payments.fetchAttempt(any()),
      ).thenAnswer((_) async => FakeBackend(signedIn: true).finishedAttempt);

      await tester.tap(find.text('Vérifier maintenant'));
      await tester.pump(const Duration(milliseconds: 100));
      await tester.pump(const Duration(milliseconds: 100));

      expect(find.text('Paiement confirmé'), findsOneWidget);
    },
  );

  testWidgets('the driver can leave the waiting screen', (tester) async {
    await payAndWait(tester);

    await tester.tap(find.text('Retour'));
    await tester.pumpAndSettle();

    expect(find.text('Confirmation en attente'), findsNothing);
  });
}
