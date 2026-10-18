import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../features/auth/presentation/session_controller.dart';
import '../config/app_config.dart';
import '../storage/token_storage.dart';
import 'auth_interceptor.dart';
import 'request_id_interceptor.dart';

final dioProvider = Provider<Dio>((ref) {
  final dio = Dio(
    BaseOptions(
      baseUrl: AppConfig.apiBaseUrl + AppConfig.apiPrefix,
      connectTimeout: const Duration(seconds: 10),
      receiveTimeout: const Duration(seconds: 20),
      headers: {'Accept': 'application/json'},
    ),
  );
  dio.interceptors.addAll([
    RequestIdInterceptor(),
    AuthInterceptor(
      tokens: ref.watch(tokenStorageProvider),
      onUnauthorized: () =>
          ref.read(sessionControllerProvider.notifier).signOut(forced: true),
    ),
  ]);
  return dio;
});
