import '../../../app/theme/brand_colors.dart';
import 'package:flutter/material.dart';

import '../../../app/theme/app_colors.dart';
import '../../platform/app_icon.dart';
import '../adaptive/platform_icon.dart';
import 'circle_badge.dart';

// An icon, a small label and its value; used for profile and contract details.
class InfoRow extends StatelessWidget {
  const InfoRow({
    super.key,
    required this.icon,
    required this.label,
    required this.value,
    this.trailing,
  });

  final AppIcon icon;
  final String label;
  final String value;
  final Widget? trailing;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 12),
      child: Row(
        children: [
          CircleBadge(
            color: context.brand.primarySoft,
            size: 38,
            child: PlatformIcon(
              icon,
              size: 19,
              color: context.brand.primaryInk,
            ),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  label,
                  style: const TextStyle(
                    color: AppColors.muted,
                    fontSize: 12.5,
                    fontWeight: FontWeight.w500,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  value,
                  style: const TextStyle(
                    color: AppColors.ink,
                    fontSize: 15.5,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ],
            ),
          ),
          ?trailing,
        ],
      ),
    );
  }
}
