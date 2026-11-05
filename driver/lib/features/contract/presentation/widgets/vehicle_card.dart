import 'package:flutter/material.dart';

import '../../../../app/theme/app_colors.dart';
import '../../../../app/theme/app_text_styles.dart';
import '../../../../core/platform/app_icons.dart';
import '../../../../core/widgets/adaptive/platform_icon.dart';
import '../../../../core/widgets/display/app_card.dart';
import '../../../../core/widgets/display/circle_badge.dart';
import '../../../../core/widgets/display/license_plate.dart';
import '../../domain/vehicle_info.dart';

// The driver's car
class VehicleCard extends StatelessWidget {
  const VehicleCard({super.key, required this.vehicle});

  final VehicleInfo vehicle;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      padding: const EdgeInsets.all(12),
      child: Row(
        children: [
          const CircleBadge(
            color: AppColors.canvas,
            size: 38,
            child: PlatformIcon(AppIcons.car, size: 20, color: AppColors.ink),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Mon véhicule', style: AppTextStyles.caption),
                const SizedBox(height: 2),
                Text(
                  vehicle.name,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: AppTextStyles.heading,
                ),
              ],
            ),
          ),
          const SizedBox(width: 12),
          FittedBox(fit: BoxFit.scaleDown, child: LicensePlate(vehicle.plate)),
        ],
      ),
    );
  }
}
