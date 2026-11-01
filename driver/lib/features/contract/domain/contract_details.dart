import '../../../core/money/money.dart';
import 'contract_progress.dart';

class ContractDetails {
  const ContractDetails({
    required this.number,
    required this.startDate,
    required this.endDate,
    required this.durationMonths,
    required this.dailyRent,
    required this.progress,
  });

  final String number;
  final DateTime startDate;
  final DateTime endDate;
  final int durationMonths;
  final Money dailyRent;
  final ContractProgress progress;
}
