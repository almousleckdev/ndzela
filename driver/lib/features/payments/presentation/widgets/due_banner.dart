import 'package:flutter/material.dart';

import '../../../../app/theme/app_colors.dart';
import '../../../../app/theme/brand_colors.dart';
import '../../../../core/formatting/date_format.dart';
import '../../../../core/platform/app_icons.dart';
import '../../../../core/widgets/adaptive/platform_icon.dart';
import '../../domain/arrears_summary.dart';

// Tells the driver by when the rent has to be paid: today's rent is open from midnight and due at noon, a late one is due now.
class DueBanner extends StatelessWidget {
  const DueBanner({super.key, required this.arrears, required this.now});

  final ArrearsSummary arrears;
  final DateTime now;

  @override
  Widget build(BuildContext context) {
    final dueAt = arrears.settleBy!;
    final late = !dueAt.isAfter(now);
    final color = late ? AppColors.danger : context.brand.primaryInk;
    return Container(
      width: double.infinity,
      margin: const EdgeInsets.fromLTRB(20, 16, 20, 0),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: late ? AppColors.dangerSoft : context.brand.primarySoft,
        borderRadius: BorderRadius.circular(14),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          PlatformIcon(
            late ? AppIcons.error : AppIcons.late,
            size: 20,
            color: color,
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              late
                  ? 'Loyer en retard depuis le ${formatDateTime(dueAt)}. Payez maintenant.'
                  : 'Loyer à payer avant le ${formatDateTime(dueAt)}.',
              style: TextStyle(
                color: color,
                fontWeight: FontWeight.w500,
                height: 1.35,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
