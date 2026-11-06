// The mobile money operator a local number belongs to: 07 is Airtel, 06 is Moov (the leading 0 is optional).
String? detectOperatorCode(String digits) {
  final significant = digits.startsWith('0') ? digits.substring(1) : digits;
  if (significant.isEmpty) return null;
  return switch (significant[0]) {
    '7' => 'AIRTEL_MONEY',
    '6' => 'MOOV_MONEY',
    _ => null,
  };
}
