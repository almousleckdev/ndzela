import '../../../../app/theme/brand_colors.dart';
import 'package:flutter/material.dart';

import '../../../../app/theme/app_colors.dart';
import '../../../../core/platform/app_icons.dart';
import '../../../../core/widgets/adaptive/platform_icon.dart';
import '../../../../core/widgets/display/circle_badge.dart';
import '../../../../core/widgets/display/status_chip.dart';

class DocumentRow extends StatelessWidget {
  const DocumentRow({super.key, required this.name, required this.available});

  final String name;
  final bool available;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 14),
      child: Row(
        children: [
          CircleBadge(
            color: context.brand.primarySoft,
            size: 38,
            child: PlatformIcon(
              AppIcons.document,
              size: 19,
              color: context.brand.primaryInk,
            ),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Text(
              name,
              style: const TextStyle(
                fontSize: 15.5,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
          available
              ? const StatusChip(
                  label: 'Disponible',
                  color: AppColors.success,
                  background: AppColors.successSoft,
                )
              : const StatusChip(
                  label: 'Manquant',
                  color: AppColors.danger,
                  background: AppColors.dangerSoft,
                ),
        ],
      ),
    );
  }
}
