import 'package:flutter/material.dart';

import '../display/app_card.dart';
import 'shimmer.dart';

// The shape of a screen while it loads: a balance card, then two list cards.
class ScreenSkeleton extends StatelessWidget {
  const ScreenSkeleton({super.key, this.cards = 3});

  final int cards;

  @override
  Widget build(BuildContext context) {
    return Shimmer(
      child: Column(
        children: [
          for (var i = 0; i < cards; i++) ...[
            if (i > 0) const SizedBox(height: 12),
            const AppCard(
              padding: EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  SkeletonBox(width: 120, height: 14),
                  SizedBox(height: 12),
                  SkeletonBox(height: 28),
                  SizedBox(height: 10),
                  SkeletonBox(width: 200, height: 12),
                ],
              ),
            ),
          ],
        ],
      ),
    );
  }
}
