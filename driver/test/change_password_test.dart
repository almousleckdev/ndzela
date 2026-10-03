import 'package:driver/features/auth/presentation/change_password_screen.dart';
import 'package:driver/features/auth/presentation/login_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

import 'support/fakes.dart';
import 'support/phone_view.dart';

void main() {
  testWidgets(
    'a first login goes to the password change before the home screen',
    (tester) async {
      usePhone(tester);
      final backend = FakeBackend(mustChangePassword: true);
      await tester.pumpWidget(appWith(backend));
      await tester.pumpAndSettle();

      await tester.enterText(
        find.byType(TextFormField).at(0),
        'awa@ndzela.test',
      );
      await tester.enterText(find.byType(TextFormField).at(1), 'Initial123');
      await tester.tap(find.text('Se connecter'));
      await tester.pumpAndSettle();

      expect(find.byType(ChangePasswordScreen), findsOneWidget);
      expect(find.text('Bonjour,'), findsNothing);
    },
  );

  testWidgets(
    'a stored session that still needs a password change resumes on that screen',
    (tester) async {
      usePhone(tester);
      await tester.pumpWidget(
        appWith(FakeBackend(signedIn: true, mustChangePassword: true)),
      );
      await tester.pumpAndSettle();

      expect(find.byType(ChangePasswordScreen), findsOneWidget);
    },
  );

  testWidgets(
    'the form checks the length and the confirmation before calling the server',
    (tester) async {
      usePhone(tester);
      final backend = FakeBackend(signedIn: true, mustChangePassword: true);
      await tester.pumpWidget(appWith(backend));
      await tester.pumpAndSettle();

      final fields = find.byType(TextFormField);
      await tester.enterText(fields.at(0), 'Initial123');
      await tester.enterText(fields.at(1), 'short');
      await tester.enterText(fields.at(2), 'different');
      await tester.tap(find.widgetWithText(FilledButton, 'Enregistrer'));
      await tester.pumpAndSettle();

      expect(find.text('Au moins 8 caractères.'), findsOneWidget);
      expect(
        find.text('Les mots de passe ne correspondent pas.'),
        findsOneWidget,
      );
      verifyNever(
        () => backend.auth.changePassword(
          currentPassword: any(named: 'currentPassword'),
          newPassword: any(named: 'newPassword'),
        ),
      );
    },
  );

  testWidgets('a valid new password is sent and opens the home screen', (
    tester,
  ) async {
    usePhone(tester);
    final backend = FakeBackend(signedIn: true, mustChangePassword: true);
    await tester.pumpWidget(appWith(backend));
    await tester.pumpAndSettle();

    final fields = find.byType(TextFormField);
    await tester.enterText(fields.at(0), 'Initial123');
    await tester.enterText(fields.at(1), 'Another123');
    await tester.enterText(fields.at(2), 'Another123');
    await tester.tap(find.widgetWithText(FilledButton, 'Enregistrer'));
    await tester.pumpAndSettle();

    verify(
      () => backend.auth.changePassword(
        currentPassword: 'Initial123',
        newPassword: 'Another123',
      ),
    ).called(1);
    expect(find.text('Bonjour,'), findsOneWidget);
  });

  testWidgets(
    'the server refusing the current password keeps the driver on the form with a message',
    (tester) async {
      usePhone(tester);
      final backend = FakeBackend(signedIn: true, mustChangePassword: true);
      when(
        () => backend.auth.changePassword(
          currentPassword: any(named: 'currentPassword'),
          newPassword: any(named: 'newPassword'),
        ),
      ).thenThrow(apiError(422, 'PASSWORD_INCORRECT'));
      await tester.pumpWidget(appWith(backend));
      await tester.pumpAndSettle();

      final fields = find.byType(TextFormField);
      await tester.enterText(fields.at(0), 'wrong');
      await tester.enterText(fields.at(1), 'Another123');
      await tester.enterText(fields.at(2), 'Another123');
      await tester.tap(find.widgetWithText(FilledButton, 'Enregistrer'));
      await tester.pumpAndSettle();

      expect(find.text('Mot de passe actuel incorrect.'), findsOneWidget);
      expect(find.byType(ChangePasswordScreen), findsOneWidget);
    },
  );

  testWidgets('the driver can sign out from the password screen', (
    tester,
  ) async {
    usePhone(tester);
    final backend = FakeBackend(signedIn: true, mustChangePassword: true);
    await tester.pumpWidget(appWith(backend));
    await tester.pumpAndSettle();

    await tester.tap(find.widgetWithText(FilledButton, 'Se déconnecter'));
    await tester.pumpAndSettle();
    expect(find.text('Se déconnecter ?'), findsOneWidget);

    await tester.tap(find.text('Annuler'));
    await tester.pumpAndSettle();
    verifyNever(() => backend.auth.signOut());

    await tester.tap(find.widgetWithText(FilledButton, 'Se déconnecter'));
    await tester.pumpAndSettle();
    await tester.tap(
      find.descendant(
        of: find.byType(AlertDialog),
        matching: find.text('Se déconnecter'),
      ),
    );
    await tester.pumpAndSettle();

    verify(() => backend.auth.signOut()).called(1);
    expect(find.byType(LoginScreen), findsOneWidget);
  });
}
