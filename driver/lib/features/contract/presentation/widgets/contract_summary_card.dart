import 'package:flutter/material.dart';

import '../../../../app/theme/app_colors.dart';
import '../../../../app/theme/app_text_styles.dart';
import '../../../../core/formatting/date_format.dart';
import '../../../../core/widgets/display/app_card.dart';
import '../../../../core/widgets/display/figure_tile.dart';
import '../../domain/contract_details.dart';

// A glance at the contract
class ContractSummaryCard extends StatelessWidget {
  const ContractSummaryCard({super.key, required this.contract, this.now});

  final ContractDetails contract;

  // The current time; only tests set it.
  final DateTime? now;

  int get _daysLeft {
    final left = contract.endDate.difference(now ?? DateTime.now()).inDays;
    return left < 0 ? 0 : left;
  }

  @override
  Widget build(BuildContext context) {
    return AppCard(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text('Mon contrat', style: AppTextStyles.display(18)),
              ),
              Text(
                contract.number,
                style: const TextStyle(
                  color: AppColors.muted,
                  fontWeight: FontWeight.w600,
                  fontSize: 13,
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          IntrinsicHeight(
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Expanded(
                  child: FigureTile(
                    label: 'Loyer par jour',
                    value: contract.dailyRent.toString(),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: FigureTile(
                    label: 'Fin du contrat',
                    value: formatDateShort(contract.endDate),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 10),
          FigureTile(label: 'Jours restants', value: '$_daysLeft jours'),
        ],
      ),
    );
  }
}
