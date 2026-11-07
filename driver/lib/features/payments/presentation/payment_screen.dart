import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/platform/app_icons.dart';
import '../../../core/widgets/adaptive/app_icon_button.dart';
import '../../../core/widgets/feedback/async_view.dart';
import '../../../core/widgets/feedback/empty_state.dart';
import '../../../core/widgets/feedback/screen_skeleton.dart';
import '../../../core/widgets/layout/screen_scaffold.dart';
import '../../auth/presentation/confirm_logout.dart';
import '../domain/driver_access.dart';
import '../domain/payment_flow_state.dart';
import 'driver_access_provider.dart';
import 'payment_flow_controller.dart';
import 'payments_providers.dart';
import 'widgets/access_restriction_banner.dart';
import 'widgets/payment_form.dart';
import 'widgets/payment_result_view.dart';
import 'widgets/payment_stalled_view.dart';
import 'widgets/payment_tracking_view.dart';
import 'widgets/due_banner.dart';

class PaymentScreen extends ConsumerWidget {
  const PaymentScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final restricted =
        ref.watch(driverAccessProvider) == DriverAccess.restricted;
    return ScreenScaffold(
      title: 'Paiement',
      subtitle: restricted ? 'Accès limité au paiement' : 'Réglez votre loyer',
      // A restricted driver has no tabs, so signing out has to be possible from here.
      trailing: restricted
          ? AppIconButton(
              icon: AppIcons.logout,
              tooltip: 'Se déconnecter',
              onPressed: () => confirmLogout(context, ref),
            )
          : null,
      body: switch (ref.watch(paymentFlowControllerProvider)) {
        PaymentFlowTracking(:final attempt) => PaymentTrackingView(
          attempt: attempt,
        ),
        PaymentFlowStalled(:final attempt) => PaymentStalledView(
          attempt: attempt,
        ),
        PaymentFlowFinished(:final attempt) => PaymentResultView(
          attempt: attempt,
        ),
        PaymentFlowIdle() => AsyncView(
          value: ref.watch(arrearsProvider),
          onRetry: () => ref.invalidate(arrearsProvider),
          skeleton: const ScreenSkeleton(),
          data: (arrears) => Column(
            children: [
              if (arrears.restricted) AccessRestrictionBanner(arrears: arrears),
              if (arrears.canPay && arrears.settleBy != null)
                DueBanner(arrears: arrears, now: DateTime.now()),
              Expanded(
                child: arrears.canPay
                    ? PaymentForm(arrears: arrears)
                    : const EmptyState(
                        icon: AppIcons.done,
                        title: 'Aucun loyer à régler',
                        message: 'Vous êtes à jour. Merci !',
                      ),
              ),
            ],
          ),
        ),
      },
    );
  }
}
