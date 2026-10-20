import '../../../core/money/money.dart';
import '../domain/contract_details.dart';
import 'contract_progress_dto.dart';

class ContractDetailsDto {
  const ContractDetailsDto({
    required this.contractNumber,
    required this.startDate,
    required this.endDate,
    required this.durationMonths,
    required this.dailyRent,
    required this.currency,
    required this.progress,
  });

  factory ContractDetailsDto.fromJson(Map<String, dynamic> json) =>
      ContractDetailsDto(
        contractNumber: json['contractNumber'] as String,
        startDate: json['startDate'] as String,
        endDate: json['endDate'] as String,
        durationMonths: json['durationMonths'] as int,
        dailyRent: (json['dailyRent'] as num).toInt(),
        currency: json['currency'] as String,
        progress: ContractProgressDto.fromJson(
          json['progress'] as Map<String, dynamic>,
        ),
      );

  final String contractNumber;
  final String startDate;
  final String endDate;
  final int durationMonths;
  final int dailyRent;
  final String currency;
  final ContractProgressDto progress;

  ContractDetails toDomain() => ContractDetails(
    number: contractNumber,
    startDate: DateTime.parse(startDate),
    endDate: DateTime.parse(endDate),
    durationMonths: durationMonths,
    dailyRent: Money(dailyRent, currency),
    progress: progress.toDomain(currency),
  );
}
