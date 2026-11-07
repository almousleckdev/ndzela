import 'package:url_launcher/url_launcher.dart';

// Opens a phone call, a chat, a mail composer or a web page outside the app. A web page falls back to the in-app browser when no other app takes it; false when nothing can handle it.
Future<bool> openExternal(Uri uri) async {
  final modes = uri.scheme == 'https' || uri.scheme == 'http'
      ? const [LaunchMode.externalApplication, LaunchMode.inAppBrowserView]
      : const [LaunchMode.externalApplication];
  for (final mode in modes) {
    try {
      if (await launchUrl(uri, mode: mode)) return true;
    } on Exception {
      continue;
    }
  }
  return false;
}

Uri phoneUri(String number) => Uri(scheme: 'tel', path: number);

Uri whatsappUri(String number) =>
    Uri.https('wa.me', '/${number.replaceAll(RegExp(r'\D'), '')}');

Uri emailUri(String address) => Uri(scheme: 'mailto', path: address);
