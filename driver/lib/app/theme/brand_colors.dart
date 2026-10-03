import 'package:flutter/material.dart';

// The colours of the company brand
@immutable
class BrandColors extends ThemeExtension<BrandColors> {
  const BrandColors({
    required this.primary,
    required this.primaryInk,
    required this.primarySoft,
  });

  static const ndzela = BrandColors(
    primary: Color(0xFFF97316),
    primaryInk: Color(0xFFC2410C),
    primarySoft: Color(0xFFFFEDD5),
  );

  static const angata = BrandColors(
    primary: Color(0xFF16A34A),
    primaryInk: Color(0xFF166534),
    primarySoft: Color(0xFFDCFCE7),
  );

  // The brand of a country code (ML is Angata); Ndzela for any other or none.
  static BrandColors forCountry(String? code) => code == 'ML' ? angata : ndzela;

  final Color primary;
  final Color primaryInk;
  final Color primarySoft;

  @override
  BrandColors copyWith({
    Color? primary,
    Color? primaryInk,
    Color? primarySoft,
  }) => BrandColors(
    primary: primary ?? this.primary,
    primaryInk: primaryInk ?? this.primaryInk,
    primarySoft: primarySoft ?? this.primarySoft,
  );

  @override
  BrandColors lerp(BrandColors? other, double t) => other == null
      ? this
      : BrandColors(
          primary: Color.lerp(primary, other.primary, t)!,
          primaryInk: Color.lerp(primaryInk, other.primaryInk, t)!,
          primarySoft: Color.lerp(primarySoft, other.primarySoft, t)!,
        );
}

extension BrandContext on BuildContext {
  BrandColors get brand =>
      Theme.of(this).extension<BrandColors>() ?? BrandColors.ndzela;
}
