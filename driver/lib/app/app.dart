import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'driver_data_reset.dart';
import 'router.dart';
import 'theme/app_theme.dart';
import 'theme/brand_provider.dart';

class NdzelaApp extends ConsumerWidget {
  const NdzelaApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    ref.watch(driverDataResetProvider);
    return MaterialApp.router(
      title: 'Ndzela',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light(ref.watch(brandProvider)),
      routerConfig: ref.watch(routerProvider),
      locale: const Locale('fr'),
      supportedLocales: const [Locale('fr')],
      localizationsDelegates: GlobalMaterialLocalizations.delegates,
    );
  }
}
