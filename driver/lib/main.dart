import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/date_symbol_data_local.dart';

import 'app/app.dart';
import 'core/config/app_config.dart';
import 'core/platform/temporary_files.dart';
import 'core/storage/first_run_guard.dart';
import 'core/storage/token_storage.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  AppConfig
      .apiBaseUrl; // a misconfigured release build stops here, not at the first request
  await initializeDateFormatting('fr');
  await const FirstRunGuard(TokenStorage(secureStorage)).run();
  await clearTemporaryFiles();
  runApp(const ProviderScope(child: NdzelaApp()));
}
