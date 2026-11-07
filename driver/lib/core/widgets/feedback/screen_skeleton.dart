import 'package:flutter/material.dart';

import 'shimmer.dart';

// The grey shape of a whole screen while it loads: a big figure, two tiles and a list. It fills the space it is given.
class ScreenSkeleton extends StatelessWidget {
  const ScreenSkeleton({super.key});

  static const _rows = 6;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final shape = Shimmer(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const SkeletonBox(height: 150, radius: 18),
              const SizedBox(height: 14),
              const Row(
                children: [
                  Expanded(child: SkeletonBox(height: 72)),
                  SizedBox(width: 12),
                  Expanded(child: SkeletonBox(height: 72)),
                ],
              ),
              const SizedBox(height: 22),
              const SkeletonBox(width: 140, height: 14),
              for (var i = 0; i < _rows; i++) ...[
                const SizedBox(height: 14),
                const _Row(),
              ],
            ],
          ),
        );
        if (!constraints.hasBoundedHeight) return shape;
        return SingleChildScrollView(
          physics: const NeverScrollableScrollPhysics(),
          padding: const EdgeInsets.all(16),
          child: shape,
        );
      },
    );
  }
}

class _Row extends StatelessWidget {
  const _Row();

  @override
  Widget build(BuildContext context) {
    return const Row(
      children: [
        SkeletonBox(width: 44, height: 44, radius: 22),
        SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              SkeletonBox(width: 180, height: 14),
              SizedBox(height: 8),
              SkeletonBox(width: 110, height: 12),
            ],
          ),
        ),
      ],
    );
  }
}
