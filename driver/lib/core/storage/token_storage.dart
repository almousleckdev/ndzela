import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class TokenStorage {
  const TokenStorage(this._storage);

  static const _accessKey = 'access_token';
  static const _refreshKey = 'refresh_token';
  static const _mustChangeKey = 'must_change_password';
  static const _rememberKey = 'remember_me';
  static const _identifierKey = 'saved_identifier';

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

  // "Se souvenir de moi": a remembered driver stays signed in until they sign out or sign in on another
  // phone. Absent means remembered, so a session from an older version is never dropped.
  Future<bool> readRemember() async =>
      await _storage.read(key: _rememberKey) != 'false';
  Future<String?> readSavedIdentifier() => _storage.read(key: _identifierKey);

  // Only the e-mail or phone is kept for the next sign-in, never the password.
  Future<void> saveRemember({
    required bool remember,
    required String identifier,
  }) async {
    await _storage.write(key: _rememberKey, value: '$remember');
    if (remember) {
      await _storage.write(key: _identifierKey, value: identifier);
    } else {
      await _storage.delete(key: _identifierKey);
    }
  }

  // The session only; the remember-me choice and the saved identifier survive a sign-out.
  Future<void> clear() async {
    await _storage.delete(key: _accessKey);
    await _storage.delete(key: _refreshKey);
    await _storage.delete(key: _mustChangeKey);
  }

  Future<void> clearAll() async {
    await clear();
    await _storage.delete(key: _rememberKey);
    await _storage.delete(key: _identifierKey);
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
