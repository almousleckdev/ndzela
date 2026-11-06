import 'package:flutter/material.dart';

import '../../../../app/theme/app_text_styles.dart';
import '../../../../core/widgets/layout/header_band.dart';

// The login band
class LoginHeader extends StatelessWidget {
  const LoginHeader({super.key});

  static const _logoSize = 64.0;

  @override
  Widget build(BuildContext context) {
    return HeaderBand(
      bottomPadding: 22,
      child: SizedBox(
        width: double.infinity,
        child: Column(
          children: [
            Image.asset('assets/images/logo.png', height: _logoSize),
            const SizedBox(height: 12),
            Text('NdZela', style: AppTextStyles.title),
            const SizedBox(height: 4),
            const Text('Votre loyer, simplement.', style: AppTextStyles.label),
          ],
        ),
      ),
    );
  }
}
