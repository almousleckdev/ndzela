import '../domain/payment_operator.dart';

// The operator's name for a code
String operatorLabel(String code, List<PaymentOperator>? operators) {
  for (final operator in operators ?? const <PaymentOperator>[]) {
    if (operator.code == code) return operator.label;
  }
  return code
      .split('_')
      .map((w) => w.isEmpty ? w : '${w[0]}${w.substring(1).toLowerCase()}')
      .join(' ');
}
