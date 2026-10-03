import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/widgets/adaptive/confirm_dialog.dart';
import 'session_controller.dart';

// Asks the driver to confirm, then signs out.
Future<void> confirmLogout(BuildContext context, WidgetRef ref) async {
  final confirmed = await showConfirmDialog(
    context,
    title: 'Se déconnecter ?',
    message:
        'Vous devrez saisir à nouveau vos identifiants pour accéder à votre compte.',
    confirmLabel: 'Se déconnecter',
    destructive: true,
  );
  if (confirmed) await ref.read(sessionControllerProvider.notifier).logout();
}
