import '../domain/support_contacts.dart';

class SupportContactsDto {
  const SupportContactsDto({this.phone, this.whatsapp, this.email});

  factory SupportContactsDto.fromJson(Map<String, dynamic> json) =>
      SupportContactsDto(
        phone: json['phone'] as String?,
        whatsapp: json['whatsapp'] as String?,
        email: json['email'] as String?,
      );

  final String? phone;
  final String? whatsapp;
  final String? email;

  SupportContacts toDomain() =>
      SupportContacts(phone: phone, whatsapp: whatsapp, email: email);
}
