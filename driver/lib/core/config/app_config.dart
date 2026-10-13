import 'package:flutter/foundation.dart';

abstract final class AppConfig {
  static const apiPrefix = '/api/v1/driver';

  // Override with --dart-define=API_BASE_URL=... (a physical phone needs the computer's network address).
  static const _configuredBaseUrl = String.fromEnvironment('API_BASE_URL');

  // The public pages of the website that the store listings and the app point to.
  static const _siteUrl = String.fromEnvironment(
    'SITE_URL',
    defaultValue: 'https://ndzela.com',
  );
  static Uri get privacyUri => Uri.parse('$_siteUrl/confidentialite');
  static Uri get termsUri => Uri.parse('$_siteUrl/conditions');

  // The Android emulator reaches the computer at 10.0.2.2; the iOS simulator at localhost.
  // A release build never guesses: it needs API_BASE_URL, over HTTPS.
  static String get apiBaseUrl => resolveBaseUrl(
    configured: _configuredBaseUrl,
    release: kReleaseMode,
    android: defaultTargetPlatform == TargetPlatform.android,
  );

  @visibleForTesting
  static String resolveBaseUrl({
    required String configured,
    required bool release,
    required bool android,
  }) {
    if (release && !configured.startsWith('https://')) {
      throw StateError(
        'A release build needs --dart-define=API_BASE_URL=https://...',
      );
    }
    if (configured.isNotEmpty) return configured;
    return android ? 'http://10.0.2.2:8080' : 'http://localhost:8080';
  }
}
