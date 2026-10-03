import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

import 'platform.dart';

// One concept, two native glyphs
class AppIcon {
  const AppIcon(this.material, this.cupertino);

  final IconData material;
  final IconData cupertino;

  IconData of(BuildContext context) =>
      isCupertino(context) ? cupertino : material;
}
