import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../../../app/router.dart';
import '../../../../app/theme/app_colors.dart';
import '../../../../app/theme/app_text_styles.dart';
import '../../../../core/widgets/adaptive/app_link_button.dart';
import '../../../../core/widgets/display/row_divider.dart';
import '../../../../core/widgets/display/section_card.dart';
import '../../../../core/widgets/feedback/async_view.dart';
import '../../../payments/domain/week_payments.dart';
import '../../../payments/presentation/operator_label.dart';
import '../../../payments/presentation/payments_providers.dart';
import '../../../payments/presentation/widgets/payment_history_tile.dart';

// The payments the driver made this week, Monday to Sunday noon.
class WeekPaymentsCard extends ConsumerWidget {
  const WeekPaymentsCard({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final operators = ref.watch(operatorsProvider).asData?.value;
    return SectionCard(
      title: 'Paiements de la semaine',
      child: AsyncView(
        value: ref.watch(paymentHistoryProvider),
        onRetry: () => ref.invalidate(paymentHistoryProvider),
        data: (attempts) {
          final week = WeekPayments.of(attempts, DateTime.now());
          final day = DateFormat('EEE d MMM', 'fr');
          return Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Padding(
                padding: const EdgeInsets.only(top: 12, bottom: 4),
                child: Row(
                  children: [
                    Expanded(
                      child: Text(
                        'Du ${day.format(week.start)} au ${day.format(week.closesAt)}, 12:00',
                        style: AppTextStyles.caption,
                      ),
                    ),
                    Text(week.total.toString(), style: AppTextStyles.heading),
                  ],
                ),
              ),
              if (week.payments.isEmpty)
                const Padding(
                  padding: EdgeInsets.symmetric(vertical: 16),
                  child: Text(
                    'Aucun paiement cette semaine.',
                    style: TextStyle(color: AppColors.muted),
                  ),
                )
              else
                for (var i = 0; i < week.payments.length; i++) ...[
                  if (i > 0) const RowDivider(),
                  PaymentHistoryTile(
                    attempt: week.payments[i],
                    operatorName: operatorLabel(
                      week.payments[i].operatorCode,
                      operators,
                    ),
                    onTap: () =>
                        context.push(Routes.paymentDetail(week.payments[i].id)),
                  ),
                ],
              Center(
                child: AppLinkButton(
                  label: 'Voir tout l’historique',
                  onTap: () => context.go(Routes.history),
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}
