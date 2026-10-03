class PaymentOperator {
  const PaymentOperator({required this.code, required this.label});

  final String code;
  final String label;

  @override
  bool operator ==(Object other) =>
      other is PaymentOperator && other.code == code;

  @override
  int get hashCode => code.hashCode;
}
