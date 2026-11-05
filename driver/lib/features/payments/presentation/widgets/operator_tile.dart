import '../../../../app/theme/brand_colors.dart';
import 'package:flutter/material.dart';

import '../../../../app/theme/app_colors.dart';
import '../../../../core/platform/app_icons.dart';
import '../../../../core/widgets/adaptive/app_tappable.dart';
import '../../../../core/widgets/adaptive/platform_icon.dart';
import '../../../../core/widgets/display/circle_badge.dart';
import '../../domain/payment_operator.dart';
import '../operator_brand.dart';

class OperatorTile extends StatelessWidget {
  const OperatorTile({
    super.key,
    required this.operator,
    required this.selected,
    required this.onTap,
  });

  final PaymentOperator operator;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final radius = BorderRadius.circular(16);
    return Semantics(
      button: true,
      selected: selected,
      label: operator.label,
      child: AppTappable(
        onTap: onTap,
        borderRadius: radius,
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 150),
          padding: const EdgeInsets.symmetric(vertical: 18, horizontal: 12),
          decoration: BoxDecoration(
            color: selected ? context.brand.primarySoft : Colors.white,
            borderRadius: radius,
            border: Border.all(
              color: selected ? context.brand.primary : AppColors.line,
              width: selected ? 2 : 1,
            ),
          ),
          child: Column(
            children: [
              CircleBadge(
                color: operatorBrandColor(operator.code, context.brand.primary),
                size: 36,
                child: const PlatformIcon(
                  AppIcons.mobile,
                  size: 20,
                  color: Colors.white,
                ),
              ),
              const SizedBox(height: 10),
              Text(
                operator.label,
                textAlign: TextAlign.center,
                style: const TextStyle(fontWeight: FontWeight.w500),
              ),
              const SizedBox(height: 6),
              PlatformIcon(
                selected ? AppIcons.selected : AppIcons.unselected,
                size: 20,
                color: selected ? context.brand.primary : AppColors.line,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
