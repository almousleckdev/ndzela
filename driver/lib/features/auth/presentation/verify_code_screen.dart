import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/router.dart';
import '../../../app/theme/app_text_styles.dart';
import '../../../core/error/error_messages.dart';
import '../../../core/widgets/adaptive/app_button.dart';
import '../../../core/widgets/adaptive/app_link_button.dart';
import '../../../core/widgets/adaptive/app_message.dart';
import '../../../core/widgets/adaptive/otp_field.dart';
import '../../../core/widgets/feedback/error_banner.dart';
import '../domain/reset_credentials.dart';
import 'forgot_password_controller.dart';
import 'widgets/auth_hero_scaffold.dart';

// Step 2 of a forgotten password: the 6-digit code, checked before the new password is asked.
class VerifyCodeScreen extends ConsumerStatefulWidget {
  const VerifyCodeScreen({super.key, required this.identifier});

  final String identifier;

  @override
  ConsumerState<VerifyCodeScreen> createState() => _VerifyCodeScreenState();
}

class _VerifyCodeScreenState extends ConsumerState<VerifyCodeScreen> {
  static const _length = 6;

  final _code = TextEditingController();

  @override
  void dispose() {
    _code.dispose();
    super.dispose();
  }

  Future<void> _verify() async {
    if (_code.text.length != _length) return;
    final ok = await ref
        .read(forgotPasswordControllerProvider.notifier)
        .verifyCode(identifier: widget.identifier, code: _code.text);
    if (ok && mounted) {
      await context.push(
        Routes.newPassword,
        extra: ResetCredentials(
          identifier: widget.identifier,
          code: _code.text,
        ),
      );
    } else if (mounted) {
      _code.clear();
    }
  }

  Future<void> _resend() async {
    final sent = await ref
        .read(forgotPasswordControllerProvider.notifier)
        .requestCode(widget.identifier);
    if (sent && mounted) showAppMessage(context, 'Un nouveau code est envoyé.');
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(forgotPasswordControllerProvider);
    return AuthHeroScaffold(
      showBack: true,
      title: 'Saisir le code',
      subtitle: 'Code à 6 chiffres reçu par e-mail.',
      children: [
        Text(
          'Si un compte existe, un code vient d’être envoyé par e-mail. '
          'Il est valable quelques minutes.',
          style: AppTextStyles.label,
        ),
        const SizedBox(height: 20),
        OtpField(
          controller: _code,
          length: _length,
          enabled: !state.isLoading,
          onCompleted: (_) => _verify(),
        ),
        if (state.hasError) ...[
          const SizedBox(height: 12),
          ErrorBanner(message: messageFor(state.error!)),
        ],
        const SizedBox(height: 20),
        AppButton(
          label: 'Vérifier le code',
          loading: state.isLoading,
          onPressed: _verify,
        ),
        const SizedBox(height: 4),
        Center(
          child: AppLinkButton(
            label: 'Renvoyer un code',
            onTap: state.isLoading ? null : _resend,
          ),
        ),
      ],
    );
  }
}
