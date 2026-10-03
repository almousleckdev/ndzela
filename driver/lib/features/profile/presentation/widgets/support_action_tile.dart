import '../../../../app/theme/brand_colors.dart';
import 'package:flutter/material.dart';

import '../../../../app/theme/app_colors.dart';
import '../../../../core/platform/app_icon.dart';
import '../../../../core/widgets/adaptive/app_tappable.dart';
import '../../../../core/widgets/adaptive/platform_icon.dart';
import '../../../../core/widgets/display/circle_badge.dart';

class SupportActionTile extends StatelessWidget {
  const SupportActionTile({
    super.key,
    required this.icon,
    required this.label,
    required this.onTap,
  });

  final AppIcon icon;
  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final radius = BorderRadius.circular(16);
    return Semantics(
      button: true,
      label: label,
      child: AppTappable(
        onTap: onTap,
        borderRadius: radius,
        child: Container(
          padding: const EdgeInsets.symmetric(vertical: 14),
          decoration: BoxDecoration(
            color: AppColors.canvas,
            borderRadius: radius,
          ),
          child: Column(
            children: [
              CircleBadge(
                color: context.brand.primary,
                size: 44,
                child: PlatformIcon(icon, size: 22, color: Colors.white),
              ),
              const SizedBox(height: 8),
              Text(
                label,
                style: const TextStyle(
                  fontWeight: FontWeight.w700,
                  fontSize: 13,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
