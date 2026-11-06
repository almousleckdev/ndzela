import 'package:flutter/material.dart';

import '../../../app/theme/app_text_styles.dart';
import 'screen_skeleton.dart';

// The first screen: the logo and the name settle in, a skeleton of the home screen shimmers under them while the session and the account load.
class SplashScreen extends StatelessWidget {
  const SplashScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(24, 0, 24, 24),
          child: Column(
            children: [
              const Spacer(flex: 3),
              TweenAnimationBuilder<double>(
                tween: Tween(begin: 0, end: 1),
                duration: MediaQuery.disableAnimationsOf(context)
                    ? Duration.zero
                    : const Duration(milliseconds: 700),
                curve: Curves.easeOutCubic,
                builder: (context, t, child) => Opacity(
                  opacity: t,
                  child: Transform.scale(scale: 0.9 + 0.1 * t, child: child),
                ),
                child: Column(
                  children: [
                    Image.asset('assets/images/logo.png', height: 96),
                    const SizedBox(height: 16),
                    Text(
                      'NdZela',
                      style: AppTextStyles.title.copyWith(fontSize: 28),
                    ),
                    const SizedBox(height: 4),
                    const Text(
                      'Votre loyer, simplement.',
                      style: AppTextStyles.label,
                    ),
                  ],
                ),
              ),
              const Spacer(flex: 2),
              const ScreenSkeleton(cards: 2),
              const Spacer(),
            ],
          ),
        ),
      ),
    );
  }
}
