import 'package:flutter/material.dart';

import '../../../app/theme/app_colors.dart';
import '../../../app/theme/app_text_styles.dart';
import '../../../app/theme/brand_colors.dart';
import '../../platform/app_icon.dart';
import '../../platform/app_icons.dart';
import '../adaptive/app_tappable.dart';
import '../adaptive/platform_icon.dart';
import 'circle_badge.dart';

// A tappable row
class ActionRow extends StatelessWidget {
  const ActionRow({
    super.key,
    required this.icon,
    required this.label,
    required this.onTap,
    this.hint,
    this.destructive = false,
    this.busy = false,
  });

  final AppIcon icon;
  final String label;
  final String? hint;
  final VoidCallback onTap;
  final bool destructive;

  /// While true the row ignores taps (a request is on its way).
  final bool busy;

  @override
  Widget build(BuildContext context) {
    final color = destructive ? AppColors.danger : context.brand.primaryInk;
    final soft = destructive ? AppColors.dangerSoft : context.brand.primarySoft;
    return Semantics(
      button: true,
      label: label,
      child: AppTappable(
        onTap: busy ? () {} : onTap,
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 10),
          child: Row(
            children: [
              CircleBadge(
                color: soft,
                size: 32,
                child: PlatformIcon(icon, size: 16, color: color),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      label,
                      style: AppTextStyles.body.copyWith(
                        color: destructive ? AppColors.danger : AppColors.ink,
                      ),
                    ),
                    if (hint != null) Text(hint!, style: AppTextStyles.caption),
                  ],
                ),
              ),
              const PlatformIcon(
                AppIcons.next,
                size: 16,
                color: AppColors.muted,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
