import 'package:flutter/material.dart';

import '../../../../app/theme/app_colors.dart';
import '../../../../app/theme/app_text_styles.dart';
import '../../domain/payment_operator.dart';
import '../operator_brand.dart';

// The operator found from the number typed, with its logo
class DetectedOperator extends StatelessWidget {
  const DetectedOperator({super.key, required this.operator});

  final PaymentOperator operator;

  static const _logos = {
    'AIRTEL_MONEY': 'assets/icon/airtel.png',
    'MOOV_MONEY': 'assets/icon/moov.png',
  };

  @override
  Widget build(BuildContext context) {
    final logo = _logos[operator.code];
    final color = operatorBrandColor(operator.code, AppColors.line);
    return Semantics(
      label: operator.label,
      child: Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: color, width: 2),
        ),
        child: Row(
          children: [
            if (logo != null)
              ClipRRect(
                borderRadius: BorderRadius.circular(10),
                child: Image.asset(logo, width: 44, height: 44),
              ),
            const SizedBox(width: 14),
            Text(
              operator.label,
              style: AppTextStyles.body.copyWith(fontWeight: FontWeight.w600),
            ),
          ],
        ),
      ),
    );
  }
}
