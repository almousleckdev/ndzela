import 'package:flutter/material.dart';

class RoadBackdropPainter extends CustomPainter {
  RoadBackdropPainter(this.color, this.phase) : super(repaint: phase);

  final Color color;

  // 0 to 1
  final Animation<double> phase;

  @override
  void paint(Canvas canvas, Size size) {
    // Hugs the bottom edge and only climbs at the far right, so it never crosses the header's text.
    final path = Path()
      ..moveTo(-20, size.height - 8)
      ..cubicTo(
        size.width * 0.5,
        size.height - 12,
        size.width * 0.94,
        size.height,
        size.width * 0.97,
        size.height * 0.55,
      )
      ..cubicTo(
        size.width * 0.99,
        size.height * 0.38,
        size.width * 1.03,
        size.height * 0.2,
        size.width * 1.1,
        size.height * 0.12,
      );
    final paint = Paint()
      ..color = color.withValues(alpha: 0.38)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 3
      ..strokeCap = StrokeCap.round;
    for (final metric in path.computeMetrics()) {
      for (double d = phase.value * 20 - 20; d < metric.length; d += 20) {
        final start = d.clamp(0.0, metric.length);
        final end = (d + 11).clamp(0.0, metric.length);
        if (end > start) canvas.drawPath(metric.extractPath(start, end), paint);
      }
    }
    final glow = Paint()
      ..shader =
          RadialGradient(
            colors: [color.withValues(alpha: 0.12), Colors.transparent],
          ).createShader(
            Rect.fromCircle(
              center: Offset(size.width * 0.9, size.height * 0.05),
              radius: size.width * 0.55,
            ),
          );
    canvas.drawCircle(
      Offset(size.width * 0.9, size.height * 0.05),
      size.width * 0.55,
      glow,
    );
  }

  @override
  bool shouldRepaint(RoadBackdropPainter old) =>
      old.color != color || old.phase != phase;
}
