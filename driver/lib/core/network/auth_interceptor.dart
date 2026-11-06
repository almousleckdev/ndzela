import 'package:dio/dio.dart';

import '../storage/token_storage.dart';
import 'token_refresher.dart';

// Adds the bearer token, renews it when the server says it expired, and reports a session
// the server no longer accepts so the app can sign the driver out.
class AuthInterceptor extends Interceptor {
  AuthInterceptor({
    required this.tokens,
    required this.refresher,
    required this.client,
    required this.onUnauthorized,
  });

  static const publicPaths = {'/auth/login'};
  static const _sentKey = 'sentAccessToken';
  static const _retriedKey = 'retriedAfterRefresh';

  final TokenStorage tokens;
  final TokenRefresher refresher;
  final Dio client;
  final void Function() onUnauthorized;

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

  @override
  Future<void> onError(
    DioException err,
    ErrorInterceptorHandler handler,
  ) async {
    final request = err.requestOptions;
    if (err.response?.statusCode != 401 || publicPaths.contains(request.path)) {
      return handler.next(err);
    }
    if (request.extra[_retriedKey] == true) {
      onUnauthorized();
      return handler.next(err);
    }
    final current = await tokens.readAccess();
    if (current != null && current != request.extra[_sentKey]) {
      return _retry(request, handler, err);
    }
    switch (await refresher.refresh()) {
      case RefreshOutcome.refreshed:
        return _retry(request, handler, err);
      case RefreshOutcome.rejected:
        onUnauthorized();
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
