import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

import '../../../app/theme/app_colors.dart';
import '../../../app/theme/app_text_styles.dart';
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
    this.compact = false,
  });

  final String label;
  final VoidCallback? onPressed;
  final bool loading;
  final AppIcon? icon;
  final AppButtonVariant variant;

  // A shorter button for use inside a card
  final bool compact;

  double get _height => compact ? 38 : AppTheme.controlHeight;

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
                PlatformIcon(icon!, size: 18),
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
        minimumSize: Size.fromHeight(_height),
        padding: const EdgeInsets.symmetric(horizontal: 20),
        onPressed: action,
        child: DefaultTextStyle.merge(
          style: AppTextStyles.action.copyWith(
            fontFamily: AppTheme.bodyFamily,
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
                border: Border.all(color: AppColors.line),
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
          minimumSize: Size.fromHeight(_height),
          foregroundColor: AppColors.ink,
          side: const BorderSide(color: AppColors.line),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(AppTheme.radius),
          ),
          textStyle: AppTextStyles.action,
        ),
        child: content,
      );
    }
    return FilledButton(
      onPressed: action,
      style: variant == AppButtonVariant.primary && !compact
          ? null
          : FilledButton.styleFrom(
              backgroundColor: variant == AppButtonVariant.primary
                  ? null
                  : _fill(context),
              foregroundColor: variant == AppButtonVariant.primary
                  ? null
                  : _foreground(context),
              minimumSize: Size.fromHeight(_height),
            ),
      child: content,
    );
  }
}
