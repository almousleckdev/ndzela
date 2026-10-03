import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../features/auth/domain/session.dart';
import '../../features/auth/presentation/session_controller.dart';
import '../../features/profile/presentation/profile_providers.dart';
import 'brand_colors.dart';

// The brand of the signed-in driver's country (orange for Gabon, green for Mali); Ndzela until it is known.
final brandProvider = Provider<BrandColors>((ref) {
  if (ref.watch(sessionControllerProvider) is! SignedIn) {
    return BrandColors.ndzela;
  }
  final profile = ref.watch(driverProfileProvider);
  return BrandColors.forCountry(profile.asData?.value.countryCode);
});
