enum PaymentStatus {
  pending('PENDING'),
  success('SUCCESS'),
  failed('FAILED'),
  expired('EXPIRED');

  const PaymentStatus(this.code);

  final String code;

  bool get isFinal => this != pending;

  static PaymentStatus fromCode(String code) =>
      values.firstWhere((s) => s.code == code, orElse: () => pending);
}
