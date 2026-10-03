import 'package:flutter/material.dart';

import '../../../../app/theme/app_text_styles.dart';
import '../../../../app/theme/brand_colors.dart';
import '../../../../core/platform/app_icons.dart';
import '../../../../core/widgets/adaptive/app_button.dart';
import '../../../../core/widgets/display/road_line.dart';
import '../../../../core/widgets/display/status_chip.dart';
import '../../../payments/domain/arrears_summary.dart';

// The first thing a driver sees
class BalanceCard extends StatelessWidget {
  const BalanceCard({
    super.key,
    required this.arrears,
    required this.onRegularise,
  });

  final ArrearsSummary arrears;
  final VoidCallback onRegularise;

  @override
  Widget build(BuildContext context) {
    final late = arrears.isLate;
    final brand = context.brand;
    final onBrand = Colors.white.withValues(alpha: 0.82);
    return Container(
      padding: const EdgeInsets.all(22),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(26),
        gradient: LinearGradient(
          colors: [brand.primary, brand.primaryInk],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        boxShadow: [
          BoxShadow(
            color: brand.primary.withValues(alpha: 0.28),
            blurRadius: 24,
            offset: const Offset(0, 10),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  'Solde à régulariser',
                  style: TextStyle(
                    color: onBrand,
                    fontWeight: FontWeight.w600,
                    fontSize: 15,
                  ),
                ),
              ),
              StatusChip(
                label: late ? 'En retard' : 'À jour',
                color: Colors.white,
                background: Colors.white.withValues(alpha: 0.22),
              ),
            ],
          ),
          const SizedBox(height: 12),
          FittedBox(
            fit: BoxFit.scaleDown,
            alignment: Alignment.centerLeft,
            child: Text.rich(
              TextSpan(
                children: [
                  TextSpan(
                    text: arrears.total.digits,
                    style: AppTextStyles.display(
                      44,
                      color: Colors.white,
                      height: 1.05,
                    ),
                  ),
                  TextSpan(
                    text: ' ${arrears.total.currencyLabel}',
                    style: AppTextStyles.display(20, color: onBrand),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 6),
          Text(
            late
                ? (arrears.unpaidDays > 1
                      ? '${arrears.unpaidDays} jours de retard'
                      : '1 jour de retard')
                : 'Aucun loyer en retard',
            style: TextStyle(
              color: onBrand,
              fontSize: 15,
              fontWeight: FontWeight.w600,
            ),
          ),
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 18),
            child: RoadLine(color: Colors.white, opacity: 0.55),
          ),
          if (late)
            AppButton(
              label: 'Régulariser maintenant',
              icon: AppIcons.pay,
              variant: AppButtonVariant.light,
              onPressed: onRegularise,
            )
          else
            Text(
              'Merci, votre compte est en règle.',
              style: TextStyle(color: onBrand, fontWeight: FontWeight.w600),
            ),
        ],
      ),
    );
  }
}
