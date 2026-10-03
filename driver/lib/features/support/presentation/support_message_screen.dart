import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/error/error_messages.dart';
import '../../../core/platform/app_icons.dart';
import '../../../core/widgets/adaptive/app_button.dart';
import '../../../core/widgets/adaptive/app_icon_button.dart';
import '../../../core/widgets/adaptive/app_message.dart';
import '../../../core/widgets/adaptive/app_text_field.dart';
import '../../../core/widgets/feedback/error_banner.dart';
import '../../../core/widgets/layout/screen_scaffold.dart';
import 'support_message_controller.dart';

class SupportMessageScreen extends ConsumerStatefulWidget {
  const SupportMessageScreen({super.key, this.initialMessage});

  // A message to start from, e.g.
  final String? initialMessage;

  @override
  ConsumerState<SupportMessageScreen> createState() =>
      _SupportMessageScreenState();
}

class _SupportMessageScreenState extends ConsumerState<SupportMessageScreen> {
  static const _maxLength = 1000;

  final _formKey = GlobalKey<FormState>();
  late final _message = TextEditingController(text: widget.initialMessage);

  @override
  void dispose() {
    _message.dispose();
    super.dispose();
  }

  Future<void> _send() async {
    if (!_formKey.currentState!.validate()) return;
    final sent = await ref
        .read(supportMessageControllerProvider.notifier)
        .send(_message.text);
    if (sent && mounted) {
      showAppMessage(
        context,
        'Message envoyé. Nous vous répondons au plus vite.',
      );
      context.pop();
    }
  }

  @override
  Widget build(BuildContext context) {
    final send = ref.watch(supportMessageControllerProvider);
    return ScreenScaffold(
      title: 'Envoyer un message',
      subtitle: 'Notre équipe vous répond au plus vite.',
      leading: AppIconButton(
        icon: AppIcons.back,
        tooltip: 'Retour',
        onPressed: () => context.pop(),
      ),
      body: SingleChildScrollView(
        keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
        padding: const EdgeInsets.all(20),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              AppTextField(
                label: 'Votre message',
                controller: _message,
                minLines: 6,
                maxLines: 10,
                maxLength: _maxLength,
                keyboardType: TextInputType.multiline,
                enabled: !send.isLoading,
                validator: (v) => (v == null || v.trim().isEmpty)
                    ? 'Écrivez votre message.'
                    : null,
              ),
              if (send.hasError) ...[
                const SizedBox(height: 12),
                ErrorBanner(message: messageFor(send.error!)),
              ],
              const SizedBox(height: 20),
              AppButton(
                label: 'Envoyer',
                icon: AppIcons.send,
                loading: send.isLoading,
                onPressed: _send,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
