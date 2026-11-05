import '../../../app/theme/brand_colors.dart';
import 'package:flutter/material.dart';

import '../../../app/theme/app_text_styles.dart';
import 'app_card.dart';

// A titled card; the orange tick before the title is the brand's road marking.
class SectionCard extends StatelessWidget {
  const SectionCard({super.key, required this.title, required this.child});

  final String title;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(4, 0, 4, 8),
          child: Row(
            children: [
              Container(
                width: 4,
                height: 14,
                decoration: BoxDecoration(
                  color: context.brand.primary,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
              const SizedBox(width: 8),
              Text(title, style: AppTextStyles.heading),
            ],
          ),
        ),
        AppCard(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 2),
          child: child,
        ),
      ],
    );
  }
}
