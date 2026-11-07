import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../features/contract/presentation/contract_providers.dart';
import '../../features/payments/presentation/payments_providers.dart';
import '../../features/profile/presentation/profile_providers.dart';

// Loads again what a tab shows each time the driver opens it, so the data is fresh and the shimmer plays.
void refreshTab(WidgetRef ref, int index) {
  switch (index) {
    case 0:
      ref
        ..invalidate(vehicleProvider)
        ..invalidate(contractDetailsProvider)
        ..invalidate(paymentHistoryProvider)
        ..invalidate(arrearsProvider);
    case 1:
      ref.invalidate(arrearsProvider);
    case 2:
      ref.invalidate(paymentHistoryProvider);
    case 3:
      ref
        ..invalidate(driverProfileProvider)
        ..invalidate(documentsProvider)
        ..invalidate(contractDetailsProvider);
  }
}
