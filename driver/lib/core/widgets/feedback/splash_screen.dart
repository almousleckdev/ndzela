import 'package:flutter/material.dart';

import '../adaptive/activity_indicator.dart';

class SplashScreen extends StatelessWidget {
  const SplashScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return const Scaffold(body: Center(child: AppActivityIndicator(size: 32)));
  }
}
