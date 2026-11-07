import 'package:flutter/material.dart';

import 'shimmer.dart';

// One grey card-sized block that shimmers, for a section that loads on its own.
class BlockSkeleton extends StatelessWidget {
  const BlockSkeleton({super.key, this.height = 120});

  final double height;

  @override
  Widget build(BuildContext context) =>
      Shimmer(child: SkeletonBox(height: height, radius: 18));
}
