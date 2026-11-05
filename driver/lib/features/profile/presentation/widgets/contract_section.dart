import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/formatting/date_format.dart';
import '../../../../core/platform/app_icons.dart';
import '../../../../core/widgets/display/info_row.dart';
import '../../../../core/widgets/display/row_divider.dart';
import '../../../../core/widgets/display/section_card.dart';
import '../../../../core/widgets/feedback/async_view.dart';
import '../../../contract/presentation/contract_providers.dart';

class ContractSection extends ConsumerWidget {
  const ContractSection({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return SectionCard(
      title: 'Détails du contrat',
      child: AsyncView(
        value: ref.watch(contractDetailsProvider),
        onRetry: () => ref.invalidate(contractDetailsProvider),
        data: (contract) => Column(
          children: [
            InfoRow(
              icon: AppIcons.calendar,
              label: 'Date de début',
              value: formatDate(contract.startDate),
            ),
            const RowDivider(),
            InfoRow(
              icon: AppIcons.duration,
              label: 'Durée totale',
              value: formatMonths(contract.durationMonths),
            ),
            const RowDivider(),
            InfoRow(
              icon: AppIcons.money,
              label: 'Total payé',
              value: contract.progress.paidAmount.toString(),
            ),
            const RowDivider(),
            InfoRow(
              icon: AppIcons.money,
              label: 'Reste à régler',
              value: contract.progress.remainingAmount.toString(),
            ),
          ],
        ),
      ),
    );
  }
}
