import 'package:flutter/material.dart';

import '../../../../app/theme/app_colors.dart';
import '../../../../app/theme/app_text_styles.dart';
import '../../domain/rent_day.dart';
import '../../domain/week_days.dart';

// Monday to Saturday of one week, each day paid, unpaid or waiting for its deadline.
class WeekDaysRow extends StatelessWidget {
  const WeekDaysRow({super.key, required this.week});

  final WeekDays week;

  static const _names = ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam'];

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        for (var i = 0; i < WeekDays.rentDaysPerWeek; i++)
          Expanded(
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 2),
              child: _DayCell(
                name: _names[i],
                date: week.start.add(Duration(days: i)),
                rent: week.days[i],
              ),
            ),
          ),
      ],
    );
  }
}

class _Look {
  const _Look(this.label, this.icon, this.color, this.background);

  final String label;
  final IconData icon;
  final Color color;
  final Color background;
}

const _looks = {
  RentDayStatus.paidOnTime: _Look(
    'payé',
    Icons.check_rounded,
    AppColors.success,
    AppColors.successSoft,
  ),
  RentDayStatus.paidLate: _Look(
    'payé en retard',
    Icons.history_rounded,
    AppColors.success,
    AppColors.successSoft,
  ),
  RentDayStatus.unpaid: _Look(
    'impayé',
    Icons.close_rounded,
    AppColors.danger,
    AppColors.dangerSoft,
  ),
  RentDayStatus.awaiting: _Look(
    'à régler',
    Icons.schedule_rounded,
    AppColors.muted,
    AppColors.canvas,
  ),
};

class _DayCell extends StatelessWidget {
  const _DayCell({required this.name, required this.date, required this.rent});

  final String name;
  final DateTime date;
  final RentDay? rent;

  @override
  Widget build(BuildContext context) {
    final look = rent == null ? null : _looks[rent!.status]!;
    return Semantics(
      container: true,
      excludeSemantics: true,
      label: '$name ${date.day} : ${look?.label ?? 'pas de loyer'}',
      child: Column(
        children: [
          Text(name, style: AppTextStyles.caption),
          const SizedBox(height: 4),
          Container(
            height: 46,
            width: double.infinity,
            decoration: BoxDecoration(
              color: look?.background,
              border: look == null ? Border.all(color: AppColors.line) : null,
              borderRadius: BorderRadius.circular(10),
            ),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                if (look != null) Icon(look.icon, size: 16, color: look.color),
                Text(
                  '${date.day}',
                  style: AppTextStyles.caption.copyWith(
                    color: look?.color ?? AppColors.muted,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
