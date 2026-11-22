import 'package:dio/dio.dart';

import '../../features/auth/domain/session.dart';
import '../storage/token_storage.dart';
import 'token_refresher.dart';

// Adds the bearer token, renews it when the server says it expired, and reports a session
// the server ended so the app can sign the driver out.
class AuthInterceptor extends Interceptor {
  AuthInterceptor({
    required this.tokens,
    required this.refresher,
    required this.client,
    required this.onSessionEnded,
  });

  static const publicPaths = {'/auth/login'};
  static const _sentKey = 'sentAccessToken';
  static const _retriedKey = 'retriedAfterRefresh';

  final TokenStorage tokens;
  final TokenRefresher refresher;
  final Dio client;
  final void Function(SessionEndReason reason) onSessionEnded;

  @override
  Future<void> onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    final access = await tokens.readAccess();
    if (access != null) {
      options.headers['Authorization'] = 'Bearer $access';
      options.extra[_sentKey] = access;
    }
    handler.next(options);
  }

  // The refresh endpoint alone decides whether the session is over: a 401 on any other request
  // (a stale token, a race with another request) never signs the driver out by itself.
  @override
  Future<void> onError(
    DioException err,
    ErrorInterceptorHandler handler,
  ) async {
    final request = err.requestOptions;
    if (err.response?.statusCode != 401 || publicPaths.contains(request.path)) {
      return handler.next(err);
    }
    final retried = request.extra[_retriedKey] == true;
    if (!retried) {
      final current = await tokens.readAccess();
      if (current != null && current != request.extra[_sentKey]) {
        return _retry(request, handler, err);
      }
    }
    switch (await refresher.refresh()) {
      case RefreshOutcome.refreshed:
        return retried ? handler.next(err) : _retry(request, handler, err);
      case RefreshOutcome.replaced:
        onSessionEnded(SessionEndReason.replaced);
        return handler.next(err);
      case RefreshOutcome.expired:
        onSessionEnded(SessionEndReason.expired);
        return handler.next(err);
      case RefreshOutcome.unavailable:
        return handler.next(
          DioException(
            requestOptions: request,
            type: DioExceptionType.connectionError,
            error: err,
          ),
        );
    }
  }

  Future<void> _retry(
    RequestOptions request,
    ErrorInterceptorHandler handler,
    DioException original,
  ) async {
    request.extra[_retriedKey] = true;
    try {
      handler.resolve(await client.fetch<dynamic>(request));
    } on DioException catch (error) {
      handler.next(error);
    }
  }
}
