import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/router.dart';
import '../../../app/theme/app_colors.dart';
import '../../../core/error/error_messages.dart';
import '../../../core/platform/app_icons.dart';
import '../../../core/widgets/adaptive/app_button.dart';
import '../../../core/widgets/adaptive/app_message.dart';
import '../../../core/widgets/adaptive/app_password_field.dart';
import '../../../core/widgets/adaptive/app_text_field.dart';
import '../../../core/widgets/feedback/error_banner.dart';
import '../../../core/widgets/layout/screen_scaffold.dart';
import 'forgot_password_controller.dart';

class ForgotPasswordScreen extends ConsumerStatefulWidget {
  const ForgotPasswordScreen({super.key});

  @override
  ConsumerState<ForgotPasswordScreen> createState() =>
      _ForgotPasswordScreenState();
}

class _ForgotPasswordScreenState extends ConsumerState<ForgotPasswordScreen> {
  static const _minLength = 8;

  final _identifierKey = GlobalKey<FormState>();
  final _resetKey = GlobalKey<FormState>();
  final _identifier = TextEditingController();
  final _code = TextEditingController();
  final _password = TextEditingController();
  final _confirm = TextEditingController();
  bool _codeSent = false;

  @override
  void dispose() {
    _identifier.dispose();
    _code.dispose();
    _password.dispose();
    _confirm.dispose();
    super.dispose();
  }

  Future<void> _sendCode() async {
    if (!_identifierKey.currentState!.validate()) return;
    final sent = await ref
        .read(forgotPasswordControllerProvider.notifier)
        .requestCode(_identifier.text);
    if (sent && mounted) setState(() => _codeSent = true);
  }

  Future<void> _resend() async {
    final sent = await ref
        .read(forgotPasswordControllerProvider.notifier)
        .requestCode(_identifier.text);
    if (sent && mounted) showAppMessage(context, 'Un nouveau code est envoyé.');
  }

  Future<void> _reset() async {
    if (!_resetKey.currentState!.validate()) return;
    final done = await ref
        .read(forgotPasswordControllerProvider.notifier)
        .reset(
          identifier: _identifier.text,
          code: _code.text,
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
    return ScreenScaffold(
      title: 'Mot de passe oublié',
      subtitle: _codeSent
          ? 'Saisissez le code reçu par e-mail.'
          : 'Recevez un code par e-mail.',
      body: SafeArea(
        child: SingleChildScrollView(
          keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
          padding: const EdgeInsets.all(24),
          child: _codeSent ? _resetForm(state) : _identifierForm(state),
        ),
      ),
    );
  }

  Widget _identifierForm(AsyncValue<void> state) => Form(
    key: _identifierKey,
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text(
          'Saisissez votre téléphone ou votre e-mail. Si un compte existe, nous envoyons un code à l’adresse e-mail liée à votre compte.',
          style: TextStyle(color: AppColors.muted),
        ),
        const SizedBox(height: 24),
        AppTextField(
          label: 'Téléphone ou e-mail',
          controller: _identifier,
          prefixIcon: AppIcons.person,
          keyboardType: TextInputType.emailAddress,
          textInputAction: TextInputAction.done,
          enabled: !state.isLoading,
          onSubmitted: (_) => _sendCode(),
          validator: (v) => (v == null || v.trim().isEmpty)
              ? 'Saisissez votre téléphone ou votre e-mail.'
              : null,
        ),
        if (state.hasError) ...[
          const SizedBox(height: 16),
          ErrorBanner(message: messageFor(state.error!)),
        ],
        const SizedBox(height: 24),
        AppButton(
          label: 'Envoyer le code',
          loading: state.isLoading,
          onPressed: _sendCode,
        ),
        const SizedBox(height: 12),
        _backToLogin(state),
      ],
    ),
  );

  Widget _resetForm(AsyncValue<void> state) => Form(
    key: _resetKey,
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text(
          'Si un compte existe, un code à 6 chiffres vient d’être envoyé par e-mail. Il est valable quelques minutes.',
          style: TextStyle(color: AppColors.muted),
        ),
        const SizedBox(height: 24),
        AppTextField(
          label: 'Code reçu par e-mail',
          controller: _code,
          prefixIcon: AppIcons.document,
          keyboardType: TextInputType.number,
          textInputAction: TextInputAction.next,
          enabled: !state.isLoading,
          inputFormatters: [
            FilteringTextInputFormatter.digitsOnly,
            LengthLimitingTextInputFormatter(6),
          ],
          validator: (v) =>
              (v != null && v.trim().length == 6) ? null : 'Code à 6 chiffres.',
        ),
        const SizedBox(height: 16),
        AppPasswordField(
          label: 'Nouveau mot de passe',
          controller: _password,
          textInputAction: TextInputAction.next,
          enabled: !state.isLoading,
          validator: (v) => (v == null || v.length < _minLength)
              ? 'Au moins $_minLength caractères.'
              : null,
        ),
        const SizedBox(height: 16),
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
          const SizedBox(height: 16),
          ErrorBanner(message: messageFor(state.error!)),
        ],
        const SizedBox(height: 24),
        AppButton(
          label: 'Changer le mot de passe',
          loading: state.isLoading,
          onPressed: _reset,
        ),
        const SizedBox(height: 12),
        AppButton(
          label: 'Renvoyer un code',
          variant: AppButtonVariant.secondary,
          onPressed: state.isLoading ? null : _resend,
        ),
        const SizedBox(height: 12),
        _backToLogin(state),
      ],
    ),
  );

  Widget _backToLogin(AsyncValue<void> state) => AppButton(
    label: 'Retour à la connexion',
    variant: AppButtonVariant.secondary,
    onPressed: state.isLoading ? null : () => context.go(Routes.login),
  );
}
