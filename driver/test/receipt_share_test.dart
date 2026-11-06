import 'package:driver/core/widgets/navigation/nav_item.dart';
import 'package:driver/features/payments/domain/payment_status.dart';
import 'package:driver/features/payments/presentation/widgets/payment_history_tile.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

import 'support/fakes.dart';
import 'support/phone_view.dart';

void main() {
  Finder shareButton() =>
      find.widgetWithText(FilledButton, 'Partager le reçu').evaluate().isEmpty
      ? find.widgetWithText(OutlinedButton, 'Partager le reçu')
      : find.widgetWithText(FilledButton, 'Partager le reçu');

  Future<FakeBackend> payAndConfirm(WidgetTester tester) async {
    usePhone(tester);
    final backend = FakeBackend(signedIn: true);
    await tester.pumpWidget(appWith(backend));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Régulariser maintenant'));
    await tester.pumpAndSettle();
    await tester.pumpAndSettle();
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, 'Payer 71 415 FCFA'));
    await tester.pump(const Duration(milliseconds: 100));
    await tester.pump(const Duration(seconds: 3));
    await tester.pump(const Duration(milliseconds: 200));
    return backend;
  }

  testWidgets(
    'after a confirmed payment the driver can share the receipt PDF',
    (tester) async {
      final backend = await payAndConfirm(tester);
      expect(find.text('Paiement confirmé'), findsOneWidget);

      await tester.tap(shareButton());
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 100));

      verify(() => backend.payments.fetchReceipt('p1')).called(1);
      expect(backend.shared, hasLength(1));
      expect(backend.shared.single.filename, 'recu-NDZREF123456.pdf');
      expect(backend.shared.single.mimeType, 'application/pdf');
      expect(backend.shared.single.size, greaterThan(0));
    },
  );

  testWidgets(
    'a receipt that cannot be fetched shows the reason and shares nothing',
    (tester) async {
      final backend = await payAndConfirm(tester);
      when(
        () => backend.payments.fetchReceipt(any()),
      ).thenThrow(apiError(422, 'RECEIPT_NOT_AVAILABLE'));

      await tester.tap(shareButton());
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 200));

      expect(
        find.text("Le reçu n'est disponible que pour un paiement confirmé."),
        findsOneWidget,
      );
      expect(backend.shared, isEmpty);
    },
  );

  testWidgets('a failed payment offers no receipt', (tester) async {
    usePhone(tester);
    final backend = FakeBackend(
      signedIn: true,
      finalStatus: PaymentStatus.failed,
    );
    await tester.pumpWidget(appWith(backend));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Régulariser maintenant'));
    await tester.pumpAndSettle();
    await tester.pumpAndSettle();
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, 'Payer 71 415 FCFA'));
    await tester.pump(const Duration(milliseconds: 100));
    await tester.pump(const Duration(seconds: 3));
    await tester.pump(const Duration(milliseconds: 200));

    expect(find.text("Le paiement n'a pas abouti"), findsOneWidget);
    expect(find.text('Partager le reçu'), findsNothing);
  });

  testWidgets('the receipt can be shared again from the history details', (
    tester,
  ) async {
    usePhone(tester);
    final probe = FakeBackend();
    final backend = FakeBackend(
      signedIn: true,
      history: [probe.finishedAttempt],
    );
    await tester.pumpWidget(appWith(backend));
    await tester.pumpAndSettle();
    await tester.tap(find.byType(NavItem).at(2));
    await tester.pumpAndSettle();
    await tester.tap(find.byType(PaymentHistoryTile));
    await tester.pumpAndSettle();

    await tester.ensureVisible(shareButton());
    await tester.tap(shareButton());
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 100));

    expect(backend.shared.single.filename, 'recu-NDZREF123456.pdf');
  });
}
