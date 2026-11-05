import 'package:flutter/material.dart';

import 'app_colors.dart';
import 'app_text_styles.dart';
import 'brand_colors.dart';

abstract final class AppTheme {
  static const radius = 12.0;
  // Height of buttons and inputs
  static const controlHeight = 44.0;
  static const bodyFamily = 'IBMPlexSans';

  static ThemeData light([BrandColors brand = BrandColors.ndzela]) =>
      _build(brand);

  static ThemeData _build(BrandColors brand) {
    final scheme =
        ColorScheme.fromSeed(
          seedColor: brand.primary,
          primary: brand.primary,
          onPrimary: Colors.white,
          surface: Colors.white,
          error: AppColors.danger,
        ).copyWith(
          onSurface: AppColors.ink,
          onSurfaceVariant: AppColors.muted,
          outline: AppColors.line,
          outlineVariant: AppColors.line,
        );
    final base = ThemeData(
      useMaterial3: true,
      colorScheme: scheme,
      extensions: [brand],
      fontFamily: bodyFamily,
    );
    final shape = RoundedRectangleBorder(
      borderRadius: BorderRadius.circular(radius),
    );
    return base.copyWith(
      scaffoldBackgroundColor: AppColors.canvas,
      textTheme: base.textTheme
          .apply(bodyColor: AppColors.ink, displayColor: AppColors.ink)
          .copyWith(
            headlineSmall: AppTextStyles.title,
            titleLarge: AppTextStyles.title,
            titleMedium: AppTextStyles.heading,
            bodyLarge: AppTextStyles.body,
            bodyMedium: AppTextStyles.body,
            bodySmall: AppTextStyles.caption,
            labelLarge: AppTextStyles.action,
          ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        hintStyle: AppTextStyles.body.copyWith(color: AppColors.muted),
        prefixIconColor: AppColors.muted,
        suffixIconColor: AppColors.muted,
        fillColor: Colors.white,
        isDense: true,
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 14,
          vertical: 12,
        ),
        border: _border(AppColors.line),
        enabledBorder: _border(AppColors.line),
        focusedBorder: _border(brand.primary, width: 1.6),
        errorBorder: _border(AppColors.danger),
        focusedErrorBorder: _border(AppColors.danger, width: 1.6),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          minimumSize: const Size.fromHeight(controlHeight),
          shape: shape,
          textStyle: AppTextStyles.action,
        ),
      ),
      navigationBarTheme: NavigationBarThemeData(
        backgroundColor: Colors.white,
        indicatorColor: brand.primarySoft,
        labelTextStyle: WidgetStateProperty.resolveWith(
          (states) => TextStyle(
            fontSize: 11,
            fontWeight: FontWeight.w400,
            color: states.contains(WidgetState.selected)
                ? brand.primaryInk
                : AppColors.muted,
          ),
        ),
        iconTheme: WidgetStateProperty.resolveWith(
          (states) => IconThemeData(
            color: states.contains(WidgetState.selected)
                ? brand.primaryInk
                : AppColors.muted,
          ),
        ),
      ),
      snackBarTheme: SnackBarThemeData(
        behavior: SnackBarBehavior.floating,
        shape: shape,
      ),
    );
  }

  static OutlineInputBorder _border(Color color, {double width = 1}) =>
      OutlineInputBorder(
        borderRadius: BorderRadius.circular(radius),
        borderSide: BorderSide(color: color, width: width),
      );
}
