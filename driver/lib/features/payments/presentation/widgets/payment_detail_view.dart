import '../../../../app/theme/brand_colors.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../app/theme/app_colors.dart';
import '../../../../app/theme/app_text_styles.dart';
import '../../../../core/widgets/adaptive/platform_icon.dart';
import '../../../../core/widgets/display/circle_badge.dart';
import '../../../../core/widgets/display/status_chip.dart';
import '../../../profile/presentation/profile_providers.dart';
import '../../domain/payment_attempt.dart';
import '../../domain/payment_status.dart';
import '../payment_status_look.dart';
import 'contact_support_button.dart';
import 'payment_receipt.dart';
import 'share_receipt_button.dart';

// One payment from the history
class PaymentDetailView extends ConsumerWidget {
  const PaymentDetailView({
    super.key,
    required this.attempt,
    required this.operatorName,
  });

  final PaymentAttempt attempt;
  final String operatorName;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final look = PaymentStatusLook.of(attempt.status, context.brand);
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Center(
          child: CircleBadge(
            color: look.soft,
            size: 56,
            child: PlatformIcon(look.icon, size: 36, color: look.color),
          ),
        ),
        const SizedBox(height: 16),
        Text(
          attempt.amount.toString(),
          textAlign: TextAlign.center,
          style: AppTextStyles.title,
        ),
        const SizedBox(height: 10),
        Center(
          child: StatusChip(
            label: look.label,
            color: look.color,
            background: look.soft,
          ),
        ),
        const SizedBox(height: 12),
        Text(
          _message(),
          textAlign: TextAlign.center,
          style: const TextStyle(color: AppColors.muted, height: 1.4),
        ),
        const SizedBox(height: 16),
        PaymentReceipt(
          attempt: attempt,
          operatorName: operatorName,
          payerName: ref.watch(driverProfileProvider).asData?.value.fullName,
        ),
        if (attempt.status == PaymentStatus.success) ...[
          const SizedBox(height: 16),
          ShareReceiptButton(attempt: attempt),
        ],
        if (attempt.status == PaymentStatus.failed ||
            attempt.status == PaymentStatus.expired) ...[
          const SizedBox(height: 16),
          ContactSupportButton(reference: attempt.reference),
        ],
      ],
    );
  }

  String _message() => switch (attempt.status) {
    PaymentStatus.success =>
      'Ce paiement a bien été enregistré sur votre compte.',
    PaymentStatus.failed =>
      "L'opérateur n'a pas confirmé ce paiement : aucun loyer n'a été enregistré.",
    PaymentStatus.expired =>
      'Aucune confirmation reçue. Si vous avez été débité, le paiement sera enregistré automatiquement.',
    PaymentStatus.pending => 'En attente de votre validation sur le téléphone.',
  };
}
