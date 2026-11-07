import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/router.dart';
import '../../../core/platform/app_icons.dart';
import '../../../core/widgets/adaptive/refreshable_list.dart';
import '../../../core/widgets/feedback/async_view.dart';
import '../../../core/widgets/feedback/empty_state.dart';
import '../../../core/widgets/feedback/screen_skeleton.dart';
import '../../../core/widgets/layout/screen_scaffold.dart';
import '../domain/payment_attempt.dart';
import 'history_filter.dart';
import 'history_grouping.dart';
import 'operator_label.dart';
import 'payments_providers.dart';
import 'widgets/history_group.dart';
import 'widgets/history_summary.dart';
import 'widgets/pill_bar.dart';

class HistoryScreen extends ConsumerStatefulWidget {
  const HistoryScreen({super.key});

  @override
  ConsumerState<HistoryScreen> createState() => _HistoryScreenState();
}

class _HistoryScreenState extends ConsumerState<HistoryScreen> {
  HistoryFilter _filter = HistoryFilter.all;
  HistoryGrouping _grouping = HistoryGrouping.week;

  // The payments by group, newest group first, each group newest payment first.
  Map<DateTime, List<PaymentAttempt>> _grouped(List<PaymentAttempt> attempts) {
    final sorted = [...attempts]
      ..sort((a, b) => b.createdAt.compareTo(a.createdAt));
    final groups = <DateTime, List<PaymentAttempt>>{};
    for (final attempt in sorted) {
      groups
          .putIfAbsent(_grouping.start(attempt.createdAt), () => [])
          .add(attempt);
    }
    return groups;
  }

  @override
  Widget build(BuildContext context) {
    final operators = ref.watch(operatorsProvider).asData?.value;
    return ScreenScaffold(
      title: 'Historique',
      subtitle: 'Vos paiements de loyer',
      body: RefreshableList(
        onRefresh: () async {
          ref.invalidate(paymentHistoryProvider);
          await ref.read(paymentHistoryProvider.future);
        },
        children: [
          AsyncView(
            value: ref.watch(paymentHistoryProvider),
            onRetry: () => ref.invalidate(paymentHistoryProvider),
            skeleton: const ScreenSkeleton(),
            data: (attempts) {
              if (attempts.isEmpty) {
                return const Padding(
                  padding: EdgeInsets.only(top: 48),
                  child: EmptyState(
                    icon: AppIcons.history,
                    title: 'Aucun paiement pour le moment',
                    message: 'Vos paiements apparaîtront ici.',
                  ),
                );
              }
              final shown = attempts.where(_filter.matches).toList();
              return Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  HistorySummary(attempts: attempts),
                  const SizedBox(height: 16),
                  PillBar<HistoryGrouping>(
                    values: HistoryGrouping.values,
                    labelOf: (grouping) => grouping.label,
                    selected: _grouping,
                    onSelected: (grouping) =>
                        setState(() => _grouping = grouping),
                  ),
                  const SizedBox(height: 10),
                  PillBar<HistoryFilter>(
                    values: HistoryFilter.values,
                    labelOf: (filter) => filter.label,
                    selected: _filter,
                    onSelected: (filter) => setState(() => _filter = filter),
                  ),
                  const SizedBox(height: 16),
                  if (shown.isEmpty)
                    const Padding(
                      padding: EdgeInsets.only(top: 32),
                      child: EmptyState(
                        icon: AppIcons.history,
                        title: 'Aucun paiement dans cette catégorie',
                      ),
                    ),
                  for (final entry in _grouped(shown).entries) ...[
                    HistoryGroup(
                      title: _grouping.title(entry.key, DateTime.now()),
                      attempts: entry.value,
                      operatorName: (attempt) =>
                          operatorLabel(attempt.operatorCode, operators),
                      onOpen: (attempt) =>
                          context.push(Routes.paymentDetail(attempt.id)),
                    ),
                    const SizedBox(height: 18),
                  ],
                ],
              );
            },
          ),
        ],
      ),
    );
  }
}
