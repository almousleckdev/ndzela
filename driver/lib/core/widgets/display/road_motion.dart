import 'package:flutter/material.dart';

// Drives the dashes of the road decorations
class RoadMotion extends StatefulWidget {
  const RoadMotion({super.key, required this.builder});

  final Widget Function(BuildContext context, Animation<double> phase) builder;

  @override
  State<RoadMotion> createState() => _RoadMotionState();
}

class _RoadMotionState extends State<RoadMotion>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 1100),
  );

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (MediaQuery.disableAnimationsOf(context)) {
      _controller.stop();
    } else if (!_controller.isAnimating) {
      _controller.repeat();
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => widget.builder(context, _controller);
}
