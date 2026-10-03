import 'package:flutter_test/flutter_test.dart';

import 'package:driver/app/theme/app_theme.dart';
import 'package:driver/app/theme/brand_colors.dart';
import 'package:driver/features/profile/data/driver_profile_dto.dart';

void main() {
  test(
    'a Mali driver gets the green brand and a Gabon driver the orange one',
    () {
      expect(BrandColors.forCountry('ML'), BrandColors.angata);
      expect(BrandColors.forCountry('GA'), BrandColors.ndzela);
      expect(BrandColors.forCountry(null), BrandColors.ndzela);
    },
  );

  test('the theme carries the brand colours of the country', () {
    final theme = AppTheme.light(BrandColors.angata);
    expect(theme.colorScheme.primary, BrandColors.angata.primary);
    expect(theme.extension<BrandColors>(), BrandColors.angata);
  });

  test('the profile keeps the country code the server sends', () {
    final profile = DriverProfileDto.fromJson({
      'firstName': 'A',
      'lastName': 'B',
      'phone': '+22370000000',
      'countryCode': 'ML',
    }).toDomain();
    expect(profile.countryCode, 'ML');
  });
}
