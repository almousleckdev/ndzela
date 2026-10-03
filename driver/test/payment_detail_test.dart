import 'package:driver/core/widgets/navigation/nav_item.dart';
import 'package:driver/features/payments/domain/payment_status.dart';
import 'package:driver/features/payments/presentation/payment_detail_screen.dart';
import 'package:driver/features/payments/presentation/widgets/payment_history_tile.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

import 'support/fakes.dart';
import 'support/phone_view.dart';

void main() {
  Future<FakeBackend> openFirstPaymentOfHistory(
    WidgetTester tester,
    PaymentStatus status,
  ) async {
    usePhone(tester);
    final probe = FakeBackend(finalStatus: status);
    final backend = FakeBackend(
      signedIn: true,
      history: [probe.finishedAttempt],
      finalStatus: status,
    );
    await tester.pumpWidget(appWith(backend));
    await tester.pumpAndSettle();
    await tester.tap(find.byType(NavItem).at(2));
    await tester.pumpAndSettle();
    await tester.tap(find.byType(PaymentHistoryTile));
    await tester.pumpAndSettle();
    return backend;
  }

  testWidgets(
    'tapping a confirmed payment shows what was paid, the fee, the total and the references',
    (tester) async {
      await openFirstPaymentOfHistory(tester, PaymentStatus.success);

      expect(find.byType(PaymentDetailScreen), findsOneWidget);
      expect(find.text('Détails du paiement'), findsOneWidget);
      expect(find.text('Confirmé'), findsOneWidget);
      expect(find.text('Moov Money'), findsOneWidget);
      expect(find.text('074111111'), findsOneWidget);
      expect(find.text('2 415 FCFA'), findsOneWidget);
      expect(find.text('Total payé'), findsOneWidget);
      expect(find.text('71 415 FCFA'), findsOneWidget);
      expect(find.text('NDZREF123456'), findsOneWidget);
      expect(find.text('PAY0110261325109'), findsOneWidget);
      expect(find.text('Confirmé le'), findsOneWidget);
      expect(find.text('Expéditeur'), findsOneWidget);
      expect(find.text('Awa Nze'), findsOneWidget);
      expect(find.text('Bénéficiaire'), findsOneWidget);
      expect(find.text('NDZELA GABON'), findsOneWidget);
    },
  );

  testWidgets(
    'a failed payment says nothing was recorded and shows no total or fee',
    (tester) async {
      await openFirstPaymentOfHistory(tester, PaymentStatus.failed);

      expect(find.text('Échoué'), findsOneWidget);
      expect(find.text('Contacter le support'), findsOneWidget);
      expect(
        find.textContaining("aucun loyer n'a été enregistré"),
        findsOneWidget,
      );
      expect(find.text('Total payé'), findsNothing);
      expect(find.text('Frais opérateur'), findsNothing);
      expect(find.text('NDZREF123456'), findsOneWidget);
    },
  );

  testWidgets(
    'the details screen hides the bottom bar and the back button returns to the history',
    (tester) async {
      await openFirstPaymentOfHistory(tester, PaymentStatus.success);
      expect(find.byType(NavItem), findsNothing);

      await tester.tap(find.byTooltip('Retour'));
      await tester.pumpAndSettle();

      expect(find.byType(PaymentDetailScreen), findsNothing);
      expect(find.byType(PaymentHistoryTile), findsOneWidget);
      expect(find.byType(NavItem), findsNWidgets(4));
    },
  );

  testWidgets('the details are read again from the server', (tester) async {
    final backend = await openFirstPaymentOfHistory(
      tester,
      PaymentStatus.success,
    );

    verify(() => backend.payments.fetchAttempt('p1')).called(1);
  });
}
