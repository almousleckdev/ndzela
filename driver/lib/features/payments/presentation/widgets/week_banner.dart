import 'package:flutter/material.dart';

import '../../../../app/theme/app_colors.dart';
import '../../../../core/formatting/date_format.dart';
import '../../../../core/platform/app_icons.dart';
import '../../../../core/widgets/adaptive/platform_icon.dart';
import '../../domain/arrears_summary.dart';

// Reminds the driver, from Friday, that the week's rents are due by Sunday noon.
class WeekBanner extends StatelessWidget {
  const WeekBanner({super.key, required this.arrears});

  final ArrearsSummary arrears;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      margin: const EdgeInsets.fromLTRB(20, 16, 20, 0),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppColors.dangerSoft,
        borderRadius: BorderRadius.circular(14),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const PlatformIcon(AppIcons.error, size: 20, color: AppColors.danger),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              'Semaine non réglée : payez avant le ${formatDateTime(arrears.weekClosesAt!)}.',
              style: const TextStyle(
                color: AppColors.danger,
                fontWeight: FontWeight.w600,
                height: 1.35,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
