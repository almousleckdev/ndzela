import '../../../core/money/money.dart';
import '../domain/contract_details.dart';

class ContractDetailsDto {
  const ContractDetailsDto({
    required this.contractNumber,
    required this.startDate,
    required this.endDate,
    required this.durationMonths,
    required this.dailyRent,
    required this.currency,
  });

  factory ContractDetailsDto.fromJson(Map<String, dynamic> json) =>
      ContractDetailsDto(
        contractNumber: json['contractNumber'] as String,
        startDate: json['startDate'] as String,
        endDate: json['endDate'] as String,
        durationMonths: json['durationMonths'] as int,
        dailyRent: (json['dailyRent'] as num).toInt(),
        currency: json['currency'] as String,
      );

  final String contractNumber;
  final String startDate;
  final String endDate;
  final int durationMonths;
  final int dailyRent;
  final String currency;

  ContractDetails toDomain() => ContractDetails(
    number: contractNumber,
    startDate: DateTime.parse(startDate),
    endDate: DateTime.parse(endDate),
    durationMonths: durationMonths,
    dailyRent: Money(dailyRent, currency),
  );
}
