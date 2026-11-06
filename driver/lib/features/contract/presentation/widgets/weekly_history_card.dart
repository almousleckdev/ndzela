import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../../../app/theme/app_text_styles.dart';
import '../../../../core/widgets/display/section_card.dart';
import '../../../../core/widgets/feedback/async_view.dart';
import '../../../payments/domain/week_days.dart';
import '../../../payments/domain/weekly_total.dart';
import '../../../payments/presentation/payments_providers.dart';
import '../../../payments/presentation/widgets/week_days_row.dart';

// Each of the last weeks: how much was paid and which rent days are paid, unpaid or waiting.
class WeeklyHistoryCard extends ConsumerWidget {
  const WeeklyHistoryCard({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final now = DateTime.now();
    final attempts =
        ref.watch(paymentHistoryProvider).asData?.value ?? const [];
    final totals = {
      for (final total in WeeklyTotal.lastWeeks(attempts, now))
        total.start: total,
    };
    return SectionCard(
      title: 'Historique hebdomadaire',
      child: AsyncView(
        value: ref.watch(arrearsProvider),
        onRetry: () => ref.invalidate(arrearsProvider),
        data: (arrears) {
          final weeks = WeekDays.lastWeeks(arrears.days, now);
          return Column(
            children: [
              for (var i = 0; i < weeks.length; i++) ...[
                if (i > 0) const Divider(height: 1),
                _WeekBlock(
                  week: weeks[i],
                  current: i == 0,
                  paid: totals[weeks[i].start]?.paid.toString(),
                ),
              ],
            ],
          );
        },
      ),
    );
  }
}

class _WeekBlock extends StatelessWidget {
  const _WeekBlock({required this.week, required this.current, this.paid});

  final WeekDays week;
  final bool current;
  final String? paid;

  String get _title {
    final day = DateFormat('d MMM', 'fr');
    final range = '${day.format(week.start)} – ${day.format(week.end)}';
    return current ? 'Cette semaine · $range' : range;
  }

  String get _counts {
    final parts = [
      if (week.paid > 0) '${week.paid} ${week.paid > 1 ? 'payés' : 'payé'}',
      if (week.unpaid > 0)
        '${week.unpaid} ${week.unpaid > 1 ? 'impayés' : 'impayé'}',
      if (week.awaiting > 0) '${week.awaiting} à régler',
    ];
    return parts.isEmpty ? 'Aucun loyer cette semaine' : parts.join(' · ');
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(child: Text(_title, style: AppTextStyles.body)),
              if (paid != null)
                Text(paid!, style: AppTextStyles.figure.copyWith(fontSize: 14)),
            ],
          ),
          const SizedBox(height: 8),
          WeekDaysRow(week: week),
          const SizedBox(height: 8),
          Text(_counts, style: AppTextStyles.caption),
        ],
      ),
    );
  }
}
