import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

// How long the splash screen stays on every launch, even when the session is ready sooner.
final splashMinimumProvider = Provider<Duration>(
  (_) => const Duration(milliseconds: 1800),
);

// True until the splash has been shown for its minimum time.
final splashHeldProvider = NotifierProvider<SplashHold, bool>(SplashHold.new);

class SplashHold extends Notifier<bool> {
  @override
  bool build() {
    final minimum = ref.read(splashMinimumProvider);
    if (minimum == Duration.zero) return false;
    final timer = Timer(minimum, () => state = false);
    ref.onDispose(timer.cancel);
    return true;
  }
}
