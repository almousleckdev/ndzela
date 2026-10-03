import 'package:flutter/material.dart';

import 'app_colors.dart';

// Names, titles and amounts use the display face; running text uses the theme's body face.
abstract final class AppTextStyles {
  static const displayFamily = 'BricolageGrotesque';

  static TextStyle display(
    double size, {
    FontWeight weight = FontWeight.w700,
    Color color = AppColors.ink,
    double? height,
  }) => TextStyle(
    fontFamily: displayFamily,
    fontSize: size,
    fontWeight: weight,
    fontVariations: [FontVariation('wght', weight.value.toDouble())],
    color: color,
    height: height,
    letterSpacing: size >= 24 ? -0.4 : 0,
  );
}
