import 'package:flutter/material.dart';

import 'road_line_painter.dart';
import 'road_motion.dart';

import '../../../app/theme/brand_colors.dart';

// The brand's dashed road marking (from the logo), used as a divider and as decoration.
class RoadLine extends StatelessWidget {
  const RoadLine({super.key, this.color, this.thickness = 2, this.opacity = 1});

  // The brand colour when none is given.
  final Color? color;
  final double thickness;
  final double opacity;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: thickness,
      width: double.infinity,
      child: RoadMotion(
        builder: (context, phase) => CustomPaint(
          painter: RoadLinePainter(
            (color ?? context.brand.primary).withValues(alpha: opacity),
            thickness,
            phase,
          ),
        ),
      ),
    );
  }
}
