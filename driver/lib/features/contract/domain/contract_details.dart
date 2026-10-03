import '../../../core/money/money.dart';

class ContractDetails {
  const ContractDetails({
    required this.number,
    required this.startDate,
    required this.endDate,
    required this.durationMonths,
    required this.dailyRent,
  });

  final String number;
  final DateTime startDate;
  final DateTime endDate;
  final int durationMonths;
  final Money dailyRent;
}
