import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:intl/date_symbol_data_local.dart';

Future<void> _loadFont(String family, String asset) async {
  final loader = FontLoader(family)..addFont(rootBundle.load(asset));
  await loader.load();
}

// Tests render with the app's real fonts so text widths match the device
Future<void> testExecutable(FutureOr<void> Function() testMain) async {
  TestWidgetsFlutterBinding.ensureInitialized();
  await initializeDateFormatting('fr');
  await _loadFont('IBMPlexSans', 'assets/fonts/IBMPlexSans.ttf');
  await _loadFont('BricolageGrotesque', 'assets/fonts/BricolageGrotesque.ttf');
  // The road decorations loop forever; tests ask for no animation so pumpAndSettle can finish.
  setUp(() {
    TestWidgetsFlutterBinding
        .instance
        .platformDispatcher
        .accessibilityFeaturesTestValue = const FakeAccessibilityFeatures(
      disableAnimations: true,
    );
  });
  await testMain();
}
