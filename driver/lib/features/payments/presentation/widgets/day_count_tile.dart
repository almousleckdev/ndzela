import '../../../../app/theme/brand_colors.dart';
import 'package:flutter/material.dart';

import '../../../../app/theme/app_colors.dart';
import '../../../../core/platform/app_icons.dart';
import '../../../../core/widgets/adaptive/app_tappable.dart';
import '../../../../core/widgets/adaptive/platform_icon.dart';
import '../../domain/payment_option.dart';

// One number of days the driver can pay
class DayCountTile extends StatelessWidget {
  const DayCountTile({
    super.key,
    required this.option,
    required this.selected,
    required this.onTap,
  });

  final PaymentOption option;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final radius = BorderRadius.circular(16);
    final label = '${option.days} ${option.days > 1 ? 'jours' : 'jour'}';
    return Semantics(
      button: true,
      selected: selected,
      label: label,
      child: AppTappable(
        onTap: onTap,
        borderRadius: radius,
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 150),
          padding: const EdgeInsets.symmetric(vertical: 14, horizontal: 16),
          decoration: BoxDecoration(
            color: selected ? context.brand.primarySoft : Colors.white,
            borderRadius: radius,
            border: Border.all(
              color: selected ? context.brand.primary : AppColors.line,
              width: selected ? 2 : 1,
            ),
          ),
          child: Row(
            children: [
              PlatformIcon(
                selected ? AppIcons.selected : AppIcons.unselected,
                size: 20,
                color: selected ? context.brand.primary : AppColors.line,
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  label,
                  style: const TextStyle(fontWeight: FontWeight.w500),
                ),
              ),
              Text(
                option.amount.toString(),
                style: const TextStyle(fontWeight: FontWeight.w500),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
