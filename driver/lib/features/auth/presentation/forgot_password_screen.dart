import 'package:flutter/material.dart';

import '../../../core/widgets/layout/screen_scaffold.dart';
import 'widgets/request_code_form.dart';
import 'widgets/reset_password_form.dart';

// A forgotten password in two steps: ask for a code by e-mail, then choose a new password.
class ForgotPasswordScreen extends StatefulWidget {
  const ForgotPasswordScreen({super.key});

  @override
  State<ForgotPasswordScreen> createState() => _ForgotPasswordScreenState();
}

class _ForgotPasswordScreenState extends State<ForgotPasswordScreen> {
  // Set once the code is sent: the identifier it was sent for.
  String? _identifier;

  @override
  Widget build(BuildContext context) {
    final identifier = _identifier;
    return ScreenScaffold(
      title: 'Mot de passe oublié',
      subtitle: identifier == null
          ? 'Recevez un code par e-mail.'
          : 'Saisissez le code reçu par e-mail.',
      body: SafeArea(
        child: SingleChildScrollView(
          keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
          padding: const EdgeInsets.all(24),
          child: identifier == null
              ? RequestCodeForm(
                  onSent: (value) => setState(() => _identifier = value),
                )
              : ResetPasswordForm(identifier: identifier),
        ),
      ),
    );
  }
}
