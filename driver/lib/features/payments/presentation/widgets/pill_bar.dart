import 'package:flutter/material.dart';

import '../../../../app/theme/app_colors.dart';
import '../../../../app/theme/brand_colors.dart';
import '../../../../core/widgets/adaptive/app_tappable.dart';

// A choice among a few values, as pills the driver can scroll sideways.
class PillBar<T> extends StatelessWidget {
  const PillBar({
    super.key,
    required this.values,
    required this.labelOf,
    required this.selected,
    required this.onSelected,
  });

  final List<T> values;
  final String Function(T value) labelOf;
  final T selected;
  final ValueChanged<T> onSelected;

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Row(
        children: [
          for (final filter in values) ...[
            Semantics(
              button: true,
              selected: filter == selected,
              label: labelOf(filter),
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
                    labelOf(filter),
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
