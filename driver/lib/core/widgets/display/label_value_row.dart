import 'package:flutter/material.dart';

import '../../../app/theme/app_text_styles.dart';

class LabelValueRow extends StatelessWidget {
  const LabelValueRow(this.label, this.value, {super.key, this.strong = false});

  final String label;
  final String value;
  final bool strong;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 5),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Flexible(child: Text(label, style: AppTextStyles.label)),
          const SizedBox(width: 12),
          Flexible(
            child: Text(
              value,
              textAlign: TextAlign.end,
              style: strong ? AppTextStyles.figure : AppTextStyles.body,
            ),
          ),
        ],
      ),
    );
  }
}
