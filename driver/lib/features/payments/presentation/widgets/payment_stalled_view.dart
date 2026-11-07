import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../app/theme/app_colors.dart';
import '../../../../app/theme/app_text_styles.dart';
import '../../../../core/widgets/adaptive/app_button.dart';
import '../../../../core/widgets/adaptive/app_link_button.dart';
import '../../domain/payment_attempt.dart';
import '../payment_flow_controller.dart';

// Shown when the operator has not answered after a long wait: the payment is not lost, the server keeps checking it.
class PaymentStalledView extends ConsumerWidget {
  const PaymentStalledView({super.key, required this.attempt});

  final PaymentAttempt attempt;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final flow = ref.read(paymentFlowControllerProvider.notifier);
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              'Confirmation en attente',
              textAlign: TextAlign.center,
              style: AppTextStyles.title,
            ),
            const SizedBox(height: 12),
            Text(
              'Nous n’avons pas encore la réponse de l’opérateur pour votre paiement de ${attempt.amount}. S’il a été confirmé, il apparaîtra dans votre historique ; vous ne serez pas débité deux fois.',
              textAlign: TextAlign.center,
              style: const TextStyle(
                color: AppColors.muted,
                fontSize: 14,
                height: 1.4,
              ),
            ),
            const SizedBox(height: 24),
            AppButton(label: 'Vérifier maintenant', onPressed: flow.recheck),
            const SizedBox(height: 8),
            AppLinkButton(label: 'Retour', onTap: flow.reset),
          ],
        ),
      ),
    );
  }
}
