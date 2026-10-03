import '../../../app/theme/brand_colors.dart';
import 'package:flutter/material.dart';

import 'circle_badge.dart';

// A round photo, falling back to the person's initials while it loads, when there is none
class AppAvatar extends StatelessWidget {
  const AppAvatar({
    super.key,
    required this.initials,
    this.photo,
    this.radius = 20,
  });

  final String initials;
  final ImageProvider? photo;
  final double radius;

  @override
  Widget build(BuildContext context) {
    final fallback = Text(
      initials,
      style: TextStyle(
        color: context.brand.primaryInk,
        fontWeight: FontWeight.w700,
        fontSize: radius * 0.8,
      ),
    );
    return CircleBadge(
      color: context.brand.primarySoft,
      size: radius * 2,
      child: photo == null
          ? fallback
          : Image(
              image: photo!,
              width: radius * 2,
              height: radius * 2,
              fit: BoxFit.cover,
              errorBuilder: (_, _, _) => fallback,
            ),
    );
  }
}
