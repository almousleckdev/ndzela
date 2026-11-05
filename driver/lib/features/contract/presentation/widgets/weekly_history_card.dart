import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../../../app/theme/app_text_styles.dart';
import '../../../../core/widgets/display/progress_bar.dart';
import '../../../../core/widgets/display/section_card.dart';
import '../../../../core/widgets/feedback/async_view.dart';
import '../../../payments/domain/weekly_total.dart';
import '../../../payments/presentation/payments_providers.dart';

// What was paid in each of the last weeks.
class WeeklyHistoryCard extends ConsumerWidget {
  const WeeklyHistoryCard({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return SectionCard(
      title: 'Historique hebdomadaire',
      child: AsyncView(
        value: ref.watch(paymentHistoryProvider),
        onRetry: () => ref.invalidate(paymentHistoryProvider),
        data: (attempts) {
          final weeks = WeeklyTotal.lastWeeks(attempts, DateTime.now());
          final most = weeks.fold(
            0,
            (m, w) => w.paid.amount > m ? w.paid.amount : m,
          );
          return Column(
            children: [
              for (var i = 0; i < weeks.length; i++) ...[
                if (i > 0) const Divider(height: 1),
                _WeekRow(
                  week: weeks[i],
                  current: i == 0,
                  share: most == 0 ? 0 : weeks[i].paid.amount / most,
                ),
              ],
            ],
          );
        },
      ),
    );
  }
}

class _WeekRow extends StatelessWidget {
  const _WeekRow({
    required this.week,
    required this.current,
    required this.share,
  });

  final WeeklyTotal week;
  final bool current;
  final double share;

  String get _title {
    final day = DateFormat('d MMM', 'fr');
    final range = '${day.format(week.start)} – ${day.format(week.end)}';
    return current ? 'Cette semaine · $range' : range;
  }

  String get _detail => week.payments == 0
      ? 'Aucun paiement'
      : '${week.payments} ${week.payments > 1 ? 'paiements' : 'paiement'} · '
            '${week.days} ${week.days > 1 ? 'jours' : 'jour'}';

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 10),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(child: Text(_title, style: AppTextStyles.body)),
              Text(
                week.paid.toString(),
                style: AppTextStyles.figure.copyWith(fontSize: 14),
              ),
            ],
          ),
          const SizedBox(height: 2),
          Text(_detail, style: AppTextStyles.caption),
          const SizedBox(height: 6),
          ProgressBar(value: share, semanticsLabel: _title),
        ],
      ),
    );
  }
}
