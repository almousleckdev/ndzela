import 'package:flutter/material.dart';

import '../../../app/theme/app_colors.dart';
import '../../../app/theme/brand_colors.dart';
import '../../../core/platform/app_icon.dart';
import '../../../core/platform/app_icons.dart';
import '../domain/payment_status.dart';

// How a payment status is shown
class PaymentStatusLook {
  const PaymentStatusLook(this.label, this.color, this.soft, this.icon);

  final String label;
  final Color color;
  final Color soft;
  final AppIcon icon;

  static PaymentStatusLook of(PaymentStatus status, BrandColors brand) =>
      switch (status) {
        PaymentStatus.success => const PaymentStatusLook(
          'Confirmé',
          AppColors.success,
          AppColors.successSoft,
          AppIcons.done,
        ),
        PaymentStatus.pending => PaymentStatusLook(
          'En attente',
          brand.primaryInk,
          brand.primarySoft,
          AppIcons.late,
        ),
        PaymentStatus.failed => const PaymentStatusLook(
          'Échoué',
          AppColors.danger,
          AppColors.dangerSoft,
          AppIcons.error,
        ),
        PaymentStatus.expired => const PaymentStatusLook(
          'Expiré',
          AppColors.muted,
          AppColors.line,
          AppIcons.late,
        ),
      };
}
