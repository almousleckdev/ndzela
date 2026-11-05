import 'package:flutter/material.dart';

import '../../../../app/theme/app_text_styles.dart';
import '../../../../core/formatting/date_format.dart';
import '../../../../core/widgets/display/app_card.dart';
import '../../../../core/widgets/display/figure_tile.dart';
import '../../../../core/widgets/display/progress_bar.dart';
import '../../domain/contract_details.dart';

// A glance at the contract: rent, end, days since day one and what remains to pay
class ContractSummaryCard extends StatelessWidget {
  const ContractSummaryCard({super.key, required this.contract});

  final ContractDetails contract;

  @override
  Widget build(BuildContext context) {
    final progress = contract.progress;
    return AppCard(
      padding: const EdgeInsets.all(14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text('Mon contrat', style: AppTextStyles.heading),
              ),
              Text(contract.number, style: AppTextStyles.caption),
            ],
          ),
          const SizedBox(height: 12),
          _pair(
            FigureTile(
              label: 'Loyer par jour',
              value: contract.dailyRent.toString(),
            ),
            FigureTile(
              label: 'Fin du contrat',
              value: formatDateShort(contract.endDate),
            ),
          ),
          const SizedBox(height: 8),
          _pair(
            FigureTile(
              label: 'Jours payés',
              value: '${progress.paidDays} sur ${progress.elapsedDays}',
            ),
            FigureTile(
              label: 'Jours restants',
              value: '${progress.daysLeft} jours',
            ),
          ),
          const SizedBox(height: 14),
          ProgressBar(
            value: progress.elapsedShare,
            semanticsLabel: 'Avancement du contrat',
          ),
          const SizedBox(height: 6),
          Text(
            'Jour ${progress.elapsedDays} sur ${progress.totalDays} · '
            'reste à régler ${progress.remainingAmount}',
            style: AppTextStyles.caption,
          ),
        ],
      ),
    );
  }

  Widget _pair(Widget left, Widget right) => IntrinsicHeight(
    child: Row(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Expanded(child: left),
        const SizedBox(width: 8),
        Expanded(child: right),
      ],
    ),
  );
}
