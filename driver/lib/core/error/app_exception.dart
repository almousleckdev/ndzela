import 'package:dio/dio.dart';

// A failure from the API, identified by the server's stable `code`
class AppException implements Exception {
  const AppException({
    required this.code,
    this.status,
    this.message,
    this.requestId,
    this.fieldErrors = const {},
  });

  final String code;
  final int? status;
  final String? message;
  final String? requestId;
  final Map<String, String> fieldErrors;

  bool get isUnauthorized => status == 401;

  factory AppException.from(Object error) {
    if (error is AppException) return error;
    if (error is DioException) {
      final body = error.response?.data;
      if (body is Map<String, dynamic> && body['code'] is String) {
        final errors = body['errors'];
        return AppException(
          code: body['code'] as String,
          status: error.response?.statusCode,
          message: body['message'] as String?,
          requestId: body['requestId'] as String?,
          fieldErrors: errors is List
              ? {
                  for (final e in errors)
                    (e['field'] as String): (e['message'] as String),
                }
              : const {},
        );
      }
      return const AppException(code: 'NETWORK_ERROR');
    }
    return const AppException(code: 'UNEXPECTED_ERROR');
  }

  @override
  String toString() => 'AppException($code, $status)';
}
