import 'package:flutter/material.dart';

import '../../../app/theme/app_colors.dart';
import '../../../app/theme/brand_colors.dart';

// A thin rounded bar filled from the left, the same drawing on iOS and Android.
class ProgressBar extends StatelessWidget {
  const ProgressBar({super.key, required this.value, this.semanticsLabel});

  // From 0 to 1.
  final double value;
  final String? semanticsLabel;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label: semanticsLabel,
      value: '${(value * 100).round()} %',
      child: ClipRRect(
        borderRadius: BorderRadius.circular(4),
        child: Stack(
          children: [
            Container(height: 8, color: AppColors.canvas),
            FractionallySizedBox(
              widthFactor: value.clamp(0, 1).toDouble(),
              child: Container(height: 8, color: context.brand.primary),
            ),
          ],
        ),
      ),
    );
  }
}
