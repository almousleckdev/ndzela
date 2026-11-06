import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:driver/core/error/app_exception.dart';
import 'package:driver/core/error/error_messages.dart';
import 'package:flutter_test/flutter_test.dart';

DioException _failure(int status, Object? data) {
  final request = RequestOptions(path: '/documents/contract');
  return DioException(
    requestOptions: request,
    response: Response(requestOptions: request, statusCode: status, data: data),
  );
}

void main() {
  test('a server failure is not reported as a lost connection', () {
    final exception = AppException.from(
      _failure(500, utf8.encode('<html>oops</html>')),
    );

    expect(exception.code, 'SERVER_ERROR');
    expect(messageFor(exception), isNot(contains('Connexion impossible')));
  });

  test('the error of a file download is read out of its bytes', () {
    final body = utf8.encode(
      jsonEncode({'code': 'NO_CONTRACT', 'message': 'none'}),
    );

    expect(AppException.from(_failure(422, body)).code, 'NO_CONTRACT');
  });

  test('no answer at all is a lost connection', () {
    final request = RequestOptions(path: '/x');

    expect(
      AppException.from(
        DioException.connectionError(
          requestOptions: request,
          reason: 'offline',
        ),
      ).code,
      'NETWORK_ERROR',
    );
  });
}
