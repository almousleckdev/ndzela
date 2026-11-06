import 'package:driver/app/theme/app_theme.dart';
import 'package:driver/core/widgets/feedback/splash_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'support/phone_view.dart';

void main() {
  testWidgets(
    'the splash screen shows the name and a skeleton on a small phone',
    (tester) async {
      usePhone(tester, height: 1700);

      await tester.pumpWidget(
        MaterialApp(theme: AppTheme.light(), home: const SplashScreen()),
      );
      await tester.pump(const Duration(milliseconds: 900));

      expect(find.text('NdZela'), findsOneWidget);
      expect(find.text('Votre loyer, simplement.'), findsOneWidget);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets('the screens are plain white, with no grey page background', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(theme: AppTheme.light(), home: const Scaffold()),
    );

    expect(
      Theme.of(tester.element(find.byType(Scaffold))).scaffoldBackgroundColor,
      Colors.white,
    );
  });
}
