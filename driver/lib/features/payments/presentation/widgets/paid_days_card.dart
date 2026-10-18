import 'package:flutter/material.dart';

import '../../../../core/formatting/date_format.dart';
import '../../../../core/widgets/display/app_card.dart';
import '../../../../core/widgets/display/label_value_row.dart';
import '../../domain/payment_attempt.dart';

// The rent days a payment settled, and when it was paid
class PaidDaysCard extends StatelessWidget {
  const PaidDaysCard({super.key, required this.attempt});

  final PaymentAttempt attempt;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      child: Column(
        children: [
          for (final day in attempt.days)
            LabelValueRow(
              '${day.isPenalty ? 'Pénalité' : 'Loyer'} du ${formatDate(day.day)}',
              '${day.amount} · ${day.onTime ? 'à temps' : 'en retard'}',
            ),
          if (attempt.completedAt != null)
            LabelValueRow('Payé le', formatDateTime(attempt.completedAt!)),
        ],
      ),
    );
  }
}
