import 'package:driver/core/widgets/navigation/nav_item.dart';
import 'package:driver/features/payments/domain/payment_status.dart';
import 'package:driver/features/payments/presentation/widgets/payment_history_tile.dart';
import 'package:flutter_test/flutter_test.dart';

import 'support/fakes.dart';
import 'support/phone_view.dart';

void main() {
  testWidgets(
    'the history totals the confirmed payments and filters the list',
    (tester) async {
      usePhone(tester);
      final confirmed = FakeBackend(
        finalStatus: PaymentStatus.success,
      ).finishedAttempt;
      final backend = FakeBackend(
        signedIn: true,
        history: [confirmed],
        finalStatus: PaymentStatus.success,
      );
      await tester.pumpWidget(appWith(backend));
      await tester.pumpAndSettle();
      await tester.tap(find.byType(NavItem).at(2));
      await tester.pumpAndSettle();

      expect(find.text('Total payé'), findsOneWidget);
      expect(find.text('paiement'), findsOneWidget);
      expect(find.byType(PaymentHistoryTile), findsOneWidget);

      await tester.tap(find.text('Échoués ou expirés'));
      await tester.pumpAndSettle();
      expect(find.byType(PaymentHistoryTile), findsNothing);
      expect(find.text('Aucun paiement dans cette catégorie'), findsOneWidget);

      await tester.tap(find.text('Confirmés'));
      await tester.pumpAndSettle();
      expect(find.byType(PaymentHistoryTile), findsOneWidget);
    },
  );
}
