import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:go_router/go_router.dart';

import '../../../app/router.dart';
import '../../../app/theme/app_text_styles.dart';
import '../../../core/error/error_messages.dart';
import '../../../core/platform/app_icons.dart';
import '../../../core/widgets/adaptive/app_button.dart';
import '../../../core/widgets/adaptive/app_password_field.dart';
import '../../../core/widgets/adaptive/app_link_button.dart';
import '../../../core/widgets/adaptive/app_text_field.dart';
import '../../../core/storage/token_storage.dart';
import '../../../core/widgets/adaptive/app_check_row.dart';
import '../../../core/widgets/adaptive/app_message.dart';
import '../domain/session.dart';
import 'login_controller.dart';
import 'session_controller.dart';
import 'widgets/auth_hero_scaffold.dart';

class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _identifier = TextEditingController();
  final _password = TextEditingController();
  var _remember = true;

  @override
  void initState() {
    super.initState();
    _prefill();
    final session = ref.read(sessionControllerProvider);
    if (session case SignedOut(:final reason?)) {
      ref.read(sessionControllerProvider.notifier).acknowledgeSessionEnd();
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted) _announce(reason);
      });
    }
  }

  void _announce(
    SessionEndReason reason,
  ) => showAppMessage(context, switch (reason) {
    SessionEndReason.replaced =>
      'Votre compte a été ouvert sur un autre appareil. Reconnectez-vous pour continuer ici.',
    SessionEndReason.expired =>
      'Votre session a expiré. Reconnectez-vous pour continuer.',
  }, title: 'Session terminée');

  Future<void> _prefill() async {
    final tokens = ref.read(tokenStorageProvider);
    final remember = await tokens.readRemember();
    final identifier = await tokens.readSavedIdentifier();
    if (!mounted) return;
    setState(() => _remember = remember);
    if (identifier != null && _identifier.text.isEmpty) {
      _identifier.text = identifier;
    }
  }

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
        .signIn(
          identifier: _identifier.text,
          password: _password.text,
          remember: _remember,
        );
  }

  String? _required(String? value, String message) =>
      (value == null || value.trim().isEmpty) ? message : null;

  @override
  Widget build(BuildContext context) {
    final login = ref.watch(loginControllerProvider);
    ref.listen(loginControllerProvider, (_, next) {
      if (next.hasError) {
        showAppMessage(
          context,
          messageFor(next.error!),
          kind: AppToastKind.error,
          title: 'Connexion impossible',
        );
      }
    });
    return AuthHeroScaffold(
      title: 'Connexion',
      subtitle: 'Connectez-vous avec les accès remis par Ndzela.',
      children: [
        AutofillGroup(
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                AppTextField(
                  label: 'E-mail',
                  hint: 'example@google.com',
                  controller: _identifier,
                  prefixIcon: AppIcons.person,
                  keyboardType: TextInputType.emailAddress,
                  textInputAction: TextInputAction.next,
                  autofillHints: const [AutofillHints.username],
                  enabled: !login.isLoading,
                  validator: (v) => _required(v, 'Saisissez votre e-mail.'),
                ),
                const SizedBox(height: 14),
                AppPasswordField(
                  label: 'Mot de passe',
                  controller: _password,
                  textInputAction: TextInputAction.done,
                  enabled: !login.isLoading,
                  onSubmitted: (_) => _submit(),
                  validator: (v) =>
                      _required(v, 'Saisissez votre mot de passe.'),
                ),
                const SizedBox(height: 12),
                AppCheckRow(
                  label: 'Se souvenir de moi',
                  value: _remember,
                  onChanged: login.isLoading
                      ? null
                      : (v) => setState(() => _remember = v),
                ),
                Align(
                  alignment: Alignment.centerRight,
                  child: AppLinkButton(
                    label: 'Mot de passe oublié ?',
                    onTap: () => context.push(Routes.forgotPassword),
                  ),
                ),
                const SizedBox(height: 8),
                AppButton(
                  label: 'Se connecter',
                  loading: login.isLoading,
                  onPressed: _submit,
                ),
                const SizedBox(height: 16),
                const Text(
                  'Pas de compte ? Votre compte est créé par Ndzela avec votre contrat.',
                  textAlign: TextAlign.center,
                  style: AppTextStyles.caption,
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}
