import '../../../core/money/money.dart';

// Where the contract stands since day one: rent days (Monday to Saturday) and money against the total price.
class ContractProgress {
  const ContractProgress({
    required this.totalDays,
    required this.elapsedDays,
    required this.daysLeft,
    required this.paidDays,
    required this.unpaidDays,
    required this.totalPrice,
    required this.paidAmount,
    required this.remainingAmount,
    required this.deposit,
  });

  final int totalDays;
  final int elapsedDays;
  final int daysLeft;
  final int paidDays;
  final int unpaidDays;
  final Money totalPrice;
  final Money paidAmount;
  final Money remainingAmount;
  final Money deposit;

  // From 0 to 1, for the progress bar.
  double get elapsedShare =>
      totalDays == 0 ? 0 : (elapsedDays / totalDays).clamp(0, 1).toDouble();
}
