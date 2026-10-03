import 'package:intl/intl.dart';

// An exact amount in the currency's smallest unit (XAF and XOF have none), never a double.
class Money {
  const Money(this.amount, this.currency);

  final int amount;
  final String currency;

  String get currencyLabel =>
      const {'XAF': 'FCFA', 'XOF': 'FCFA'}[currency] ?? currency;

  // Digits only, grouped the French way
  String get digits => NumberFormat.decimalPattern(
    'fr',
  ).format(amount).replaceAll(RegExp(r'[  ]'), ' ');

  @override
  String toString() => '$digits $currencyLabel';

  Money operator *(int times) => Money(amount * times, currency);

  @override
  bool operator ==(Object other) =>
      other is Money && other.amount == amount && other.currency == currency;

  @override
  int get hashCode => Object.hash(amount, currency);
}
