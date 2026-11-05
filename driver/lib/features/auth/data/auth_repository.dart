import 'package:dio/dio.dart';

import '../../../core/storage/token_storage.dart';
import 'login_response_dto.dart';

class AuthRepository {
  const AuthRepository(this._dio, this._tokens);

  final Dio _dio;
  final TokenStorage _tokens;

  Future<void> signIn({
    required String identifier,
    required String password,
  }) async {
    final response = await _dio.post<Map<String, dynamic>>(
      '/auth/login',
      data: {'identifier': identifier, 'password': password},
    );
    await _store(response.data!);
  }

  // Asks for a 6-digit code by e-mail; the server answers the same whether or not the account exists.
  Future<void> requestResetCode(String identifier) => _dio.post<void>(
    '/auth/forgot-password',
    data: {'identifier': identifier},
  );

  // Checks the code without using it, so the next screen can ask for the new password.
  Future<void> verifyResetCode({
    required String identifier,
    required String code,
  }) => _dio.post<void>(
    '/auth/verify-reset-code',
    data: {'identifier': identifier, 'code': code},
  );

  Future<void> resetPassword({
    required String identifier,
    required String code,
    required String newPassword,
  }) => _dio.post<void>(
    '/auth/reset-password',
    data: {'identifier': identifier, 'code': code, 'newPassword': newPassword},
  );

  Future<void> changePassword({
    required String currentPassword,
    required String newPassword,
  }) async {
    final response = await _dio.put<Map<String, dynamic>>(
      '/me/password',
      data: {'currentPassword': currentPassword, 'newPassword': newPassword},
    );
    await _store(response.data!);
  }

  // Revokes the session on the server when reachable; the local tokens are always cleared by the caller.
  Future<void> signOut() async {
    final refresh = await _tokens.readRefresh();
    try {
      await _dio.post<void>('/auth/logout', data: {'refreshToken': refresh});
    } on DioException {
      return;
    }
  }

  Future<void> _store(Map<String, dynamic> json) {
    final login = LoginResponseDto.fromJson(json);
    return _tokens.save(
      access: login.accessToken,
      refresh: login.refreshToken,
      mustChangePassword: login.mustChangePassword,
    );
  }
}
