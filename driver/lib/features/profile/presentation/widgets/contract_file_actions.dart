import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/error/error_messages.dart';
import '../../../../core/platform/app_icons.dart';
import '../../../../core/widgets/adaptive/activity_indicator.dart';
import '../../../../core/widgets/adaptive/app_icon_button.dart';
import '../../../../core/widgets/adaptive/app_message.dart';
import '../contract_file_controller.dart';

// "Ouvrir" and "Télécharger" for the driver's contract.
class ContractFileActions extends ConsumerWidget {
  const ContractFileActions({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(contractFileControllerProvider);
    ref.listen(contractFileControllerProvider, (_, next) {
      if (next.hasError) showAppMessage(context, messageFor(next.error!));
    });
    if (state.isLoading) return const AppActivityIndicator(size: 20);
    final controller = ref.read(contractFileControllerProvider.notifier);
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        AppIconButton(
          icon: AppIcons.open,
          tooltip: 'Ouvrir le contrat',
          size: 36,
          onPressed: controller.open,
        ),
        const SizedBox(width: 8),
        AppIconButton(
          icon: AppIcons.download,
          tooltip: 'Télécharger le contrat',
          size: 36,
          onPressed: controller.download,
        ),
      ],
    );
  }
}
