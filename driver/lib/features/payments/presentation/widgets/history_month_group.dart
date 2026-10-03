import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../../app/theme/app_colors.dart';
import '../../../../core/widgets/display/app_card.dart';
import '../../domain/payment_attempt.dart';
import 'payment_history_tile.dart';

// The payments of one month under its name, in a single card with hairlines between them.
class HistoryMonthGroup extends StatelessWidget {
  const HistoryMonthGroup({
    super.key,
    required this.month,
    required this.attempts,
    required this.operatorName,
    required this.onOpen,
  });

  final DateTime month;
  final List<PaymentAttempt> attempts;
  final String Function(PaymentAttempt attempt) operatorName;
  final void Function(PaymentAttempt attempt) onOpen;

  @override
  Widget build(BuildContext context) {
    final title = DateFormat('MMMM y', 'fr').format(month);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(4, 0, 4, 8),
          child: Text(
            title[0].toUpperCase() + title.substring(1),
            style: const TextStyle(
              color: AppColors.muted,
              fontWeight: FontWeight.w700,
              fontSize: 14,
            ),
          ),
        ),
        AppCard(
          padding: EdgeInsets.zero,
          child: Column(
            children: [
              for (var i = 0; i < attempts.length; i++) ...[
                if (i > 0)
                  const Divider(height: 1, color: AppColors.line, indent: 72),
                PaymentHistoryTile(
                  attempt: attempts[i],
                  operatorName: operatorName(attempts[i]),
                  onTap: () => onOpen(attempts[i]),
                ),
              ],
            ],
          ),
        ),
      ],
    );
  }
}
