import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router.dart';
import '../../../../app/theme/app_colors.dart';
import '../../../../app/theme/app_text_styles.dart';
import '../../../../core/platform/app_icons.dart';
import '../../../../core/widgets/adaptive/app_button.dart';
import '../../../../core/widgets/adaptive/platform_icon.dart';
import '../../../../core/widgets/display/circle_badge.dart';
import '../../../profile/presentation/profile_providers.dart';
import '../../domain/payment_attempt.dart';
import '../../domain/payment_status.dart';
import '../operator_label.dart';
import '../payment_flow_controller.dart';
import '../payments_providers.dart';
import 'contact_support_button.dart';
import 'payment_receipt.dart';
import 'share_receipt_button.dart';

// The operator's final answer
class PaymentResultView extends ConsumerWidget {
  const PaymentResultView({super.key, required this.attempt});

  final PaymentAttempt attempt;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final paid = attempt.status == PaymentStatus.success;
    final flow = ref.read(paymentFlowControllerProvider.notifier);
    return ListView(
      padding: const EdgeInsets.all(24),
      children: [
        const SizedBox(height: 8),
        Center(
          child: CircleBadge(
            color: paid ? AppColors.successSoft : AppColors.dangerSoft,
            size: 84,
            child: PlatformIcon(
              paid ? AppIcons.done : AppIcons.error,
              size: 44,
              color: paid ? AppColors.success : AppColors.danger,
            ),
          ),
        ),
        const SizedBox(height: 20),
        Text(
          paid ? 'Paiement confirmé' : "Le paiement n'a pas abouti",
          textAlign: TextAlign.center,
          style: AppTextStyles.display(26),
        ),
        const SizedBox(height: 10),
        Text(
          _message(),
          textAlign: TextAlign.center,
          style: const TextStyle(
            color: AppColors.muted,
            fontSize: 15,
            height: 1.4,
          ),
        ),
        const SizedBox(height: 24),
        PaymentReceipt(
          attempt: attempt,
          operatorName: operatorLabel(
            attempt.operatorCode,
            ref.watch(operatorsProvider).asData?.value,
          ),
          payerName: ref.watch(driverProfileProvider).asData?.value.fullName,
        ),
        const SizedBox(height: 28),
        if (paid) ...[
          ShareReceiptButton(attempt: attempt),
          const SizedBox(height: 12),
        ],
        AppButton(
          label: paid ? "Retour à l'accueil" : 'Réessayer',
          onPressed: () {
            flow.reset();
            if (paid) context.go(Routes.home);
          },
        ),
        if (!paid) ...[
          const SizedBox(height: 12),
          ContactSupportButton(reference: attempt.reference),
        ],
      ],
    );
  }

  String _message() => switch (attempt.status) {
    PaymentStatus.success => 'Merci ! Votre paiement a bien été enregistré.',
    PaymentStatus.expired =>
      "Nous n'avons pas reçu de confirmation. Si vous avez été débité, le paiement sera enregistré automatiquement.",
    _ =>
      "L'opérateur n'a pas confirmé le paiement : aucun montant n'a été enregistré. Causes possibles : solde Mobile Money insuffisant, code secret incorrect, demande annulée ou expirée.",
  };
}
