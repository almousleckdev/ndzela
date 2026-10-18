import 'dart:typed_data';

import 'package:dio/dio.dart';

import '../domain/arrears_summary.dart';
import '../domain/payment_attempt.dart';
import '../domain/payment_operator.dart';
import '../domain/payment_quote.dart';
import '../domain/payment_request.dart';
import 'arrears_dto.dart';
import 'payment_attempt_dto.dart';
import 'payment_operator_dto.dart';
import 'payment_quote_dto.dart';

class PaymentsRepository {
  const PaymentsRepository(this._dio);

  final Dio _dio;

  Future<ArrearsSummary> fetchArrears() async {
    final response = await _dio.get<Map<String, dynamic>>('/account');
    return ArrearsDto.fromJson(response.data!).toDomain();
  }

  Future<List<PaymentOperator>> fetchOperators() async {
    final response = await _dio.get<List<dynamic>>('/payments/operators');
    return [
      for (final item in response.data!)
        PaymentOperatorDto.fromJson(item as Map<String, dynamic>).toDomain(),
    ];
  }

  Future<PaymentQuote> quote({
    required String operatorCode,
    required String phone,
    int? days,
  }) async {
    final response = await _dio.get<Map<String, dynamic>>(
      '/payments/quote',
      queryParameters: {
        'operator': operatorCode,
        'phone': phone,
        'days': ?days,
      },
    );
    return PaymentQuoteDto.fromJson(response.data!).toDomain();
  }

  // Asks the server to start the payment; the amount is decided there, from the days chosen.
  Future<PaymentAttempt> start(PaymentRequest request) async {
    final response = await _dio.post<Map<String, dynamic>>(
      '/payments',
      data: {
        'operator': request.operator.code,
        'phoneNumber': request.phone,
        'days': ?request.days,
      },
    );
    return PaymentAttemptDto.fromJson(response.data!).toDomain();
  }

  Future<PaymentAttempt> fetchAttempt(String id) async {
    final response = await _dio.get<Map<String, dynamic>>('/payments/$id');
    return PaymentAttemptDto.fromJson(response.data!).toDomain();
  }

  // The receipt of a confirmed payment as a PDF, drawn by the server.
  Future<Uint8List> fetchReceipt(String id) async {
    final response = await _dio.get<List<int>>(
      '/payments/$id/receipt',
      options: Options(responseType: ResponseType.bytes),
    );
    return Uint8List.fromList(response.data!);
  }

  Future<List<PaymentAttempt>> fetchHistory() async {
    final response = await _dio.get<List<dynamic>>('/payments');
    return [
      for (final item in response.data!)
        PaymentAttemptDto.fromJson(item as Map<String, dynamic>).toDomain(),
    ];
  }
}
