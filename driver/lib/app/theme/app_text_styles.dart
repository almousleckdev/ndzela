import 'package:flutter/material.dart';

import 'app_colors.dart';

// The type scale: regular weight everywhere, medium only for names and figures.
abstract final class AppTextStyles {
  static const displayFamily = 'BricolageGrotesque';

  static TextStyle display(
    double size, {
    FontWeight weight = FontWeight.w500,
    Color color = AppColors.ink,
    double? height,
  }) => TextStyle(
    fontFamily: displayFamily,
    fontSize: size,
    fontWeight: weight,
    fontVariations: [FontVariation('wght', weight.value.toDouble())],
    color: color,
    height: height,
  );

  // Screen titles
  static TextStyle get title => display(20);

  // Card and section headings
  static TextStyle get heading => display(16);

  // Amounts and values
  static TextStyle get figure => display(16);

  // The one big number of a screen
  static TextStyle get hero => display(28);

  static const body = TextStyle(
    fontSize: 14,
    fontWeight: FontWeight.w400,
    color: AppColors.ink,
  );

  static const label = TextStyle(
    fontSize: 13,
    fontWeight: FontWeight.w400,
    color: AppColors.muted,
  );

  static const caption = TextStyle(
    fontSize: 12,
    fontWeight: FontWeight.w400,
    color: AppColors.muted,
  );

  // Buttons and chips
  static const action = TextStyle(fontSize: 14, fontWeight: FontWeight.w500);
}
