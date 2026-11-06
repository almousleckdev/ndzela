import 'package:dio/dio.dart';

import '../storage/token_storage.dart';

enum RefreshOutcome { refreshed, rejected, unavailable }

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
    if (refresh == null) return RefreshOutcome.rejected;
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
      return _rejectedStatuses.contains(status)
          ? RefreshOutcome.rejected
          : RefreshOutcome.unavailable;
    }
  }
}
