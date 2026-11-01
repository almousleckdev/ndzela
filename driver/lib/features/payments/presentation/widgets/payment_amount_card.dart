import 'package:flutter/material.dart';

import '../../../../app/theme/app_colors.dart';
import '../../../../core/widgets/display/app_card.dart';
import '../../domain/arrears_summary.dart';
import '../../domain/payment_option.dart';

class PaymentAmountCard extends StatelessWidget {
  const PaymentAmountCard({super.key, required this.arrears, this.selected});

  final ArrearsSummary arrears;

  // The number of days chosen, when the driver can choose
  final PaymentOption? selected;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            selected == null
                ? 'Montant total à régulariser'
                : 'Montant à régler',
            style: TextStyle(
              color: AppColors.muted,
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            (selected?.amount ?? arrears.total).toString(),
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
          Text(_note(), style: TextStyle(color: AppColors.muted, fontSize: 13)),
        ],
      ),
    );
  }

  String _note() {
    if (arrears.options.isEmpty) {
      return 'Le paiement couvre la totalité de vos loyers en retard.';
    }
    if (arrears.options.length > 1) {
      return 'Choisissez combien de jours régler : les plus anciens sont payés en premier.';
    }
    return arrears.unpaidDays == 1
        ? 'Votre jour de loyer en retard se règle en une fois.'
        : 'Vos ${arrears.unpaidDays} jours de loyer en retard se règlent ensemble.';
  }
}
