import 'dart:typed_data';

import 'package:dio/dio.dart';

import '../domain/driver_document.dart';
import '../domain/driver_profile.dart';
import 'driver_document_dto.dart';
import 'driver_profile_dto.dart';

class ProfileRepository {
  const ProfileRepository(this._dio);

  final Dio _dio;

  Future<DriverProfile> fetchMe() async {
    final response = await _dio.get<Map<String, dynamic>>('/me');
    return DriverProfileDto.fromJson(response.data!).toDomain();
  }

  Future<List<DriverDocument>> fetchDocuments() async {
    final response = await _dio.get<List<dynamic>>('/documents');
    return [
      for (final item in response.data!)
        ?DriverDocumentDto.fromJson(item as Map<String, dynamic>).toDomain(),
    ];
  }

  // The driver's profile picture, or null when none was uploaded.
  Future<Uint8List?> fetchPhoto() async {
    try {
      final response = await _dio.get<List<int>>(
        '/me/photo',
        options: Options(responseType: ResponseType.bytes),
      );
      return Uint8List.fromList(response.data!);
    } on DioException catch (error) {
      if (error.response?.statusCode == 404) return null;
      rethrow;
    }
  }
}
