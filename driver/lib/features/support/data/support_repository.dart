import 'package:dio/dio.dart';

import '../domain/support_contacts.dart';
import 'support_contacts_dto.dart';

class SupportRepository {
  const SupportRepository(this._dio);

  final Dio _dio;

  Future<SupportContacts> fetchContacts() async {
    final response = await _dio.get<Map<String, dynamic>>('/support');
    return SupportContactsDto.fromJson(response.data!).toDomain();
  }

  Future<void> requestAccountDeletion() =>
      _dio.post<void>('/account/deletion-request');

  Future<void> sendMessage(String message) =>
      _dio.post<void>('/support/messages', data: {'message': message});
}
