import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/router.dart';
import '../../../app/theme/app_text_styles.dart';
import '../../../core/error/error_messages.dart';
import '../../../core/platform/app_icons.dart';
import '../../../core/widgets/adaptive/app_button.dart';
import '../../../core/widgets/adaptive/app_text_field.dart';
import '../../../core/widgets/feedback/error_banner.dart';
import 'forgot_password_controller.dart';
import 'widgets/auth_hero_scaffold.dart';
import 'widgets/back_to_login_button.dart';

// Step 1 of a forgotten password: the phone or e-mail that gets the code.
class ForgotPasswordScreen extends ConsumerStatefulWidget {
  const ForgotPasswordScreen({super.key});

  @override
  ConsumerState<ForgotPasswordScreen> createState() =>
      _ForgotPasswordScreenState();
}

class _ForgotPasswordScreenState extends ConsumerState<ForgotPasswordScreen> {
  final _formKey = GlobalKey<FormState>();
  final _identifier = TextEditingController();

  @override
  void dispose() {
    _identifier.dispose();
    super.dispose();
  }

  Future<void> _sendCode() async {
    if (!_formKey.currentState!.validate()) return;
    final identifier = _identifier.text.trim();
    final sent = await ref
        .read(forgotPasswordControllerProvider.notifier)
        .requestCode(identifier);
    if (sent && mounted) {
      await context.push(Routes.verifyCode, extra: identifier);
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(forgotPasswordControllerProvider);
    return AuthHeroScaffold(
      title: 'Mot de passe oublié',
      subtitle: 'Recevez un code par e-mail.',
      children: [
        Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(
                'Saisissez votre e-mail. Si un compte existe, '
                'nous envoyons un code à l’adresse e-mail liée à votre compte.',
                style: AppTextStyles.label,
              ),
              const SizedBox(height: 20),
              AppTextField(
                label: 'E-mail',
                hint: 'example@google.com',
                controller: _identifier,
                prefixIcon: AppIcons.person,
                keyboardType: TextInputType.emailAddress,
                textInputAction: TextInputAction.done,
                enabled: !state.isLoading,
                onSubmitted: (_) => _sendCode(),
                validator: (v) => (v == null || v.trim().isEmpty)
                    ? 'Saisissez votre e-mail.'
                    : null,
              ),
              if (state.hasError) ...[
                const SizedBox(height: 12),
                ErrorBanner(message: messageFor(state.error!)),
              ],
              const SizedBox(height: 20),
              AppButton(
                label: 'Envoyer le code',
                loading: state.isLoading,
                onPressed: _sendCode,
              ),
              const SizedBox(height: 10),
              BackToLoginButton(busy: state.isLoading),
            ],
          ),
        ),
      ],
    );
  }
}
