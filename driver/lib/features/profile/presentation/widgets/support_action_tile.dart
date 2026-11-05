import 'package:flutter/material.dart';

import '../../../../app/theme/app_colors.dart';
import '../../../../app/theme/app_text_styles.dart';
import '../../../../core/platform/app_icon.dart';
import '../../../../core/widgets/adaptive/app_tappable.dart';
import '../../../../core/widgets/adaptive/platform_icon.dart';

// One way to reach support: the channel's own icon over its name.
class SupportActionTile extends StatelessWidget {
  const SupportActionTile({
    super.key,
    required this.icon,
    required this.label,
    required this.color,
    required this.background,
    required this.onTap,
  });

  final AppIcon icon;
  final String label;
  final Color color;
  final Color background;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final radius = BorderRadius.circular(12);
    return Semantics(
      button: true,
      label: label,
      child: AppTappable(
        onTap: onTap,
        borderRadius: radius,
        child: Container(
          padding: const EdgeInsets.symmetric(vertical: 12),
          decoration: BoxDecoration(
            color: AppColors.canvas,
            borderRadius: radius,
          ),
          child: Column(
            children: [
              Container(
                width: 36,
                height: 36,
                decoration: BoxDecoration(
                  color: background,
                  shape: BoxShape.circle,
                ),
                child: PlatformIcon(icon, size: 18, color: color),
              ),
              const SizedBox(height: 6),
              Text(label, style: AppTextStyles.label),
            ],
          ),
        ),
      ),
    );
  }
}
