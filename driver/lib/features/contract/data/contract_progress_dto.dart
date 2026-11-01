import '../../../core/money/money.dart';
import '../domain/contract_progress.dart';

class ContractProgressDto {
  const ContractProgressDto(this.json);

  factory ContractProgressDto.fromJson(Map<String, dynamic> json) =>
      ContractProgressDto(json);

  final Map<String, dynamic> json;

  int _int(String key) => (json[key] as num).toInt();

  ContractProgress toDomain(String currency) => ContractProgress(
    totalDays: _int('totalDays'),
    elapsedDays: _int('elapsedDays'),
    daysLeft: _int('daysLeft'),
    paidDays: _int('paidDays'),
    unpaidDays: _int('unpaidDays'),
    totalPrice: Money(_int('totalPrice'), currency),
    paidAmount: Money(_int('paidAmount'), currency),
    remainingAmount: Money(_int('remainingAmount'), currency),
    deposit: Money(_int('deposit'), currency),
  );
}
