import 'package:flutter/material.dart';

import '../../../app/theme/app_colors.dart';
import 'road_backdrop.dart';

// The light band at the top of every screen; it runs under the status bar and curves at the bottom.
class HeaderBand extends StatelessWidget {
  const HeaderBand({
    super.key,
    required this.child,
    this.bottomPadding = 12,
    this.radius = 20,
  });

  final Widget child;
  final double bottomPadding;
  final double radius;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      clipBehavior: Clip.antiAlias,
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.vertical(bottom: Radius.circular(radius)),
        boxShadow: [
          BoxShadow(
            color: AppColors.ink.withValues(alpha: 0.06),
            blurRadius: 16,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Stack(
        children: [
          const Positioned.fill(child: RoadBackdrop()),
          Padding(
            padding: EdgeInsets.fromLTRB(
              16,
              MediaQuery.paddingOf(context).top + 8,
              16,
              bottomPadding,
            ),
            child: child,
          ),
        ],
      ),
    );
  }
}
