import 'package:flutter/material.dart';

class RoadLinePainter extends CustomPainter {
  RoadLinePainter(this.color, this.thickness, this.phase)
    : super(repaint: phase);

  final Color color;
  final double thickness;

  // 0 to 1
  final Animation<double> phase;

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = color
      ..strokeWidth = thickness
      ..strokeCap = StrokeCap.round;
    final dash = thickness * 4;
    final gap = thickness * 3;
    final period = dash + gap;
    for (
      double x = phase.value * period - period;
      x < size.width;
      x += period
    ) {
      canvas.drawLine(
        Offset(x.clamp(0, size.width), size.height / 2),
        Offset((x + dash).clamp(0, size.width), size.height / 2),
        paint,
      );
    }
  }

  @override
  bool shouldRepaint(RoadLinePainter old) =>
      old.color != color || old.thickness != thickness || old.phase != phase;
}
