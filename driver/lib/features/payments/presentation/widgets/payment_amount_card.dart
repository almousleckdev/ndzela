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
            'Montant à régler',
            style: TextStyle(
              color: AppColors.muted,
              fontWeight: FontWeight.w500,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            (selected?.amount ?? arrears.amountToSettle).toString(),
            style: const TextStyle(
              fontSize: 24,
              fontWeight: FontWeight.w500,
              color: AppColors.danger,
            ),
          ),
          const SizedBox(height: 4),
          Text(_summary(), style: const TextStyle(color: AppColors.muted)),
          const SizedBox(height: 10),
          Text(_note(), style: TextStyle(color: AppColors.muted, fontSize: 13)),
        ],
      ),
    );
  }

  String _summary() {
    final days = arrears.daysToSettle;
    final base = '$days ${days > 1 ? 'jours' : 'jour'} de loyer à régler';
    final late = arrears.unpaidDays;
    return late > 0 && late < days ? '$base, dont $late en retard' : base;
  }

  String _note() {
    if (arrears.options.isEmpty) {
      return 'Le paiement couvre la totalité de vos loyers à régler.';
    }
    if (arrears.options.length > 1) {
      return 'Choisissez combien de jours régler : les plus anciens sont payés en premier.';
    }
    return arrears.daysToSettle == 1
        ? 'Votre loyer du jour se règle en une fois.'
        : 'Vos ${arrears.daysToSettle} jours de loyer se règlent ensemble.';
  }
}
