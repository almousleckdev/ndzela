import '../../../../app/theme/brand_colors.dart';
import 'package:flutter/material.dart';

import '../../../../app/theme/app_colors.dart';
import '../../../../app/theme/app_text_styles.dart';
import '../../../../core/widgets/layout/header_band.dart';

// The login band
class LoginHeader extends StatelessWidget {
  const LoginHeader({super.key});

  static const _logoSize = 84.0;

  @override
  Widget build(BuildContext context) {
    return HeaderBand(
      radius: 36,
      bottomPadding: 28,
      child: SizedBox(
        width: double.infinity,
        child: Column(
          children: [
            Container(
              width: _logoSize,
              height: _logoSize,
              padding: const EdgeInsets.all(5),
              decoration: BoxDecoration(
                color: Colors.white,
                shape: BoxShape.circle,
                boxShadow: [
                  BoxShadow(
                    color: context.brand.primary.withValues(alpha: 0.45),
                    blurRadius: 36,
                    spreadRadius: 2,
                  ),
                ],
              ),
              child: ClipOval(
                child: Image.asset('assets/images/logo.png', fit: BoxFit.cover),
              ),
            ),
            const SizedBox(height: 16),
            Text('NdZela', style: AppTextStyles.display(30)),
            const SizedBox(height: 6),
            const Text(
              'Votre loyer, simplement.',
              style: TextStyle(color: AppColors.muted, fontSize: 16),
            ),
          ],
        ),
      ),
    );
  }
}
