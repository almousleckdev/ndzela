import 'dart:math';

import 'package:dio/dio.dart';

class RequestIdInterceptor extends Interceptor {
  final _random = Random.secure();

  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) {
    final bytes = List<int>.generate(16, (_) => _random.nextInt(256));
    options.headers['X-Request-ID'] = bytes
        .map((b) => b.toRadixString(16).padLeft(2, '0'))
        .join();
    handler.next(options);
  }
}
