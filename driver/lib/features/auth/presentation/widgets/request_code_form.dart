import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../app/theme/app_colors.dart';
import '../../../../core/error/error_messages.dart';
import '../../../../core/platform/app_icons.dart';
import '../../../../core/widgets/adaptive/app_button.dart';
import '../../../../core/widgets/adaptive/app_text_field.dart';
import '../../../../core/widgets/feedback/error_banner.dart';
import '../forgot_password_controller.dart';
import 'back_to_login_button.dart';

// First step of a forgotten password: the phone or e-mail that gets the code.
class RequestCodeForm extends ConsumerStatefulWidget {
  const RequestCodeForm({super.key, required this.onSent});

  final ValueChanged<String> onSent;

  @override
  ConsumerState<RequestCodeForm> createState() => _RequestCodeFormState();
}

class _RequestCodeFormState extends ConsumerState<RequestCodeForm> {
  final _formKey = GlobalKey<FormState>();
  final _identifier = TextEditingController();

  @override
  void dispose() {
    _identifier.dispose();
    super.dispose();
  }

  Future<void> _sendCode() async {
    if (!_formKey.currentState!.validate()) return;
    final sent = await ref
        .read(forgotPasswordControllerProvider.notifier)
        .requestCode(_identifier.text);
    if (sent && mounted) widget.onSent(_identifier.text);
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(forgotPasswordControllerProvider);
    return Form(
      key: _formKey,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Text(
            'Saisissez votre téléphone ou votre e-mail. Si un compte existe, '
            'nous envoyons un code à l’adresse e-mail liée à votre compte.',
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
          BackToLoginButton(busy: state.isLoading),
        ],
      ),
    );
  }
}
