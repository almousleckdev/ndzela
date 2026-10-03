import 'package:dio/dio.dart';

import '../storage/token_storage.dart';

// Adds the bearer token and reports a rejected session so the app can sign the driver out.
class AuthInterceptor extends Interceptor {
  AuthInterceptor({required this.tokens, required this.onUnauthorized});

  static const publicPaths = {'/auth/login'};

  final TokenStorage tokens;
  final void Function() onUnauthorized;

  @override
  Future<void> onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    final access = await tokens.readAccess();
    if (access != null) options.headers['Authorization'] = 'Bearer $access';
    handler.next(options);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    if (err.response?.statusCode == 401 &&
        !publicPaths.contains(err.requestOptions.path)) {
      onUnauthorized();
    }
    handler.next(err);
  }
}
