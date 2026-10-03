import 'package:flutter/material.dart';

import '../../../app/theme/app_colors.dart';

class LabelValueRow extends StatelessWidget {
  const LabelValueRow(this.label, this.value, {super.key, this.strong = false});

  final String label;
  final String value;
  final bool strong;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            label,
            style: TextStyle(
              color: AppColors.muted,
              fontWeight: strong ? FontWeight.w700 : FontWeight.w500,
            ),
          ),
          const SizedBox(width: 12),
          Flexible(
            child: Text(
              value,
              textAlign: TextAlign.end,
              style: TextStyle(
                fontWeight: FontWeight.w800,
                fontSize: strong ? 18 : 15,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
