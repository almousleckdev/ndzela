import 'package:flutter/material.dart';

import '../../../../app/theme/app_colors.dart';
import '../../../../core/platform/app_icons.dart';
import '../../../../core/widgets/adaptive/platform_icon.dart';
import '../../domain/arrears_summary.dart';

// Tells a restricted driver why only the payment screen is open and what to do.
class AccessRestrictionBanner extends StatelessWidget {
  const AccessRestrictionBanner({super.key, required this.arrears});

  final ArrearsSummary arrears;

  @override
  Widget build(BuildContext context) {
    final message = arrears.locked
        ? 'Vous avez ${arrears.unpaidDays} jours de loyer impayés. Réglez la totalité pour retrouver l\'accès à l\'application.'
        : 'Votre compte est suspendu. Contactez Ndzela pour le réactiver.';
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
              message,
              style: const TextStyle(
                color: AppColors.danger,
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
