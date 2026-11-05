import '../../../../app/theme/brand_colors.dart';
import 'package:flutter/material.dart';

import '../../../../app/theme/app_colors.dart';
import '../../../../app/theme/app_text_styles.dart';
import '../../../../core/widgets/adaptive/activity_indicator.dart';
import '../../domain/payment_attempt.dart';

// Shown while the driver approves the payment on their phone
class PaymentTrackingView extends StatelessWidget {
  const PaymentTrackingView({super.key, required this.attempt});

  final PaymentAttempt attempt;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            AppActivityIndicator(size: 56, color: context.brand.primary),
            const SizedBox(height: 28),
            Text(
              'Validez sur votre téléphone',
              textAlign: TextAlign.center,
              style: AppTextStyles.title,
            ),
            const SizedBox(height: 12),
            Text(
              'Une demande de paiement de ${attempt.amount} vient d\'être envoyée. Saisissez votre code secret pour confirmer.',
              textAlign: TextAlign.center,
              style: const TextStyle(
                color: AppColors.muted,
                fontSize: 14,
                height: 1.4,
              ),
            ),
            const SizedBox(height: 16),
            const Text(
              'Cela peut prendre une minute. Ne quittez pas cet écran.',
              textAlign: TextAlign.center,
              style: TextStyle(color: AppColors.muted),
            ),
          ],
        ),
      ),
    );
  }
}
