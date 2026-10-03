import '../../../app/theme/brand_colors.dart';
import 'package:flutter/material.dart';

import '../../../app/theme/app_colors.dart';

// A number plate
class LicensePlate extends StatelessWidget {
  const LicensePlate(this.number, {super.key, this.countryCode});

  final String number;
  final String? countryCode;

  @override
  Widget build(BuildContext context) {
    return Container(
      clipBehavior: Clip.antiAlias,
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: AppColors.ink, width: 1.6),
      ),
      child: IntrinsicHeight(
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (countryCode != null)
              Container(
                width: 26,
                alignment: Alignment.center,
                color: context.brand.primary,
                child: Text(
                  countryCode!,
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 11,
                    fontWeight: FontWeight.w800,
                  ),
                ),
              ),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 5),
              child: Text(
                number,
                style: const TextStyle(
                  color: AppColors.ink,
                  fontSize: 16,
                  fontWeight: FontWeight.w800,
                  letterSpacing: 1.6,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
