import 'package:flutter/material.dart';

import '../../platform/platform.dart';

// Makes a custom surface tappable
class AppTappable extends StatelessWidget {
  const AppTappable({
    super.key,
    required this.onTap,
    required this.borderRadius,
    required this.child,
  });

  final VoidCallback onTap;
  final BorderRadius borderRadius;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    if (isCupertino(context)) {
      return GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTap: onTap,
        child: child,
      );
    }
    return InkWell(onTap: onTap, borderRadius: borderRadius, child: child);
  }
}
