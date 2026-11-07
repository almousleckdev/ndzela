import 'package:flutter/material.dart';

import '../../../../app/theme/app_text_styles.dart';
import '../../../../core/widgets/display/license_plate.dart';
import '../../domain/vehicle_info.dart';

// The driver's car: its picture, its name and its plate.
class VehicleCard extends StatelessWidget {
  const VehicleCard({super.key, required this.vehicle});

  final VehicleInfo vehicle;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        AspectRatio(
          aspectRatio: 2.4,
          child: Image.asset(
            'assets/images/image.png',
            fit: BoxFit.cover,
            alignment: const Alignment(0, 0.02),
            filterQuality: FilterQuality.high,
            excludeFromSemantics: true,
          ),
        ),
        const SizedBox(height: 8),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 2),
          child: Row(
            children: [
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
              FittedBox(
                fit: BoxFit.scaleDown,
                child: LicensePlate(vehicle.plate),
              ),
            ],
          ),
        ),
      ],
    );
  }
}
