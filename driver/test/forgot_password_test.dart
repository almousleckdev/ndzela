import 'package:driver/features/auth/presentation/forgot_password_screen.dart';
import 'package:driver/features/auth/presentation/login_screen.dart';
import 'package:driver/features/auth/presentation/new_password_screen.dart';
import 'package:driver/features/auth/presentation/verify_code_screen.dart';
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
    expect(find.text('Saisissez votre e-mail.'), findsOneWidget);
    verifyNever(() => backend.auth.requestResetCode(any()));

    await tester.enterText(find.byType(TextFormField).first, 'awa@ndzela.test');
    await tester.tap(find.text('Envoyer le code'));
    await tester.pumpAndSettle();
    verify(() => backend.auth.requestResetCode('awa@ndzela.test')).called(1);
    expect(find.byType(VerifyCodeScreen), findsOneWidget);

    // A wrong code is refused by the server and the driver stays on the code screen.
    when(
      () => backend.auth.verifyResetCode(
        identifier: 'awa@ndzela.test',
        code: '000000',
      ),
    ).thenThrow(apiError(422, 'INVALID_RESET_CODE'));
    await tester.enterText(find.byType(EditableText), '000000');
    await tester.pumpAndSettle();
    expect(find.byType(VerifyCodeScreen), findsOneWidget);
    expect(find.byType(NewPasswordScreen), findsNothing);

    await tester.tap(find.text('Renvoyer un code'));
    await tester.pumpAndSettle();
    verify(() => backend.auth.requestResetCode('awa@ndzela.test')).called(1);

    await tester.enterText(find.byType(EditableText), '123456');
    await tester.pumpAndSettle();
    verify(
      () => backend.auth.verifyResetCode(
        identifier: 'awa@ndzela.test',
        code: '123456',
      ),
    ).called(1);
    expect(find.byType(NewPasswordScreen), findsOneWidget);

    await tester.tap(find.text('Changer le mot de passe'));
    await tester.pumpAndSettle();
    expect(find.text('Au moins 8 caractères.'), findsOneWidget);

    final fields = find.byType(TextFormField);
    await tester.enterText(fields.at(0), 'Nouveau123');
    await tester.enterText(fields.at(1), 'Nouveau123');
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
