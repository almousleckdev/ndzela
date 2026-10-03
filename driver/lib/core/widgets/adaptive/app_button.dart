import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

import '../../../app/theme/app_colors.dart';
import '../../../app/theme/app_theme.dart';
import '../../../app/theme/brand_colors.dart';
import '../../platform/app_icon.dart';
import '../../platform/platform.dart';
import 'activity_indicator.dart';
import 'platform_icon.dart';

// `light` is a white button for a branded surface.
enum AppButtonVariant { primary, secondary, destructive, light }

// The app's one action button
class AppButton extends StatelessWidget {
  const AppButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.loading = false,
    this.icon,
    this.variant = AppButtonVariant.primary,
  });

  final String label;
  final VoidCallback? onPressed;
  final bool loading;
  final AppIcon? icon;
  final AppButtonVariant variant;

  bool get _outlined => variant == AppButtonVariant.secondary;

  Color _fill(BuildContext context) => switch (variant) {
    AppButtonVariant.destructive => AppColors.danger,
    AppButtonVariant.light => Colors.white,
    _ => context.brand.primary,
  };

  Color _foreground(BuildContext context) => switch (variant) {
    AppButtonVariant.secondary => AppColors.ink,
    AppButtonVariant.light => context.brand.primaryInk,
    _ => Colors.white,
  };

  @override
  Widget build(BuildContext context) {
    final action = loading ? null : onPressed;
    final disabled = action == null && !loading;
    final content = loading
        ? AppActivityIndicator(color: _foreground(context))
        : Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              if (icon != null) ...[
                PlatformIcon(icon!, size: 20),
                const SizedBox(width: 8),
              ],
              Flexible(child: Text(label, overflow: TextOverflow.ellipsis)),
            ],
          );

    if (isCupertino(context)) {
      final textColor = disabled ? AppColors.muted : _foreground(context);
      final button = CupertinoButton(
        color: _outlined ? null : _fill(context),
        disabledColor: _outlined
            ? Colors.transparent
            : (loading ? _fill(context) : AppColors.line),
        borderRadius: BorderRadius.circular(AppTheme.radius),
        minimumSize: const Size.fromHeight(54),
        padding: const EdgeInsets.symmetric(horizontal: 20),
        onPressed: action,
        child: DefaultTextStyle.merge(
          style: TextStyle(
            fontFamily: AppTheme.bodyFamily,
            fontSize: 16,
            fontWeight: FontWeight.w600,
            color: textColor,
          ),
          child: IconTheme.merge(
            data: IconThemeData(color: textColor),
            child: content,
          ),
        ),
      );
      return _outlined
          ? DecoratedBox(
              decoration: BoxDecoration(
                border: Border.all(color: AppColors.line, width: 1.5),
                borderRadius: BorderRadius.circular(AppTheme.radius),
              ),
              child: button,
            )
          : button;
    }
    if (_outlined) {
      return OutlinedButton(
        onPressed: action,
        style: OutlinedButton.styleFrom(
          minimumSize: const Size.fromHeight(54),
          foregroundColor: AppColors.ink,
          side: const BorderSide(color: AppColors.line, width: 1.5),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(AppTheme.radius),
          ),
          textStyle: const TextStyle(
            fontFamily: AppTheme.bodyFamily,
            fontSize: 16,
            fontWeight: FontWeight.w600,
          ),
        ),
        child: content,
      );
    }
    return FilledButton(
      onPressed: action,
      style: variant == AppButtonVariant.primary
          ? null
          : FilledButton.styleFrom(
              backgroundColor: _fill(context),
              foregroundColor: _foreground(context),
            ),
      child: content,
    );
  }
}
