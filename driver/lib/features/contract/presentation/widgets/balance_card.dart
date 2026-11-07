import 'package:flutter/material.dart';

import '../../../../app/theme/app_text_styles.dart';
import '../../../../app/theme/brand_colors.dart';
import '../../../../core/platform/app_icons.dart';
import '../../../../core/widgets/adaptive/app_button.dart';
import '../../../../core/widgets/display/status_chip.dart';
import '../../../payments/domain/arrears_summary.dart';

// The first thing a driver sees: what is owed, and the way to pay it.
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
    final toPay = arrears.canPay;
    final brand = context.brand;
    final onBrand = Colors.white.withValues(alpha: 0.85);
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: brand.primary,
        borderRadius: BorderRadius.circular(18),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  'Loyer à régler',
                  style: AppTextStyles.label.copyWith(color: onBrand),
                ),
              ),
              StatusChip(
                label: late ? 'En retard' : (toPay ? 'À régler' : 'À jour'),
                color: Colors.white,
                background: Colors.white.withValues(alpha: 0.22),
              ),
            ],
          ),
          const SizedBox(height: 8),
          FittedBox(
            fit: BoxFit.scaleDown,
            alignment: Alignment.centerLeft,
            child: Text.rich(
              TextSpan(
                children: [
                  TextSpan(
                    text: arrears.amountToSettle.digits,
                    style: AppTextStyles.hero.copyWith(
                      color: Colors.white,
                      fontSize: 24,
                    ),
                  ),
                  TextSpan(
                    text: ' ${arrears.amountToSettle.currencyLabel}',
                    style: AppTextStyles.heading.copyWith(color: onBrand),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 2),
          Text(
            late
                ? (arrears.unpaidDays > 1
                      ? '${arrears.unpaidDays} jours de retard'
                      : '1 jour de retard')
                : (toPay
                      ? 'Le loyer du jour est ouvert : payez avant 12:00.'
                      : 'Aucun loyer en retard. Merci !'),
            style: AppTextStyles.label.copyWith(color: onBrand),
          ),
          if (toPay) ...[
            const SizedBox(height: 10),
            AppButton(
              compact: true,
              label: late ? 'Régulariser maintenant' : 'Payer mon loyer',
              icon: AppIcons.pay,
              variant: AppButtonVariant.light,
              onPressed: onRegularise,
            ),
          ],
        ],
      ),
    );
  }
}
