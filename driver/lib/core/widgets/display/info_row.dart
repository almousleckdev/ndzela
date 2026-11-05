import 'package:flutter/material.dart';

import '../../../app/theme/app_text_styles.dart';
import '../../../app/theme/brand_colors.dart';
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
      padding: const EdgeInsets.symmetric(vertical: 10),
      child: Row(
        children: [
          CircleBadge(
            color: context.brand.primarySoft,
            size: 32,
            child: PlatformIcon(
              icon,
              size: 16,
              color: context.brand.primaryInk,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(label, style: AppTextStyles.caption),
                Text(value, style: AppTextStyles.body),
              ],
            ),
          ),
          ?trailing,
        ],
      ),
    );
  }
}
