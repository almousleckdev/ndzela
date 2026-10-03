import '../domain/driver_profile.dart';

class DriverProfileDto {
  const DriverProfileDto({
    required this.firstName,
    required this.lastName,
    required this.phone,
    required this.localPhone,
    this.email,
    this.birthDate,
    this.address,
    this.countryCode,
  });

  factory DriverProfileDto.fromJson(Map<String, dynamic> json) =>
      DriverProfileDto(
        firstName: json['firstName'] as String,
        lastName: json['lastName'] as String,
        phone: json['phone'] as String,
        localPhone: (json['localPhone'] ?? json['phone']) as String,
        email: json['email'] as String?,
        birthDate: json['birthDate'] as String?,
        address: json['address'] as String?,
        countryCode: json['countryCode'] as String?,
      );

  final String firstName;
  final String lastName;
  final String phone;
  final String localPhone;
  final String? email;
  final String? birthDate;
  final String? address;
  final String? countryCode;

  DriverProfile toDomain() => DriverProfile(
    firstName: firstName,
    lastName: lastName,
    phone: phone,
    localPhone: localPhone,
    email: email,
    birthDate: birthDate == null ? null : DateTime.parse(birthDate!),
    address: (address == null || address!.isEmpty) ? null : address,
    countryCode: countryCode,
  );
}
