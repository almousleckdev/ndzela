import '../config/app_config.dart';

// Photos come back as server paths ("/media/..."); make them loadable.
String? mediaUrl(String? path) {
  if (path == null || path.isEmpty) return null;
  return path.startsWith('http') ? path : AppConfig.apiBaseUrl + path;
}
