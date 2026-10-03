import 'package:driver/features/auth/presentation/login_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

import 'support/fakes.dart';
import 'support/phone_view.dart';

void main() {
  testWidgets('without a stored token the driver lands on the login screen', (
    tester,
  ) async {
    usePhone(tester);
    await tester.pumpWidget(appWith(FakeBackend()));
    await tester.pumpAndSettle();
    expect(find.byType(LoginScreen), findsOneWidget);
  });

  testWidgets(
    'empty credentials show the field errors and do not call the server',
    (tester) async {
      usePhone(tester);
      final backend = FakeBackend();
      await tester.pumpWidget(appWith(backend));
      await tester.pumpAndSettle();

      await tester.tap(find.text('Se connecter'));
      await tester.pumpAndSettle();

      expect(
        find.text('Saisissez votre téléphone ou votre e-mail.'),
        findsOneWidget,
      );
      expect(find.text('Saisissez votre mot de passe.'), findsOneWidget);
      verifyNever(
        () => backend.auth.signIn(
          identifier: any(named: 'identifier'),
          password: any(named: 'password'),
        ),
      );
    },
  );

  testWidgets(
    'valid credentials sign in through the server and open the home screen',
    (tester) async {
      usePhone(tester);
      final backend = FakeBackend();
      await tester.pumpWidget(appWith(backend));
      await tester.pumpAndSettle();

      await tester.enterText(find.byType(TextFormField).at(0), ' 074111111 ');
      await tester.enterText(find.byType(TextFormField).at(1), 'Secret123');
      await tester.tap(find.text('Se connecter'));
      await tester.pumpAndSettle();

      verify(
        () =>
            backend.auth.signIn(identifier: '074111111', password: 'Secret123'),
      ).called(1);
      expect(find.text('Bonjour,'), findsOneWidget);
      expect(find.text('Awa Nze'), findsOneWidget);
    },
  );

  testWidgets(
    'wrong credentials show the French message from the server code',
    (tester) async {
      usePhone(tester);
      final backend = FakeBackend();
      when(
        () => backend.auth.signIn(
          identifier: any(named: 'identifier'),
          password: any(named: 'password'),
        ),
      ).thenThrow(apiError(401, 'INVALID_CREDENTIALS'));
      await tester.pumpWidget(appWith(backend));
      await tester.pumpAndSettle();

      await tester.enterText(find.byType(TextFormField).at(0), 'x@y.z');
      await tester.enterText(find.byType(TextFormField).at(1), 'bad');
      await tester.tap(find.text('Se connecter'));
      await tester.pumpAndSettle();

      expect(
        find.text('Identifiant ou mot de passe incorrect.'),
        findsOneWidget,
      );
      expect(find.byType(LoginScreen), findsOneWidget);
    },
  );
}
