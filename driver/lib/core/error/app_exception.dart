import 'dart:convert';

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
      final body = _jsonBody(error.response?.data);
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
      final status = error.response?.statusCode;
      return status != null && status >= 500
          ? AppException(code: 'SERVER_ERROR', status: status)
          : const AppException(code: 'NETWORK_ERROR');
    }
    return const AppException(code: 'UNEXPECTED_ERROR');
  }

  // An error answer to a file download arrives as bytes: read the server's JSON out of them.
  static Object? _jsonBody(Object? data) {
    if (data is! List<int>) return data;
    try {
      return jsonDecode(utf8.decode(data));
    } on FormatException {
      return null;
    }
  }

  @override
  String toString() => 'AppException($code, $status)';
}
