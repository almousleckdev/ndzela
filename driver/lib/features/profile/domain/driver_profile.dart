class DriverProfile {
  const DriverProfile({
    required this.firstName,
    required this.lastName,
    required this.phone,
    required this.localPhone,
    this.email,
    this.birthDate,
    this.address,
    this.countryCode,
  });

  final String firstName;
  final String lastName;
  final String phone;

  // The number as it is dialled inside the country (074111111), used to prefill the payment form.
  final String localPhone;
  final String? email;
  final DateTime? birthDate;
  final String? address;

  // The driver's country (GA, ML)
  final String? countryCode;

  String get fullName => '$firstName $lastName';

  String get initials => [firstName, lastName]
      .where((n) => n.isNotEmpty)
      .map((n) => String.fromCharCode(n.runes.first))
      .join()
      .toUpperCase();
}
