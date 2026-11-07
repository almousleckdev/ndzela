import 'package:flutter/material.dart';

import '../../../../app/theme/app_colors.dart';
import '../../../../core/money/money.dart';
import '../../../../core/widgets/display/app_card.dart';
import '../../domain/payment_attempt.dart';
import '../../domain/payment_status.dart';
import 'payment_history_tile.dart';

// The payments of one day, week or month under its title and what was paid, in a single card with hairlines between them.
class HistoryGroup extends StatelessWidget {
  const HistoryGroup({
    super.key,
    required this.title,
    required this.attempts,
    required this.operatorName,
    required this.onOpen,
  });

  final String title;
  final List<PaymentAttempt> attempts;
  final String Function(PaymentAttempt attempt) operatorName;
  final void Function(PaymentAttempt attempt) onOpen;

  Money? get _paid {
    final confirmed = attempts.where((a) => a.status == PaymentStatus.success);
    if (confirmed.isEmpty) return null;
    return Money(
      confirmed.fold(0, (sum, a) => sum + a.amount.amount),
      confirmed.first.amount.currency,
    );
  }

  @override
  Widget build(BuildContext context) {
    const style = TextStyle(
      color: AppColors.muted,
      fontWeight: FontWeight.w500,
      fontSize: 13,
    );
    final paid = _paid;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(4, 0, 4, 8),
          child: Row(
            children: [
              Expanded(child: Text(title, style: style)),
              if (paid != null) Text(paid.toString(), style: style),
            ],
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
