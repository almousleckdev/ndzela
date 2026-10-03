import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:go_router/go_router.dart';

import '../../../app/router.dart';
import '../../../app/theme/app_colors.dart';
import '../../../app/theme/brand_colors.dart';
import '../../../core/error/error_messages.dart';
import '../../../core/platform/app_icons.dart';
import '../../../core/widgets/adaptive/app_button.dart';
import '../../../core/widgets/adaptive/app_password_field.dart';
import '../../../core/widgets/adaptive/app_tappable.dart';
import '../../../core/widgets/adaptive/app_text_field.dart';
import '../../../core/widgets/feedback/error_banner.dart';
import 'login_controller.dart';
import 'widgets/login_header.dart';

class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _identifier = TextEditingController();
  final _password = TextEditingController();

  @override
  void dispose() {
    _identifier.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    TextInput.finishAutofillContext();
    await ref
        .read(loginControllerProvider.notifier)
        .signIn(identifier: _identifier.text, password: _password.text);
  }

  String? _required(String? value, String message) =>
      (value == null || value.trim().isEmpty) ? message : null;

  @override
  Widget build(BuildContext context) {
    final login = ref.watch(loginControllerProvider);
    return Scaffold(
      body: SingleChildScrollView(
        keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
        child: Column(
          children: [
            const LoginHeader(),
            Padding(
              padding: const EdgeInsets.fromLTRB(24, 32, 24, 24),
              child: AutofillGroup(
                child: Form(
                  key: _formKey,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Text(
                        'Connexion',
                        style: Theme.of(context).textTheme.headlineSmall
                            ?.copyWith(fontWeight: FontWeight.w800),
                      ),
                      const SizedBox(height: 4),
                      const Text(
                        'Connectez-vous avec les accès remis par Ndzela.',
                        style: TextStyle(color: AppColors.muted),
                      ),
                      const SizedBox(height: 24),
                      AppTextField(
                        label: 'Téléphone ou e-mail',
                        controller: _identifier,
                        prefixIcon: AppIcons.person,
                        keyboardType: TextInputType.emailAddress,
                        textInputAction: TextInputAction.next,
                        autofillHints: const [AutofillHints.username],
                        enabled: !login.isLoading,
                        validator: (v) => _required(
                          v,
                          'Saisissez votre téléphone ou votre e-mail.',
                        ),
                      ),
                      const SizedBox(height: 16),
                      AppPasswordField(
                        label: 'Mot de passe',
                        controller: _password,
                        textInputAction: TextInputAction.done,
                        enabled: !login.isLoading,
                        onSubmitted: (_) => _submit(),
                        validator: (v) =>
                            _required(v, 'Saisissez votre mot de passe.'),
                      ),
                      if (login.hasError) ...[
                        const SizedBox(height: 16),
                        ErrorBanner(message: messageFor(login.error!)),
                      ],
                      const SizedBox(height: 8),
                      Align(
                        alignment: Alignment.centerRight,
                        child: AppTappable(
                          onTap: () => context.push(Routes.forgotPassword),
                          borderRadius: BorderRadius.circular(8),
                          child: Padding(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 8,
                              vertical: 8,
                            ),
                            child: Text(
                              'Mot de passe oublié ?',
                              style: TextStyle(
                                color: context.brand.primaryInk,
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                          ),
                        ),
                      ),
                      const SizedBox(height: 16),
                      AppButton(
                        label: 'Se connecter',
                        loading: login.isLoading,
                        onPressed: _submit,
                      ),
                      const SizedBox(height: 20),
                      const Text(
                        'Pas de compte ? Votre compte est créé par Ndzela avec votre contrat.',
                        textAlign: TextAlign.center,
                        style: TextStyle(color: AppColors.muted, fontSize: 13),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
