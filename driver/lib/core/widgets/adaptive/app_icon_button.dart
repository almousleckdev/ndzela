import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

import '../../../app/theme/app_colors.dart';
import '../../platform/app_icon.dart';
import '../../platform/platform.dart';
import 'platform_icon.dart';

// A round icon button.
class AppIconButton extends StatelessWidget {
  const AppIconButton({
    super.key,
    required this.icon,
    required this.onPressed,
    required this.tooltip,
    this.outlined = true,
  });

  final AppIcon icon;
  final VoidCallback? onPressed;
  final String tooltip;
  final bool outlined;

  static const _foreground = AppColors.ink;

  static const _border = AppColors.line;

  @override
  Widget build(BuildContext context) {
    if (isCupertino(context)) {
      return Semantics(
        button: true,
        label: tooltip,
        child: CupertinoButton(
          padding: EdgeInsets.zero,
          minimumSize: const Size(44, 44),
          onPressed: onPressed,
          child: Container(
            width: 42,
            height: 42,
            decoration: outlined
                ? BoxDecoration(
                    shape: BoxShape.circle,
                    border: Border.all(color: _border),
                  )
                : null,
            child: PlatformIcon(icon, size: 22, color: _foreground),
          ),
        ),
      );
    }
    final glyph = PlatformIcon(icon);
    return outlined
        ? IconButton.outlined(
            tooltip: tooltip,
            style: IconButton.styleFrom(
              side: BorderSide(color: _border),
              foregroundColor: _foreground,
            ),
            icon: glyph,
            onPressed: onPressed,
          )
        : IconButton(
            tooltip: tooltip,
            style: IconButton.styleFrom(foregroundColor: _foreground),
            icon: glyph,
            onPressed: onPressed,
          );
  }
}
