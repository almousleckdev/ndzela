import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../app/theme/app_colors.dart';
import '../../../../app/theme/brand_colors.dart';
import '../../../../core/platform/app_icons.dart';
import '../../../../core/platform/external_link.dart';
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
          color: context.brand.primaryInk,
          background: context.brand.primarySoft,
          onTap: () => _open(context, phoneUri(contacts.phone!)),
        ),
      if (contacts.whatsapp != null)
        SupportActionTile(
          icon: AppIcons.whatsapp,
          label: 'WhatsApp',
          color: AppColors.whatsapp,
          background: AppColors.whatsappSoft,
          onTap: () => _open(context, whatsappUri(contacts.whatsapp!)),
        ),
      if (contacts.email != null)
        SupportActionTile(
          icon: AppIcons.mail,
          label: 'E-mail',
          color: context.brand.primaryInk,
          background: context.brand.primarySoft,
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
        padding: const EdgeInsets.symmetric(vertical: 10),
        child: AsyncView(
          value: ref.watch(supportContactsProvider),
          onRetry: () => ref.invalidate(supportContactsProvider),
          data: (contacts) => Row(children: _tiles(context, contacts)),
        ),
      ),
    );
  }
}
