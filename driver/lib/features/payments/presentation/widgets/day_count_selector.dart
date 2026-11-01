import 'package:flutter/material.dart';

import '../../domain/payment_option.dart';
import 'day_count_tile.dart';

// How many of the oldest unpaid days to pay
class DayCountSelector extends StatelessWidget {
  const DayCountSelector({
    super.key,
    required this.options,
    required this.selected,
    required this.onSelected,
  });

  final List<PaymentOption> options;
  final int selected;
  final ValueChanged<int> onSelected;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        for (final option in options) ...[
          DayCountTile(
            option: option,
            selected: option.days == selected,
            onTap: () => onSelected(option.days),
          ),
          if (option != options.last) const SizedBox(height: 10),
        ],
      ],
    );
  }
}
