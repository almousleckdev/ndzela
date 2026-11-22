import 'package:dio/dio.dart';

import '../storage/token_storage.dart';

// refreshed: new tokens saved. replaced: the driver signed in on another phone. expired: the server no
// longer knows the refresh token. unavailable: the server could not be asked (the session is kept).
enum RefreshOutcome { refreshed, replaced, expired, unavailable }

// Exchanges the stored refresh token for a new pair; callers that ask together share one exchange,
// because a refresh token can be used only once.
class TokenRefresher {
  TokenRefresher({required this.tokens, required this.client});

  static const _rejectedStatuses = {400, 401, 403};

  final TokenStorage tokens;

  // A client with no interceptors, so a refused refresh can never trigger another one
  final Dio client;

  Future<RefreshOutcome>? _exchange;

  Future<RefreshOutcome> refresh() {
    final running = _exchange ??= _run().whenComplete(() => _exchange = null);
    return running;
  }

  Future<RefreshOutcome> _run() async {
    final refresh = await tokens.readRefresh();
    if (refresh == null) return RefreshOutcome.expired;
    try {
      final response = await client.post<Map<String, dynamic>>(
        '/auth/refresh',
        data: {'refreshToken': refresh},
      );
      final json = response.data!;
      await tokens.save(
        access: json['accessToken'] as String,
        refresh: json['refreshToken'] as String,
        mustChangePassword: json['mustChangePassword'] as bool? ?? false,
      );
      return RefreshOutcome.refreshed;
    } on DioException catch (error) {
      final status = error.response?.statusCode;
      if (!_rejectedStatuses.contains(status)) {
        return RefreshOutcome.unavailable;
      }
      final body = error.response?.data;
      final code = body is Map<String, dynamic> ? body['code'] : null;
      return code == 'SESSION_REPLACED'
          ? RefreshOutcome.replaced
          : RefreshOutcome.expired;
    }
  }
}
