import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../app/theme/app_colors.dart';
import '../../../../app/theme/app_text_styles.dart';
import '../../../../core/error/error_messages.dart';
import '../../../../core/widgets/display/app_card.dart';
import '../../../../core/widgets/display/app_divider.dart';
import '../../../../core/widgets/display/label_value_row.dart';
import '../../domain/arrears_summary.dart';
import '../../domain/payment_quote.dart';
import '../payment_form_controller.dart';

// The Récapitulatif
class PaymentSummaryCard extends StatelessWidget {
  const PaymentSummaryCard({
    super.key,
    required this.arrears,
    required this.form,
    required this.quote,
    required this.operatorName,
    this.onRetry,
  });

  final ArrearsSummary arrears;
  final PaymentFormState form;
  final AsyncValue<PaymentQuote>? quote;
  final String? operatorName;

  // Asks for the fee again after a failure
  final VoidCallback? onRetry;

  @override
  Widget build(BuildContext context) {
    final quoted = quote?.asData?.value;
    final waiting = quote != null && quote!.isLoading;
    return AppCard(
      child: Column(
        children: [
          LabelValueRow(
            'Nombre de jours',
            '${form.days ?? arrears.daysToSettle}',
          ),
          LabelValueRow('Opérateur', operatorName ?? 'À choisir'),
          LabelValueRow('Numéro', form.phone.isEmpty ? 'À saisir' : form.phone),
          if (quoted?.accountHolder != null)
            LabelValueRow('Compte au nom de', quoted!.accountHolder!),
          const Padding(
            padding: EdgeInsets.symmetric(vertical: 8),
            child: AppDivider(),
          ),
          LabelValueRow(
            'Montant',
            quoted?.amount.toString() ?? (waiting ? '…' : '—'),
          ),
          LabelValueRow(
            'Frais estimés',
            quoted?.fee.toString() ?? (waiting ? '…' : '—'),
          ),
          LabelValueRow(
            'Total',
            quoted?.total.toString() ?? (waiting ? '…' : '—'),
            strong: true,
          ),
          if (quote != null && quote!.hasError && !waiting) ...[
            const SizedBox(height: 12),
            Text(
              messageFor(quote!.error!),
              style: AppTextStyles.caption.copyWith(color: AppColors.danger),
            ),
            if (onRetry != null)
              TextButton(onPressed: onRetry, child: const Text('Réessayer')),
          ],
        ],
      ),
    );
  }
}
