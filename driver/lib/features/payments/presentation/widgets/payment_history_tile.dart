import 'package:flutter/material.dart';

import '../../../../app/theme/app_colors.dart';
import '../../../../app/theme/app_text_styles.dart';
import '../../../../app/theme/brand_colors.dart';
import '../../../../core/formatting/date_format.dart';
import '../../../../core/platform/app_icons.dart';
import '../../../../core/widgets/adaptive/app_tappable.dart';
import '../../../../core/widgets/adaptive/platform_icon.dart';
import '../../../../core/widgets/display/circle_badge.dart';
import '../../domain/payment_attempt.dart';
import '../payment_status_look.dart';

// One payment as a row of the history
class PaymentHistoryTile extends StatelessWidget {
  const PaymentHistoryTile({
    super.key,
    required this.attempt,
    required this.operatorName,
    required this.onTap,
  });

  final PaymentAttempt attempt;
  final String operatorName;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final look = PaymentStatusLook.of(attempt.status, context.brand);
    return Semantics(
      button: true,
      label: 'Détails du paiement de ${attempt.amount}',
      child: AppTappable(
        onTap: onTap,
        borderRadius: BorderRadius.circular(18),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
          child: Row(
            children: [
              CircleBadge(
                color: look.soft,
                size: 42,
                child: PlatformIcon(look.icon, size: 21, color: look.color),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      operatorName,
                      style: const TextStyle(
                        fontWeight: FontWeight.w700,
                        fontSize: 15,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      formatDateTime(attempt.createdAt),
                      style: const TextStyle(
                        color: AppColors.muted,
                        fontSize: 13,
                      ),
                    ),
                  ],
                ),
              ),
              Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Text(
                    attempt.amount.toString(),
                    style: AppTextStyles.display(16),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    look.label,
                    style: TextStyle(
                      color: look.color,
                      fontSize: 12.5,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ],
              ),
              const SizedBox(width: 6),
              const PlatformIcon(
                AppIcons.next,
                size: 18,
                color: AppColors.muted,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
