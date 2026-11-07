import 'package:flutter/material.dart';

import '../../../app/theme/app_colors.dart';
import '../../../app/theme/app_text_styles.dart';

// The first screen: the logo and the name settle in on white while the session loads.
class SplashScreen extends StatelessWidget {
  const SplashScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final still = MediaQuery.disableAnimationsOf(context);
    return Scaffold(
      backgroundColor: AppColors.canvas,
      body: Center(
        child: TweenAnimationBuilder<double>(
          tween: Tween(begin: 0, end: 1),
          duration: still ? Duration.zero : const Duration(milliseconds: 800),
          curve: Curves.easeOutCubic,
          builder: (context, t, child) => Opacity(
            opacity: t,
            child: Transform.translate(
              offset: Offset(0, 12 * (1 - t)),
              child: child,
            ),
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Image.asset('assets/images/logo.png', height: 112),
              const SizedBox(height: 20),
              Text('NdZela', style: AppTextStyles.title.copyWith(fontSize: 30)),
              const SizedBox(height: 6),
              const Text(
                'Votre loyer, simplement.',
                style: AppTextStyles.label,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
