import 'package:dio/dio.dart';

import '../domain/contract_details.dart';
import '../domain/vehicle_info.dart';
import 'contract_details_dto.dart';
import 'vehicle_dto.dart';

class ContractRepository {
  const ContractRepository(this._dio);

  final Dio _dio;

  Future<VehicleInfo> fetchVehicle() async {
    final response = await _dio.get<Map<String, dynamic>>('/vehicle');
    return VehicleDto.fromJson(response.data!).toDomain();
  }

  Future<ContractDetails> fetchDetails() async {
    final response = await _dio.get<Map<String, dynamic>>('/contract');
    return ContractDetailsDto.fromJson(response.data!).toDomain();
  }
}
