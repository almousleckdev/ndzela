import 'package:flutter/material.dart';

import '../../../app/theme/brand_colors.dart';
import '../display/road_motion.dart';
import 'road_backdrop_painter.dart';

// A faint winding dashed road behind a header, echoing the logo.
class RoadBackdrop extends StatelessWidget {
  const RoadBackdrop({super.key});

  @override
  Widget build(BuildContext context) => IgnorePointer(
    child: RoadMotion(
      builder: (context, phase) => CustomPaint(
        painter: RoadBackdropPainter(context.brand.primary, phase),
        size: Size.infinite,
      ),
    ),
  );
}
