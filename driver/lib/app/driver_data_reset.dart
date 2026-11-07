import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../core/platform/temporary_files.dart';
import '../features/auth/domain/session.dart';
import '../features/auth/presentation/session_controller.dart';
import '../features/contract/presentation/contract_providers.dart';
import '../features/payments/presentation/payment_flow_controller.dart';
import '../features/payments/presentation/payments_providers.dart';
import '../features/profile/presentation/profile_providers.dart';
import '../features/support/presentation/support_providers.dart';

// Forgets everything loaded for the previous driver when a session ends, so the next one never sees it.
final driverDataResetProvider = Provider<void>((ref) {
  ref.listen(sessionControllerProvider, (previous, next) {
    if (next is! SignedOut) return;
    clearTemporaryFiles();
    ref
      ..invalidate(driverProfileProvider)
      ..invalidate(driverPhotoProvider)
      ..invalidate(documentsProvider)
      ..invalidate(vehicleProvider)
      ..invalidate(contractDetailsProvider)
      ..invalidate(supportContactsProvider)
      ..invalidate(arrearsProvider)
      ..invalidate(operatorsProvider)
      ..invalidate(paymentHistoryProvider)
      ..invalidate(paymentFlowControllerProvider);
  });
});
