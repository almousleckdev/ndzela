import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../app/theme/app_colors.dart';
import '../../../core/error/error_messages.dart';
import '../../../core/widgets/adaptive/app_button.dart';
import '../../../core/widgets/adaptive/app_password_field.dart';
import '../../../core/widgets/feedback/error_banner.dart';
import '../../../core/widgets/layout/screen_scaffold.dart';
import 'change_password_controller.dart';
import 'confirm_logout.dart';

class ChangePasswordScreen extends ConsumerStatefulWidget {
  const ChangePasswordScreen({super.key});

  @override
  ConsumerState<ChangePasswordScreen> createState() =>
      _ChangePasswordScreenState();
}

class _ChangePasswordScreenState extends ConsumerState<ChangePasswordScreen> {
  static const _minLength = 8;

  final _formKey = GlobalKey<FormState>();
  final _current = TextEditingController();
  final _new = TextEditingController();
  final _confirm = TextEditingController();

  @override
  void dispose() {
    _current.dispose();
    _new.dispose();
    _confirm.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    await ref
        .read(changePasswordControllerProvider.notifier)
        .change(currentPassword: _current.text, newPassword: _new.text);
  }

  @override
  Widget build(BuildContext context) {
    final change = ref.watch(changePasswordControllerProvider);
    return ScreenScaffold(
      title: 'Nouveau mot de passe',
      subtitle: 'Choisissez un mot de passe personnel.',
      body: SafeArea(
        child: SingleChildScrollView(
          keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
          padding: const EdgeInsets.all(24),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const Text(
                  'Pour votre sécurité, choisissez un mot de passe personnel avant de continuer.',
                  style: TextStyle(color: AppColors.muted),
                ),
                const SizedBox(height: 24),
                AppPasswordField(
                  label: 'Mot de passe actuel',
                  controller: _current,
                  textInputAction: TextInputAction.next,
                  enabled: !change.isLoading,
                  validator: (v) => (v == null || v.isEmpty)
                      ? 'Saisissez votre mot de passe actuel.'
                      : null,
                ),
                const SizedBox(height: 16),
                AppPasswordField(
                  label: 'Nouveau mot de passe',
                  controller: _new,
                  textInputAction: TextInputAction.next,
                  enabled: !change.isLoading,
                  validator: (v) => (v == null || v.length < _minLength)
                      ? 'Au moins $_minLength caractères.'
                      : null,
                ),
                const SizedBox(height: 16),
                AppPasswordField(
                  label: 'Confirmer le mot de passe',
                  controller: _confirm,
                  textInputAction: TextInputAction.done,
                  enabled: !change.isLoading,
                  onSubmitted: (_) => _submit(),
                  validator: (v) => v == _new.text
                      ? null
                      : 'Les mots de passe ne correspondent pas.',
                ),
                if (change.hasError) ...[
                  const SizedBox(height: 16),
                  ErrorBanner(message: messageFor(change.error!)),
                ],
                const SizedBox(height: 24),
                AppButton(
                  label: 'Enregistrer',
                  loading: change.isLoading,
                  onPressed: _submit,
                ),
                const SizedBox(height: 12),
                AppButton(
                  label: 'Se déconnecter',
                  variant: AppButtonVariant.destructive,
                  onPressed: change.isLoading
                      ? null
                      : () => confirmLogout(context, ref),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
