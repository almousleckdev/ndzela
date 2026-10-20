import 'package:driver/core/money/money.dart';
import 'package:driver/core/widgets/navigation/nav_item.dart';
import 'package:driver/features/payments/domain/arrears_summary.dart';
import 'package:flutter_test/flutter_test.dart';

import 'support/fakes.dart';
import 'support/phone_view.dart';

void main() {
  Future<FakeBackend> openHome(
    WidgetTester tester, {
    ArrearsSummary? arrears,
  }) async {
    usePhone(tester);
    final backend = arrears == null
        ? FakeBackend(signedIn: true)
        : FakeBackend(arrears: arrears, signedIn: true);
    await tester.pumpWidget(appWith(backend));
    await tester.pumpAndSettle();
    return backend;
  }

  testWidgets('home shows what the server returned', (tester) async {
    await openHome(tester);

    expect(find.text('Awa Nze'), findsOneWidget);
    expect(find.text('Toyota Yaris'), findsOneWidget);
    expect(find.text('AB-123-CD'), findsOneWidget);
    expect(find.text('En retard'), findsOneWidget);
    expect(find.text('69 000 FCFA'), findsOneWidget);
    expect(find.text('NDZ-77836447'), findsOneWidget);
    expect(find.text('23 000 FCFA'), findsOneWidget);
  });

  testWidgets('home shows the contract days and money since day one', (
    tester,
  ) async {
    await openHome(tester);

    expect(find.text('10 sur 12'), findsOneWidget);
    expect(find.text('1236 jours'), findsOneWidget);
    expect(
      find.text('Jour 12 sur 1248 · reste à régler 28 474 000 FCFA'),
      findsOneWidget,
    );
  });

  testWidgets('a driver who is up to date sees no regularise button', (
    tester,
  ) async {
    await openHome(
      tester,
      arrears: const ArrearsSummary(unpaidDays: 0, total: Money(0, 'XAF')),
    );

    expect(find.text('À jour'), findsOneWidget);
    expect(find.text('Régulariser maintenant'), findsNothing);
  });

  testWidgets(
    'the bottom bar shows the four tabs, all labelled, and marks the selected one',
    (tester) async {
      await openHome(tester);

      final items = tester.widgetList<NavItem>(find.byType(NavItem)).toList();
      expect(items.map((item) => item.tab.label), [
        'Accueil',
        'Payer',
        'Historique',
        'Profil',
      ]);
      for (final label in ['Accueil', 'Payer', 'Historique', 'Profil']) {
        expect(
          find.descendant(of: find.byType(NavItem), matching: find.text(label)),
          findsOneWidget,
        );
      }
      expect(items.map((item) => item.selected), [true, false, false, false]);

      await tester.tap(find.byType(NavItem).at(1));
      await tester.pumpAndSettle();
      expect(
        tester
            .widgetList<NavItem>(find.byType(NavItem))
            .map((item) => item.selected),
        [false, true, false, false],
      );
    },
  );
}
