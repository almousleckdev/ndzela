import 'package:flutter/material.dart';

import '../../../../app/theme/app_colors.dart';
import '../../../../app/theme/app_text_styles.dart';
import '../../../../app/theme/brand_colors.dart';
import '../../../../core/platform/app_icons.dart';
import '../../../../core/widgets/adaptive/activity_indicator.dart';
import '../../../../core/widgets/adaptive/app_link_button.dart';
import '../../../../core/widgets/adaptive/platform_icon.dart';
import '../../../../core/widgets/display/circle_badge.dart';
import '../../../../core/widgets/display/status_chip.dart';

// One document: its name, whether it is on file, and optional actions (open, download).
class DocumentRow extends StatelessWidget {
  const DocumentRow({
    super.key,
    required this.name,
    required this.available,
    this.actions,
    this.onAdd,
    this.adding = false,
  });

  final String name;
  final bool available;
  final Widget? actions;

  // Adds the document when it is missing; null for one the driver cannot add
  final VoidCallback? onAdd;
  final bool adding;

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
              AppIcons.document,
              size: 16,
              color: context.brand.primaryInk,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(child: Text(name, style: AppTextStyles.body)),
          if (actions != null && available)
            actions!
          else if (available)
            const StatusChip(
              label: 'Disponible',
              color: AppColors.success,
              background: AppColors.successSoft,
            )
          else ...[
            const StatusChip(
              label: 'Manquant',
              color: AppColors.danger,
              background: AppColors.dangerSoft,
            ),
            if (onAdd != null) ...[
              const SizedBox(width: 8),
              adding
                  ? const AppActivityIndicator(size: 20)
                  : AppLinkButton(label: 'Ajouter', onTap: onAdd),
            ],
          ],
        ],
      ),
    );
  }
}
