import 'package:flutter/material.dart';

import '../../../../app/theme/app_colors.dart';
import '../../../../app/theme/brand_colors.dart';
import '../../../../core/widgets/adaptive/app_tappable.dart';
import '../history_filter.dart';

// The choice of which payments to list, as pills the driver can scroll sideways.
class HistoryFilterBar extends StatelessWidget {
  const HistoryFilterBar({
    super.key,
    required this.selected,
    required this.onSelected,
  });

  final HistoryFilter selected;
  final ValueChanged<HistoryFilter> onSelected;

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Row(
        children: [
          for (final filter in HistoryFilter.values) ...[
            Semantics(
              button: true,
              selected: filter == selected,
              label: filter.label,
              child: AppTappable(
                onTap: () => onSelected(filter),
                borderRadius: BorderRadius.circular(22),
                child: Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 16,
                    vertical: 9,
                  ),
                  decoration: BoxDecoration(
                    color: filter == selected
                        ? context.brand.primary
                        : Colors.white,
                    borderRadius: BorderRadius.circular(22),
                    border: Border.all(
                      color: filter == selected
                          ? context.brand.primary
                          : AppColors.line,
                    ),
                  ),
                  child: Text(
                    filter.label,
                    style: TextStyle(
                      fontWeight: FontWeight.w500,
                      color: filter == selected ? Colors.white : AppColors.ink,
                    ),
                  ),
                ),
              ),
            ),
            const SizedBox(width: 8),
          ],
        ],
      ),
    );
  }
}
