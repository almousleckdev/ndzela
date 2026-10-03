import 'package:driver/features/auth/presentation/forgot_password_screen.dart';
import 'package:driver/features/auth/presentation/login_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

import 'support/fakes.dart';
import 'support/phone_view.dart';

void main() {
  testWidgets('a forgotten password is reset with the code sent by e-mail', (
    tester,
  ) async {
    usePhone(tester);
    final backend = FakeBackend();
    await tester.pumpWidget(appWith(backend));
    await tester.pumpAndSettle();

    await tester.ensureVisible(find.text('Mot de passe oublié ?'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Mot de passe oublié ?'));
    await tester.pumpAndSettle();
    expect(find.byType(ForgotPasswordScreen), findsOneWidget);

    await tester.tap(find.text('Envoyer le code'));
    await tester.pumpAndSettle();
    expect(
      find.text('Saisissez votre téléphone ou votre e-mail.'),
      findsOneWidget,
    );
    verifyNever(() => backend.auth.requestResetCode(any()));

    await tester.enterText(find.byType(TextFormField).first, 'awa@ndzela.test');
    await tester.tap(find.text('Envoyer le code'));
    await tester.pumpAndSettle();
    verify(() => backend.auth.requestResetCode('awa@ndzela.test')).called(1);
    expect(find.text('Changer le mot de passe'), findsOneWidget);

    await tester.tap(find.text('Changer le mot de passe'));
    await tester.pumpAndSettle();
    expect(find.text('Code à 6 chiffres.'), findsOneWidget);

    final fields = find.byType(TextFormField);
    await tester.enterText(fields.at(0), '123456');
    await tester.enterText(fields.at(1), 'Nouveau123');
    await tester.enterText(fields.at(2), 'Nouveau123');
    await tester.tap(find.text('Changer le mot de passe'));
    await tester.pumpAndSettle();

    verify(
      () => backend.auth.resetPassword(
        identifier: 'awa@ndzela.test',
        code: '123456',
        newPassword: 'Nouveau123',
      ),
    ).called(1);
    expect(find.byType(LoginScreen), findsOneWidget);
  });
}
