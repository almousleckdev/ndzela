import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

import '../../platform/platform.dart';

class AppActivityIndicator extends StatelessWidget {
  const AppActivityIndicator({super.key, this.color, this.size = 22});

  final Color? color;
  final double size;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: size,
      height: size,
      child: isCupertino(context)
          ? CupertinoActivityIndicator(color: color)
          : CircularProgressIndicator(strokeWidth: 2.5, color: color),
    );
  }
}
