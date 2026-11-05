import 'package:driver/app/router.dart';
import 'package:driver/features/support/presentation/support_message_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

// The message to support is reached from the payment screens; tests go straight to it.
Future<void> openMessageScreen(WidgetTester tester) async {
  GoRouter.of(
    tester.element(find.byType(Scaffold).first),
  ).push(Routes.supportMessage);
  await tester.pumpAndSettle();
  expect(find.byType(SupportMessageScreen), findsOneWidget);
}
