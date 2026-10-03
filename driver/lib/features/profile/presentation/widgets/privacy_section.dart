import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/config/app_config.dart';
import '../../../../core/error/error_messages.dart';
import '../../../../core/platform/app_icons.dart';
import '../../../../core/platform/external_link.dart';
import '../../../../core/widgets/adaptive/app_message.dart';
import '../../../../core/widgets/adaptive/confirm_dialog.dart';
import '../../../../app/theme/app_colors.dart';
import '../../../../core/widgets/display/action_row.dart';
import '../../../../core/widgets/display/app_card.dart';
import '../../../../core/widgets/display/section_card.dart';
import '../../../support/presentation/account_deletion_controller.dart';

// Where the driver reads the legal texts, and, apart from them, asks for the account to be deleted.
class PrivacySection extends ConsumerWidget {
  const PrivacySection({super.key});

  Future<void> _open(BuildContext context, Uri uri) async {
    if (!await openExternal(uri) && context.mounted) {
      showAppMessage(context, "Impossible d'ouvrir cette page.");
    }
  }

  Future<void> _delete(BuildContext context, WidgetRef ref) async {
    final confirmed = await showConfirmDialog(
      context,
      title: 'Supprimer mon compte ?',
      message:
          "Votre demande est transmise à Ndzela, qui supprime vos données personnelles. L'historique de vos loyers est conservé tant que la loi l'exige.",
      confirmLabel: 'Envoyer la demande',
      destructive: true,
    );
    if (!confirmed || !context.mounted) return;
    final sent = await ref
        .read(accountDeletionControllerProvider.notifier)
        .request();
    if (!context.mounted) return;
    showAppMessage(
      context,
      sent
          ? 'Demande envoyée. Ndzela vous contactera.'
          : messageFor(ref.read(accountDeletionControllerProvider).error!),
    );
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final busy = ref.watch(accountDeletionControllerProvider).isLoading;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SectionCard(
          title: 'Informations légales',
          child: Column(
            children: [
              ActionRow(
                icon: AppIcons.document,
                label: 'Politique de confidentialité',
                hint: 'Comment vos données sont utilisées',
                onTap: () => _open(context, AppConfig.privacyUri),
              ),
              const Divider(height: 1, color: AppColors.line, indent: 54),
              ActionRow(
                icon: AppIcons.document,
                label: 'Conditions d’utilisation',
                hint: 'Les règles de l’application',
                onTap: () => _open(context, AppConfig.termsUri),
              ),
            ],
          ),
        ),
        const SizedBox(height: 20),
        AppCard(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
          child: ActionRow(
            icon: AppIcons.logout,
            label: 'Supprimer mon compte',
            hint: 'Demander la suppression de vos données',
            destructive: true,
            busy: busy,
            onTap: () => _delete(context, ref),
          ),
        ),
      ],
    );
  }
}
