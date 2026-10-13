import 'package:driver/core/config/app_config.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  String resolve(
    String configured, {
    bool release = false,
    bool android = false,
  }) => AppConfig.resolveBaseUrl(
    configured: configured,
    release: release,
    android: android,
  );

  test('a debug build falls back to the local server of its platform', () {
    expect(resolve(''), 'http://localhost:8080');
    expect(resolve('', android: true), 'http://10.0.2.2:8080');
    expect(resolve('http://192.168.1.5:8080'), 'http://192.168.1.5:8080');
  });

  test('a release build refuses a missing or non-HTTPS server', () {
    expect(() => resolve('', release: true), throwsStateError);
    expect(
      () => resolve('http://api.ndzela.com', release: true),
      throwsStateError,
    );
    expect(
      resolve('https://api.ndzela.com', release: true),
      'https://api.ndzela.com',
    );
  });
}
