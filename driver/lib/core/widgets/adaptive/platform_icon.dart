import 'package:flutter/widgets.dart';

import '../../platform/app_icon.dart';

class PlatformIcon extends StatelessWidget {
  const PlatformIcon(this.icon, {super.key, this.size, this.color});

  final AppIcon icon;
  final double? size;
  final Color? color;

  @override
  Widget build(BuildContext context) =>
      Icon(icon.of(context), size: size, color: color);
}
