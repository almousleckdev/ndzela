import 'package:flutter/material.dart';

import '../../../../core/formatting/date_format.dart';
import '../../../../core/widgets/display/app_card.dart';
import '../../../../core/widgets/display/app_divider.dart';
import '../../../../core/widgets/display/label_value_row.dart';
import '../../domain/payment_attempt.dart';
import '../../domain/payment_status.dart';
import 'paid_days_card.dart';

// Everything about one payment as three cards
class PaymentReceipt extends StatelessWidget {
  const PaymentReceipt({
    super.key,
    required this.attempt,
    required this.operatorName,
    this.payerName,
  });

  final PaymentAttempt attempt;
  final String operatorName;
  final String? payerName;

  @override
  Widget build(BuildContext context) {
    final paid = attempt.status == PaymentStatus.success;
    return Column(
      children: [
        AppCard(
          child: Column(
            children: [
              LabelValueRow('Date et heure', formatDateTime(attempt.createdAt)),
              if (paid && attempt.completedAt != null)
                LabelValueRow(
                  'Confirmé le',
                  formatDateTime(attempt.completedAt!),
                ),
              if (payerName != null) LabelValueRow('Expéditeur', payerName!),
              if (attempt.phone.isNotEmpty)
                LabelValueRow('Numéro', attempt.phone),
              LabelValueRow('Opérateur', operatorName),
              if (attempt.beneficiary != null)
                LabelValueRow('Bénéficiaire', attempt.beneficiary!),
            ],
          ),
        ),
        if (attempt.days.isNotEmpty) ...[
          const SizedBox(height: 16),
          PaidDaysCard(attempt: attempt),
        ],
        const SizedBox(height: 16),
        AppCard(
          child: Column(
            children: [
              LabelValueRow('Montant', attempt.amount.toString()),
              if (paid && attempt.fee != null)
                LabelValueRow('Frais opérateur', attempt.fee!.toString()),
              if (paid) ...[
                const Padding(
                  padding: EdgeInsets.symmetric(vertical: 8),
                  child: AppDivider(),
                ),
                LabelValueRow(
                  'Total payé',
                  attempt.totalPaid.toString(),
                  strong: true,
                ),
              ],
            ],
          ),
        ),
        const SizedBox(height: 16),
        AppCard(
          child: Column(
            children: [
              LabelValueRow('Référence', attempt.reference),
              if (attempt.providerReference != null)
                LabelValueRow('N° de transaction', attempt.providerReference!),
            ],
          ),
        ),
      ],
    );
  }
}
