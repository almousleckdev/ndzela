import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../data/contract_repository.dart';
import '../domain/contract_details.dart';
import '../domain/vehicle_info.dart';

final contractRepositoryProvider = Provider<ContractRepository>(
  (ref) => ContractRepository(ref.watch(dioProvider)),
);

final vehicleProvider = FutureProvider<VehicleInfo>(
  (ref) => ref.watch(contractRepositoryProvider).fetchVehicle(),
);

final contractDetailsProvider = FutureProvider<ContractDetails>(
  (ref) => ref.watch(contractRepositoryProvider).fetchDetails(),
);
