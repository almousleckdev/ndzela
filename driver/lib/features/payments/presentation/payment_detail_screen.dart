import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/platform/app_icons.dart';
import '../../../core/widgets/adaptive/app_icon_button.dart';
import '../../../core/widgets/feedback/async_view.dart';
import '../../../core/widgets/layout/screen_scaffold.dart';
import 'operator_label.dart';
import 'payments_providers.dart';
import 'widgets/payment_detail_view.dart';

class PaymentDetailScreen extends ConsumerWidget {
  const PaymentDetailScreen({super.key, required this.id});

  final String id;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final operators = ref.watch(operatorsProvider).asData?.value;
    return ScreenScaffold(
      title: 'Détails du paiement',
      leading: AppIconButton(
        icon: AppIcons.back,
        tooltip: 'Retour',
        onPressed: () => context.pop(),
      ),
      body: AsyncView(
        value: ref.watch(paymentAttemptProvider(id)),
        onRetry: () => ref.invalidate(paymentAttemptProvider(id)),
        data: (attempt) => PaymentDetailView(
          attempt: attempt,
          operatorName: operatorLabel(attempt.operatorCode, operators),
        ),
      ),
    );
  }
}
