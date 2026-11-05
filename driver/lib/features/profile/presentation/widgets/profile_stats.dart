import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/widgets/display/figure_tile.dart';
import '../../../contract/presentation/contract_providers.dart';

// Three figures of the contract at a glance, under the profile band.
class ProfileStats extends ConsumerWidget {
  const ProfileStats({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final contract = ref.watch(contractDetailsProvider).asData?.value;
    if (contract == null) return const SizedBox.shrink();
    return Row(
      children: [
        Expanded(
          child: FigureTile(label: 'Contrat', value: contract.number),
        ),
        const SizedBox(width: 8),
        Expanded(
          child: FigureTile(
            label: 'Loyer par jour',
            value: contract.dailyRent.toString(),
          ),
        ),
        const SizedBox(width: 8),
        Expanded(
          child: FigureTile(
            label: 'Jours payés',
            value:
                '${contract.progress.paidDays}/${contract.progress.elapsedDays}',
          ),
        ),
      ],
    );
  }
}
