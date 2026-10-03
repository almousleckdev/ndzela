import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router.dart';
import '../../../../core/platform/app_icons.dart';
import '../../../../core/widgets/adaptive/app_button.dart';

// Opens the message to support with the payment's reference already written
class ContactSupportButton extends StatelessWidget {
  const ContactSupportButton({super.key, required this.reference});

  final String reference;

  @override
  Widget build(BuildContext context) {
    return AppButton(
      label: 'Contacter le support',
      icon: AppIcons.chat,
      variant: AppButtonVariant.secondary,
      onPressed: () => context.push(
        Routes.supportMessage,
        extra:
            'Bonjour, mon paiement (référence $reference) n\'a pas abouti. Pouvez-vous m\'aider ?',
      ),
    );
  }
}
