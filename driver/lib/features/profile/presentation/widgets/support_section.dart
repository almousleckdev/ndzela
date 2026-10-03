import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router.dart';
import '../../../../core/platform/app_icons.dart';
import '../../../../core/platform/external_link.dart';
import '../../../../core/widgets/adaptive/app_button.dart';
import '../../../../core/widgets/adaptive/app_message.dart';
import '../../../../core/widgets/display/section_card.dart';
import '../../../../core/widgets/feedback/async_view.dart';
import '../../../support/domain/support_contacts.dart';
import '../../../support/presentation/support_providers.dart';
import 'support_action_tile.dart';

class SupportSection extends ConsumerWidget {
  const SupportSection({super.key});

  Future<void> _open(BuildContext context, Uri uri) async {
    if (!await openExternal(uri) && context.mounted) {
      showAppMessage(context, "Impossible d'ouvrir cette application.");
    }
  }

  List<Widget> _tiles(BuildContext context, SupportContacts contacts) {
    final tiles = <Widget>[
      if (contacts.phone != null)
        SupportActionTile(
          icon: AppIcons.call,
          label: 'Appeler',
          onTap: () => _open(context, phoneUri(contacts.phone!)),
        ),
      if (contacts.whatsapp != null)
        SupportActionTile(
          icon: AppIcons.chat,
          label: 'WhatsApp',
          onTap: () => _open(context, whatsappUri(contacts.whatsapp!)),
        ),
      if (contacts.email != null)
        SupportActionTile(
          icon: AppIcons.email,
          label: 'E-mail',
          onTap: () => _open(context, emailUri(contacts.email!)),
        ),
    ];
    return [
      for (var i = 0; i < tiles.length; i++) ...[
        if (i > 0) const SizedBox(width: 10),
        Expanded(child: tiles[i]),
      ],
    ];
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return SectionCard(
      title: 'Contacter le support',
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 12),
        child: Column(
          children: [
            AsyncView(
              value: ref.watch(supportContactsProvider),
              onRetry: () => ref.invalidate(supportContactsProvider),
              data: (contacts) => Row(children: _tiles(context, contacts)),
            ),
            const SizedBox(height: 14),
            AppButton(
              label: 'Envoyer un message',
              icon: AppIcons.send,
              variant: AppButtonVariant.secondary,
              onPressed: () => context.push(Routes.supportMessage),
            ),
          ],
        ),
      ),
    );
  }
}
