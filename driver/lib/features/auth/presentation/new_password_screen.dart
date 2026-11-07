import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/router.dart';
import '../../../app/theme/app_text_styles.dart';
import '../../../core/error/error_messages.dart';
import '../../../core/widgets/adaptive/app_button.dart';
import '../../../core/widgets/adaptive/app_message.dart';
import '../../../core/widgets/adaptive/app_password_field.dart';
import '../../../core/widgets/feedback/error_banner.dart';
import '../domain/reset_credentials.dart';
import 'forgot_password_controller.dart';
import 'widgets/auth_hero_scaffold.dart';

// Step 3 of a forgotten password: the new password, once the code is known to be good.
class NewPasswordScreen extends ConsumerStatefulWidget {
  const NewPasswordScreen({super.key, required this.credentials});

  final ResetCredentials credentials;

  @override
  ConsumerState<NewPasswordScreen> createState() => _NewPasswordScreenState();
}

class _NewPasswordScreenState extends ConsumerState<NewPasswordScreen> {
  static const _minLength = 8;

  final _formKey = GlobalKey<FormState>();
  final _password = TextEditingController();
  final _confirm = TextEditingController();

  @override
  void dispose() {
    _password.dispose();
    _confirm.dispose();
    super.dispose();
  }

  Future<void> _reset() async {
    if (!_formKey.currentState!.validate()) return;
    final done = await ref
        .read(forgotPasswordControllerProvider.notifier)
        .reset(
          identifier: widget.credentials.identifier,
          code: widget.credentials.code,
          newPassword: _password.text,
        );
    if (done && mounted) {
      showAppMessage(context, 'Mot de passe modifié. Connectez-vous.');
      context.go(Routes.login);
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(forgotPasswordControllerProvider);
    return AuthHeroScaffold(
      showBack: true,
      title: 'Nouveau mot de passe',
      subtitle: 'Choisissez un mot de passe personnel.',
      children: [
        Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(
                'Le code est valide. Au moins $_minLength caractères.',
                style: AppTextStyles.label,
              ),
              const SizedBox(height: 20),
              AppPasswordField(
                label: 'Nouveau mot de passe',
                controller: _password,
                textInputAction: TextInputAction.next,
                enabled: !state.isLoading,
                validator: (v) => (v == null || v.length < _minLength)
                    ? 'Au moins $_minLength caractères.'
                    : null,
              ),
              const SizedBox(height: 14),
              AppPasswordField(
                label: 'Confirmer le mot de passe',
                controller: _confirm,
                textInputAction: TextInputAction.done,
                enabled: !state.isLoading,
                onSubmitted: (_) => _reset(),
                validator: (v) => v == _password.text
                    ? null
                    : 'Les mots de passe ne correspondent pas.',
              ),
              if (state.hasError) ...[
                const SizedBox(height: 12),
                ErrorBanner(message: messageFor(state.error!)),
              ],
              const SizedBox(height: 20),
              AppButton(
                label: 'Changer le mot de passe',
                loading: state.isLoading,
                onPressed: _reset,
              ),
            ],
          ),
        ),
      ],
    );
  }
}
