import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class TokenStorage {
  const TokenStorage(this._storage);

  static const _accessKey = 'access_token';
  static const _refreshKey = 'refresh_token';
  static const _mustChangeKey = 'must_change_password';

  final FlutterSecureStorage _storage;

  Future<String?> readAccess() => _storage.read(key: _accessKey);
  Future<String?> readRefresh() => _storage.read(key: _refreshKey);
  Future<bool> readMustChangePassword() async =>
      await _storage.read(key: _mustChangeKey) == 'true';

  Future<void> save({
    required String access,
    required String refresh,
    required bool mustChangePassword,
  }) async {
    await _storage.write(key: _accessKey, value: access);
    await _storage.write(key: _refreshKey, value: refresh);
    await _storage.write(key: _mustChangeKey, value: '$mustChangePassword');
  }

  Future<void> clear() async {
    await _storage.delete(key: _accessKey);
    await _storage.delete(key: _refreshKey);
    await _storage.delete(key: _mustChangeKey);
  }
}

// Tokens stay on this phone: never in a backup, never moved to another device.
const secureStorage = FlutterSecureStorage(
  iOptions: IOSOptions(
    accessibility: KeychainAccessibility.first_unlock_this_device,
  ),
);

final tokenStorageProvider = Provider<TokenStorage>(
  (ref) => const TokenStorage(secureStorage),
);
