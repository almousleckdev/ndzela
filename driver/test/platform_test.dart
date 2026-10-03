import 'package:driver/core/widgets/navigation/app_bottom_bar.dart';
import 'package:driver/core/widgets/navigation/nav_item.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'support/fakes.dart';
import 'support/phone_view.dart';

void main() {
  testWidgets(
    'on iOS the login screen uses Cupertino controls and no Material ones',
    (tester) async {
      usePhone(tester);
      await tester.pumpWidget(appWith(FakeBackend()));
      await tester.pumpAndSettle();

      expect(find.byType(CupertinoTextField), findsNWidgets(2));
      expect(find.byType(CupertinoButton), findsWidgets);
      expect(find.byType(TextFormField), findsNothing);
      expect(find.byType(FilledButton), findsNothing);
    },
    variant: TargetPlatformVariant.only(TargetPlatform.iOS),
  );

  testWidgets(
    'on iOS the dock has no Material bar and the logout dialog is a Cupertino alert',
    (tester) async {
      usePhone(tester, height: 7000);
      await tester.pumpWidget(appWith(FakeBackend(signedIn: true)));
      await tester.pumpAndSettle();

      expect(find.byType(AppBottomBar), findsOneWidget);
      expect(find.byType(NavigationBar), findsNothing);
      expect(find.byType(CupertinoTabBar), findsNothing);

      await tester.tap(find.byType(NavItem).at(3));
      await tester.pumpAndSettle();
      await tester.tap(find.widgetWithText(CupertinoButton, 'Se déconnecter'));
      await tester.pumpAndSettle();

      expect(find.byType(CupertinoAlertDialog), findsOneWidget);
      expect(find.byType(AlertDialog), findsNothing);
    },
    variant: TargetPlatformVariant.only(TargetPlatform.iOS),
  );

  testWidgets(
    'on iOS the message screen uses a Cupertino field and button',
    (tester) async {
      usePhone(tester, height: 7000);
      await tester.pumpWidget(appWith(FakeBackend(signedIn: true)));
      await tester.pumpAndSettle();
      await tester.tap(find.byType(NavItem).at(3));
      await tester.pumpAndSettle();
      await tester.tap(
        find.widgetWithText(CupertinoButton, 'Envoyer un message'),
      );
      await tester.pumpAndSettle();

      expect(find.byType(CupertinoTextField), findsOneWidget);
      expect(find.widgetWithText(CupertinoButton, 'Envoyer'), findsOneWidget);
      expect(find.byType(TextFormField), findsNothing);
    },
    variant: TargetPlatformVariant.only(TargetPlatform.iOS),
  );

  testWidgets(
    'on Android the dock is the same and the dialog is a Material alert',
    (tester) async {
      usePhone(tester);
      await tester.pumpWidget(appWith(FakeBackend(signedIn: true)));
      await tester.pumpAndSettle();

      expect(find.byType(AppBottomBar), findsOneWidget);
      expect(find.byType(NavigationBar), findsNothing);
      expect(find.byType(CupertinoTabBar), findsNothing);
    },
  );
}
