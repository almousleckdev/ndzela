import 'package:flutter/painting.dart';

import '../../../app/theme/app_colors.dart';

// The operator's brand colour for its tile; the list of operators itself comes from the server.
Color operatorBrandColor(String code, Color fallback) => switch (code) {
  'AIRTEL_MONEY' => AppColors.airtel,
  'MOOV_MONEY' => AppColors.moov,
  'ORANGE_MONEY' => AppColors.orange,
  _ => fallback,
};
