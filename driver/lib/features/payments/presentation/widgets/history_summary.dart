import 'package:flutter/material.dart';

import '../../../../app/theme/app_colors.dart';
import '../../../../app/theme/app_text_styles.dart';
import '../../../../core/money/money.dart';
import '../../../../core/widgets/display/app_card.dart';
import '../../domain/payment_attempt.dart';
import '../../domain/payment_status.dart';

// What the driver has paid so far
class HistorySummary extends StatelessWidget {
  const HistorySummary({super.key, required this.attempts});

  final List<PaymentAttempt> attempts;

  @override
  Widget build(BuildContext context) {
    final confirmed = attempts
        .where((a) => a.status == PaymentStatus.success)
        .toList();
    final total = confirmed.isEmpty
        ? null
        : Money(
            confirmed.fold(0, (sum, a) => sum + a.amount.amount),
            confirmed.first.amount.currency,
          );
    return AppCard(
      padding: const EdgeInsets.all(20),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Total payé',
                  style: TextStyle(
                    color: AppColors.muted,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  total?.toString() ?? '0',
                  style: AppTextStyles.display(28),
                ),
              ],
            ),
          ),
          Column(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Text('${confirmed.length}', style: AppTextStyles.display(28)),
              Text(
                confirmed.length > 1 ? 'paiements' : 'paiement',
                style: const TextStyle(
                  color: AppColors.muted,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
