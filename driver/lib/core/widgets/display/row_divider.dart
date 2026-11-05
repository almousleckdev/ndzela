import 'package:flutter/material.dart';

import '../../../app/theme/app_colors.dart';

// The hairline between rows of a card, indented past the icon.
class RowDivider extends StatelessWidget {
  const RowDivider({super.key});

  @override
  Widget build(BuildContext context) => const Padding(
    padding: EdgeInsets.only(left: 44),
    child: SizedBox(
      height: 1,
      width: double.infinity,
      child: ColoredBox(color: AppColors.line),
    ),
  );
}
