import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../auth/domain/session.dart';
import '../../auth/presentation/session_controller.dart';
import '../domain/driver_access.dart';
import 'payments_providers.dart';

// Derived from the server's answer about the driver's arrears
final driverAccessProvider = Provider<DriverAccess>((ref) {
  if (ref.watch(sessionControllerProvider) is! SignedIn) {
    return DriverAccess.open;
  }
  final arrears = ref.watch(arrearsProvider);
  if (arrears.hasValue) {
    return arrears.requireValue.restricted
        ? DriverAccess.restricted
        : DriverAccess.open;
  }
  return arrears.isLoading ? DriverAccess.loading : DriverAccess.open;
});
