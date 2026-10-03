import 'package:flutter/material.dart';

import '../../../../app/theme/app_colors.dart';
import '../../../../core/widgets/display/app_card.dart';
import '../../domain/arrears_summary.dart';

class PaymentAmountCard extends StatelessWidget {
  const PaymentAmountCard({super.key, required this.arrears});

  final ArrearsSummary arrears;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Montant total à régulariser',
            style: TextStyle(
              color: AppColors.muted,
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            arrears.total.toString(),
            style: const TextStyle(
              fontSize: 30,
              fontWeight: FontWeight.w800,
              color: AppColors.danger,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            '${arrears.unpaidDays} ${arrears.unpaidDays > 1 ? 'jours' : 'jour'} de loyer en retard',
            style: const TextStyle(color: AppColors.muted),
          ),
          const SizedBox(height: 10),
          const Text(
            'Le paiement couvre la totalité de vos loyers en retard.',
            style: TextStyle(color: AppColors.muted, fontSize: 13),
          ),
        ],
      ),
    );
  }
}
